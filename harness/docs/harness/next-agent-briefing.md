# Next Agent Briefing

State: DONE (Phase 2) — Aguardando Planner Phase 3

## Resume order

1. Ler `AGENTS.md`.
2. Se usar Claude Code, ler `CLAUDE.md`.
3. Ler `harness/docs/harness/START_HERE.md`.
4. Ler `docs/GUIA-DE-REFATORACAO.md` — **the project source of truth** (precedence over generic best practices; §0).
5. Ler `harness/docs/harness/source-map.md` e `harness/docs/harness/phase-state.md`.
6. Ler o roadmap §15.3 do guia para identificar o escopo da Phase 3.
7. Executar Pre-Slice Checklist completo (9 itens) antes de qualquer código.
8. Abrir issue de tracking GitHub (template `feature_request.md`) antes de iniciar.

## Current situation

**Phase 2 — Safety Net: COMPLETAMENTE DONE** (Turno 21, evaluator sign-off 2026-06-12).

Todos os 3 slices concluídos com evidência XML real (JDK 17 + Gradle 7.4 + SB 2.6.3):

| Slice | Status | Evidência |
|-------|--------|-----------|
| 1 — springdoc-openapi | DONE (Turno 14) | 69/69 testes; `/v3/api-docs` expõe contrato REST |
| 2 — contract-tests | DONE (Turno 18) | 75/75 testes; 6 operações GraphQL DGS congeladas |
| 3 — integration-tests | DONE (Turno 21) | 76/76 testes; 9 endpoints REST encadeados (flow completo) |

**Issues de housekeeping pendentes (usuário deve fechar manualmente):**
- `#4` — Slice 2 (OPEN desde Turno 19; classifier bloqueou auto-close).
- `#7` — Slice 3 (DONE desde Turno 21).
- `#1` — tracking Phase 2 (Phase 2 DONE).

## Arquivos relevantes (Phase 2 — referência)

- `harness/docs/plans/active/phase-2-safety-net/evaluation-report.md` — sign-offs Slices 1, 2 e 3
- `harness/docs/plans/active/phase-2-safety-net/progress.md` — histórico completo (Turnos 1–21)
- `harness/docs/plans/active/phase-2-safety-net/task-card.md` — status DONE
- `src/test/java/io/spring/api/OpenApiContractTest.java` — Slice 1
- `src/test/java/io/spring/graphql/` — Slice 2 (5 suites DGS)
- `src/test/java/io/spring/api/RealworldFlowIntegrationTest.java` — Slice 3

## Resolved issues (Phase 2)

- `#2` — JDK 17 localizado em `C:\JAVA\jdk17.0.19_10`.
- `#3` — wrapper revertido para `gradle-7.4-bin.zip`. Commit `fa60c47`.
- `#5` — Phase 1 Fundação bootstrap (commit `0b441cf`). Closed 2026-06-12.
- `#6` — Phase 2 Slice 1 springdoc (commits `4cc546a`+`980059d`). Closed 2026-06-12.

## Binding constraints (from the guide)

- **Architecture & contract are immutable** without permission (§0.2, §3): DDD layers `api/core/application/infrastructure`, CQRS, REST+GraphQL coexistence, RealWorld API contract, base `http://localhost:8080` **without** `/api` prefix.
- **Permission protocol §14**: STOP and open/use `needs-decision` issue for SQLite Hibernate dialect vs. DB swap, CQRS read-model strategy in JPA, Joda→`java.time` vs `AttributeConverter`, dependency bumps for SB4/Gradle9, reading-time cache mechanism, `record` conversions that change the contract, and anything not listed in §4/§5.
- **Safety net complete** (§11, §15.2): Phase 2 DONE — REST + GraphQL + flow integration tests frozen. Phase 3 may now begin.
- **Issue before code** (§6): every commit references `#<id>` — Conventional Commits (§7).
- **JDK 17 + Gradle 7.4**: executar `./gradlew test` com `JAVA_HOME=C:\JAVA\jdk17.0.19_10`.

## Next action — Planner (Phase 3)

1. Ler `docs/GUIA-DE-REFATORACAO.md` §15.3 para identificar escopo da Phase 3.
2. Executar `harness/scripts/recommend-tier.py --with-models "<intent Phase 3>"`.
3. Executar Pre-Slice Checklist completo (9 itens).
4. Criar issue de tracking GitHub (template `feature_request.md`) — **antes do código**.
5. Criar `harness/docs/plans/active/phase-3-<slug>/` com `task-card.md`, `spec.md` (com `## Model Profile`), `progress.md`.
6. Propor escopo, slices, Model Profile, budget e stop conditions.
7. Aguardar confirmação do usuário (Mode A) antes de gerar código.

> **Cross-family evaluator**: qualquer slice fact-sensitive (S1) exige evaluator de família diferente do generator. Google/Gemini para generator Anthropic, ou vice-versa.

## Do not

- Do not change architecture, the RealWorld contract, or the GraphQL schema without §14 permission.
- Do not start a 4.x migration before reading §15.3 and executing the Pre-Slice Checklist.
- Do not store real secrets, credentials, or sensitive payloads.
- Do not commit without a tracking issue.
