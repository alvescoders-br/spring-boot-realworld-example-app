#!/usr/bin/env python3
"""Validate the local Docker Compose operations stack.

The script intentionally uses only the Python standard library. It starts the
Compose stack, waits for the public `/tags` endpoint, checks the Spring Boot
startup log, validates app metrics, Mimir ingestion and Tempo trace ingestion,
tears the stack down, and verifies that no Compose containers are left running.
"""
from __future__ import annotations

import argparse
import http.client
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


DEFAULT_APP_BASE_URL = "http://localhost:8080"
DEFAULT_READY_URL = "http://localhost:8080/tags"
DEFAULT_METRICS_URL = "http://localhost:8080/actuator/prometheus"
DEFAULT_ENDPOINTS = ("/tags", "/actuator/health", "/actuator/info")
DEFAULT_MIMIR_READY_URL = "http://localhost:9909/ready"
DEFAULT_MIMIR_QUERY_URL = "http://localhost:9909/prometheus/api/v1/query"
DEFAULT_TEMPO_READY_URL = "http://localhost:33200/ready"
DEFAULT_TEMPO_METRICS_URL = "http://localhost:33200/metrics"
TEMPO_TRACE_BATCHES_RECEIVED_METRIC = "tempo_distributor_traces_per_batch_count"
STARTUP_LOG_MARKER = "RealWorldApplication startup complete"
SHUTDOWN_LOG_MARKER = "RealWorldApplication shutdown complete"


def run_command(command: list[str], *, cwd: Path, timeout_seconds: int) -> subprocess.CompletedProcess[str]:
    print(f"$ {' '.join(command)}")
    return subprocess.run(
        command,
        cwd=cwd,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout_seconds,
        check=False,
    )


def require_success(result: subprocess.CompletedProcess[str], action: str) -> None:
    if result.returncode == 0:
        return

    output = result.stdout.strip()
    raise RuntimeError(f"{action} failed with exit code {result.returncode}\n{output}")


def wait_for_http(url: str, timeout_seconds: int) -> None:
    deadline = time.monotonic() + timeout_seconds
    last_error = "not attempted"

    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=5) as response:
                if 200 <= response.status < 500:
                    print(f"readiness OK: {url} -> HTTP {response.status}")
                    return
        except (urllib.error.URLError, TimeoutError, http.client.HTTPException, OSError) as exc:
            last_error = str(exc)
        time.sleep(2)

    raise TimeoutError(f"Timed out waiting for {url}; last error: {last_error}")


def compose_command(compose_file: Path) -> list[str]:
    return ["docker", "compose", "-f", str(compose_file)]


def collect_logs(command_prefix: list[str], cwd: Path, timeout_seconds: int) -> str:
    result = run_command(
        [*command_prefix, "logs", "--no-color", "app"],
        cwd=cwd,
        timeout_seconds=timeout_seconds,
    )
    require_success(result, "Collect app logs")
    return result.stdout


def validate_startup_log(logs: str) -> None:
    if STARTUP_LOG_MARKER not in logs:
        raise RuntimeError(f"Startup log marker not found: {STARTUP_LOG_MARKER!r}")
    print(f"startup log OK: found {STARTUP_LOG_MARKER!r}")


def validate_shutdown_log(logs: str) -> None:
    if SHUTDOWN_LOG_MARKER not in logs:
        raise RuntimeError(f"Shutdown log marker not found: {SHUTDOWN_LOG_MARKER!r}")
    print(f"shutdown log OK: found {SHUTDOWN_LOG_MARKER!r}")


def stop_app(command_prefix: list[str], cwd: Path, timeout_seconds: int) -> None:
    result = run_command(
        [*command_prefix, "stop", "app"],
        cwd=cwd,
        timeout_seconds=timeout_seconds,
    )
    require_success(result, "Stop app service")


def validate_shutdown(command_prefix: list[str], cwd: Path, timeout_seconds: int) -> None:
    result = run_command(
        [*command_prefix, "ps", "-q"],
        cwd=cwd,
        timeout_seconds=timeout_seconds,
    )
    require_success(result, "Check Compose container state after shutdown")
    remaining_containers = [line for line in result.stdout.splitlines() if line.strip()]
    if remaining_containers:
        raise RuntimeError(
            "Compose shutdown validation failed; containers still running: "
            + ", ".join(remaining_containers)
        )
    print("shutdown OK: no Compose containers left running")


def read_url(url: str, timeout_seconds: int = 10) -> str:
    with urllib.request.urlopen(url, timeout=timeout_seconds) as response:
        return response.read().decode("utf-8", errors="replace")


def endpoint_url(base_url: str, endpoint: str) -> str:
    if endpoint.startswith(("http://", "https://")):
        return endpoint
    return f"{base_url.rstrip('/')}/{endpoint.lstrip('/')}"


def endpoint_counter(metrics_text: str, endpoint_uri: str) -> float:
    total = 0.0
    for line in metrics_text.splitlines():
        if line.startswith("#") or "http_server_requests_seconds_count" not in line:
            continue
        if f'uri="{endpoint_uri}"' not in line:
            continue
        try:
            total += float(line.rsplit(" ", 1)[1])
        except (IndexError, ValueError):
            continue
    return total


def prometheus_metric_total(metrics_text: str, metric_name: str) -> float:
    total = 0.0
    for line in metrics_text.splitlines():
        if line.startswith("#"):
            continue
        if not (line.startswith(f"{metric_name} ") or line.startswith(f"{metric_name}{{")):
            continue
        try:
            total += float(line.rsplit(" ", 1)[1])
        except (IndexError, ValueError):
            continue
    return total


def promql_label_value(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def mimir_endpoint_counter(query_url: str, endpoint_uri: str) -> float:
    query = f'sum(http_server_requests_seconds_count{{uri="{promql_label_value(endpoint_uri)}"}})'
    url = f"{query_url}?{urllib.parse.urlencode({'query': query})}"
    payload = json.loads(read_url(url))
    if payload.get("status") != "success":
        raise RuntimeError(f"Mimir query failed: {payload}")

    total = 0.0
    for result in payload.get("data", {}).get("result", []):
        try:
            total += float(result["value"][1])
        except (KeyError, IndexError, TypeError, ValueError):
            continue
    return total


def validate_endpoint_counter(
    *,
    app_base_url: str,
    metrics_url: str,
    mimir_query_url: str,
    endpoint_uri: str,
    timeout_seconds: int,
) -> None:
    before_app = endpoint_counter(read_url(metrics_url), endpoint_uri)
    before_mimir = mimir_endpoint_counter(mimir_query_url, endpoint_uri)
    read_url(endpoint_url(app_base_url, endpoint_uri))

    deadline = time.monotonic() + timeout_seconds
    app_seen = False
    mimir_seen = False
    last_error = "not attempted"

    while time.monotonic() < deadline:
        try:
            after_app = endpoint_counter(read_url(metrics_url), endpoint_uri)
            after_mimir = mimir_endpoint_counter(mimir_query_url, endpoint_uri)
            app_seen = app_seen or after_app > before_app
            mimir_seen = mimir_seen or after_mimir > before_mimir
            if app_seen and mimir_seen:
                print(
                    "endpoint counter OK: "
                    f"{endpoint_uri} app {before_app:g}->{after_app:g}; "
                    f"mimir {before_mimir:g}->{after_mimir:g}"
                )
                return
        except (urllib.error.URLError, TimeoutError, http.client.HTTPException, OSError, RuntimeError) as exc:
            last_error = str(exc)
        time.sleep(2)

    raise RuntimeError(
        f"Endpoint counter did not increment in app and Mimir for {endpoint_uri!r}; "
        f"app_seen={app_seen}, mimir_seen={mimir_seen}, last_error={last_error}"
    )


def validate_endpoint_counters(
    *,
    app_base_url: str,
    metrics_url: str,
    mimir_query_url: str,
    endpoints: list[str],
    timeout_seconds: int,
) -> None:
    for endpoint in endpoints:
        validate_endpoint_counter(
            app_base_url=app_base_url,
            metrics_url=metrics_url,
            mimir_query_url=mimir_query_url,
            endpoint_uri=endpoint,
            timeout_seconds=timeout_seconds,
        )


def tempo_trace_batches_received(metrics_url: str) -> float:
    return prometheus_metric_total(read_url(metrics_url), TEMPO_TRACE_BATCHES_RECEIVED_METRIC)


def validate_tempo_trace_batches(metrics_url: str, before: float, timeout_seconds: int) -> None:
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        after = tempo_trace_batches_received(metrics_url)
        if after > before:
            print(
                "tempo traces OK: "
                f"{TEMPO_TRACE_BATCHES_RECEIVED_METRIC} incremented from {before:g} to {after:g}"
            )
            return
        time.sleep(2)
    raise RuntimeError(f"Tempo trace batch counter did not increment from {before:g}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--compose-file",
        default="docker-compose.yml",
        help="Compose file path relative to the repository root.",
    )
    parser.add_argument(
        "--ready-url",
        default=DEFAULT_READY_URL,
        help="Public URL used for application readiness validation.",
    )
    parser.add_argument(
        "--app-base-url",
        default=DEFAULT_APP_BASE_URL,
        help="Base URL used to call endpoint paths during counter validation.",
    )
    parser.add_argument(
        "--metrics-url",
        default=DEFAULT_METRICS_URL,
        help="Prometheus metrics URL used to validate endpoint counters.",
    )
    parser.add_argument(
        "--metrics-uri",
        default=None,
        help="Legacy alias for validating a single endpoint uri label.",
    )
    parser.add_argument(
        "--endpoint",
        action="append",
        dest="endpoints",
        help="Endpoint uri label to validate; may be passed multiple times.",
    )
    parser.add_argument(
        "--mimir-ready-url",
        default=DEFAULT_MIMIR_READY_URL,
        help="Mimir readiness URL.",
    )
    parser.add_argument(
        "--mimir-query-url",
        default=DEFAULT_MIMIR_QUERY_URL,
        help="Mimir Prometheus-compatible instant query URL.",
    )
    parser.add_argument(
        "--tempo-ready-url",
        default=DEFAULT_TEMPO_READY_URL,
        help="Tempo readiness URL.",
    )
    parser.add_argument(
        "--tempo-metrics-url",
        default=DEFAULT_TEMPO_METRICS_URL,
        help="Tempo Prometheus metrics URL used to validate trace ingestion.",
    )
    parser.add_argument(
        "--ingestion-timeout-seconds",
        type=int,
        default=90,
        help="Maximum time to wait for Mimir and Tempo ingestion after calls.",
    )
    parser.add_argument(
        "--startup-timeout-seconds",
        type=int,
        default=180,
        help="Maximum time to wait for the application readiness endpoint.",
    )
    parser.add_argument(
        "--command-timeout-seconds",
        type=int,
        default=600,
        help="Maximum time for Docker Compose commands.",
    )
    parser.add_argument(
        "--no-build",
        action="store_true",
        help="Run Compose without rebuilding the app image.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = Path(__file__).resolve().parents[1]
    compose_file = repo_root / args.compose_file
    if not compose_file.is_file():
        print(f"Compose file not found: {compose_file}", file=sys.stderr)
        return 2

    command_prefix = compose_command(compose_file)
    up_command = [*command_prefix, "up", "-d"]
    if not args.no_build:
        up_command.append("--build")

    endpoints = args.endpoints or ([args.metrics_uri] if args.metrics_uri else list(DEFAULT_ENDPOINTS))

    env_note = os.environ.get("REALWORLD_JWT_SECRET")
    if env_note:
        print("REALWORLD_JWT_SECRET is set in the caller environment; Compose service uses its own local value.")

    try:
        result = run_command(up_command, cwd=repo_root, timeout_seconds=args.command_timeout_seconds)
        require_success(result, "Start Compose stack")

        wait_for_http(args.ready_url, args.startup_timeout_seconds)
        wait_for_http(args.mimir_ready_url, args.startup_timeout_seconds)
        wait_for_http(args.tempo_ready_url, args.startup_timeout_seconds)
        logs = collect_logs(command_prefix, repo_root, args.command_timeout_seconds)
        validate_startup_log(logs)
        trace_batches_before = tempo_trace_batches_received(args.tempo_metrics_url)
        validate_endpoint_counters(
            app_base_url=args.app_base_url,
            metrics_url=args.metrics_url,
            mimir_query_url=args.mimir_query_url,
            endpoints=endpoints,
            timeout_seconds=args.ingestion_timeout_seconds,
        )
        validate_tempo_trace_batches(args.tempo_metrics_url, trace_batches_before, args.ingestion_timeout_seconds)
        stop_app(command_prefix, repo_root, args.command_timeout_seconds)
        logs = collect_logs(command_prefix, repo_root, args.command_timeout_seconds)
        validate_shutdown_log(logs)
    except Exception as exc:
        print(f"validation failed: {exc}", file=sys.stderr)
        return 1
    finally:
        down_result = run_command(
            [*command_prefix, "down"],
            cwd=repo_root,
            timeout_seconds=args.command_timeout_seconds,
        )
        if down_result.returncode != 0:
            print(down_result.stdout, file=sys.stderr)
            print("warning: docker compose down failed", file=sys.stderr)

    try:
        validate_shutdown(command_prefix, repo_root, args.command_timeout_seconds)
    except Exception as exc:
        print(f"shutdown validation failed: {exc}", file=sys.stderr)
        return 1

    print("operations validation PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
