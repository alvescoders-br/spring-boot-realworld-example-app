# Next Agent Briefing

State: IMPLEMENTING

## Resume order

1. Read `AGENTS.md`.
2. If using Claude Code, read `CLAUDE.md`.
3. Read `harness/docs/harness/START_HERE.md`.
4. Read `docs/GUIA-DE-REFATORACAO.md` — **the project source of truth** (precedence over generic best practices; §0).
5. Read `harness/docs/harness/source-map.md` and `harness/docs/harness/phase-state.md`.
6. Read the current phase file `harness/docs/harness/phases/phase-2.md`.
7. Read the active plan files under `harness/docs/plans/active/phase-2-safety-net/`.
8. Resume from GitHub issue `#1`; unblock via decision issue `#3` before changing the wrapper.

## Current situation

Phase 2 — Safety Net is active. **Slice 1 (`springdoc-openapi`) is DONE** (Turno 14).
**Slice 2 (`contract-tests`) is EVALUATING** — Generator completed at Turno 16; awaits evaluator sign-off.

Tracking issue: `#1` — `feat: planejar safety net de contrato da Phase 2`.
Slice 2 issue: `#4` — `feat: contract-tests — REST @WebMvcTest estendidos + testes DGS GraphQL`.
Resolved decision issue: `#2` — JDK 17 exists at `C:\JAVA\jdk17.0.19_10`.
Resolved decision issue: `#3` — wrapper reverted to `gradle-7.4-bin.zip` (guide §1 baseline). Commit `fa60c47`.
Retroactive issue: `#5` — Phase 1 Fundação bootstrap (commit `0b441cf`). Closed 2026-06-12.
Retroactive issue: `#6` — Phase 2 Slice 1 springdoc (commits `4cc546a`+`980059d`). Closed 2026-06-12.

Turno 14 result (Slice 1 evaluator sign-off):
- 21 suites total: **69 tests, 0 failures, 0 errors, 0 skipped**.
- `evaluation-report.md` state = `DONE`.

Turno 15 result (Slice 2 Planner):
- `spec-2.md` created at `harness/docs/plans/active/phase-2-safety-net/spec-2.md`.
- Issue `#4` created.
- Pre-Slice Checklist (9 items) completed; Model Profile proposed.

Turno 16 result (Slice 2 Generator):
- 5 DGS test suites created; 6 test methods covering tags, articles, profile, me, createUser, login.
- `application-test.properties` updated: `spring.datasource.hikari.maximum-pool-size=1` for in-memory SQLite.
- `./gradlew test`: BUILD SUCCESSFUL — **75 tests, 0 failures** (26 suites). Commit `f9fdf74`.
- AC-1 gap: `graphql-dgs-spring-boot-starter-test:4.9.21` absent from Maven Central; `DgsQueryExecutor` served by main starter. Evaluator must rule on AC-1.
- AC-2 PASS (6 ops), AC-3 PASS, AC-4 PASS, AC-5 PASS.

## Active artifacts

- `harness/docs/plans/active/phase-2-safety-net/task-card.md`
- `harness/docs/plans/active/phase-2-safety-net/spec.md` (Slice 1 — reference)
- `harness/docs/plans/active/phase-2-safety-net/spec-2.md` (Slice 2 — **active**)
- `harness/docs/plans/active/phase-2-safety-net/progress.md`
- `harness/docs/plans/active/phase-2-safety-net/evaluation-report.md`

## Binding constraints (from the guide)

- **Architecture & contract are immutable** without permission (§0.2, §3): DDD layers `api/core/application/infrastructure`, CQRS, REST+GraphQL coexistence, RealWorld API contract, base `http://localhost:8080` **without** `/api` prefix.
- **Permission protocol §14**: STOP and open/use `needs-decision` issue for SQLite Hibernate dialect vs. DB swap, CQRS read-model strategy in JPA, Joda→`java.time` vs `AttributeConverter`, dependency bumps for SB4/Gradle9, reading-time cache mechanism, `record` conversions that change the contract, and anything not listed in §4/§5.
- **Safety net first** (§11, §15): freeze REST+GraphQL contract with tests **before** any 4.x migration.
- **Issue before code** (§6): every commit references `#<id>` — use `#4` for Slice 2 commits; Conventional Commits (§7).
- **JDK 17 + Gradle 7.4**: executar `./gradlew test` com `JAVA_HOME=C:\JAVA\jdk17.0.19_10`.

## Next action — Evaluator (Slice 2 sign-off)

1. Ler `spec-2.md` (ACs) e `progress.md` (Turno 16).
2. Verificar XMLs em `build/test-results/test/TEST-io.spring.graphql.*.xml` — confirmar 0 falhas.
3. Avaliar AC-1 gap: `graphql-dgs-spring-boot-starter-test:4.9.21` inexistente; `DgsQueryExecutor` disponível via starter principal. Decidir PASS ou PARTIAL.
4. Emitir sign-off em `evaluation-report.md` (adicionar seção Slice 2).
5. Se DONE: atualizar `phase-state.md`, `task-card.md` (Slice 2 → DONE) e `progress.md`.
6. Cross-family: google/gemini-3.5-flash recomendado; se indisponível, registrar divergência.

## Do not

- Do not change architecture, the RealWorld contract, or the GraphQL schema without §14 permission.
- Do not start a 4.x migration before the Phase 2 safety net is green on REST **and** GraphQL.
- Do not store real secrets, credentials, or sensitive payloads.
