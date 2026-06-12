# Next Agent Briefing

State: EVALUATING

## Resume order

1. Read `AGENTS.md`.
2. If using Claude Code, read `CLAUDE.md`.
3. Read `harness/docs/harness/START_HERE.md`.
4. Read `docs/GUIA-DE-REFATORACAO.md` — **the project source of truth** (precedence over generic best practices; §0).
5. Read `harness/docs/harness/source-map.md` and `harness/docs/harness/phase-state.md`.
6. Read the current phase file `harness/docs/harness/phases/phase-2.md`.
7. Read the active plan files under `harness/docs/plans/active/phase-2-safety-net/`.
8. Resume from GitHub issue `#7`; Evaluator role (Turno 21) verifica evidência XML e emite sign-off do Slice 3.

## Current situation

Phase 2 — Safety Net is active. **Slice 1 (`springdoc-openapi`) is DONE** (Turno 14).
**Slice 2 (`contract-tests`) is DONE** — evaluator sign-off emitted at Turno 18 (26 suites, 75 tests, 0 failures; 6 GraphQL ops frozen).
**Slice 3 (`integration-tests`) is EVALUATING** — Generator completed at Turno 20 (`RealworldFlowIntegrationTest.java` + commit `71a6456`); Evaluator is the next role.

Tracking issue: `#1` — `feat: planejar safety net de contrato da Phase 2`.
Slice 2 issue: `#4` — `feat: contract-tests — REST @WebMvcTest estendidos + testes DGS GraphQL`. **Housekeeping pendente: fechar manualmente (classifier bloqueou o close automático no Turno 19).**
Slice 3 issue: `#7` — `feat: integration-tests — @SpringBootTest + RestAssured standalone dos flows principais`.
Resolved decision issue: `#2` — JDK 17 exists at `C:\JAVA\jdk17.0.19_10`.
Resolved decision issue: `#3` — wrapper reverted to `gradle-7.4-bin.zip` (guide §1 baseline). Commit `fa60c47`.
Retroactive issue: `#5` — Phase 1 Fundação bootstrap (commit `0b441cf`). Closed 2026-06-12.
Retroactive issue: `#6` — Phase 2 Slice 1 springdoc (commits `4cc546a`+`980059d`). Closed 2026-06-12.

Turno 20 result (Slice 3 Generator):
- `RealworldFlowIntegrationTest.java` criado em `src/test/java/io/spring/api/`.
- `@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("test")` + RestAssured standalone.
- 1 método de teste encadeia 9 endpoints: register → login → GET /user → POST /articles → GET /articles/{slug} → POST comments → POST favorite → DELETE favorite → GET profile.
- `./gradlew test` BUILD SUCCESSFUL: **27 suites, 76 testes, 0 falhas, 0 erros**. Commit `71a6456 #7`.
- Nenhuma dependência nova; nenhum arquivo de produção alterado. AC-1 a AC-5 todos PASS.

## Active artifacts

- `harness/docs/plans/active/phase-2-safety-net/task-card.md`
- `harness/docs/plans/active/phase-2-safety-net/spec.md` (Slice 1 — reference)
- `harness/docs/plans/active/phase-2-safety-net/spec-2.md` (Slice 2 — reference)
- `harness/docs/plans/active/phase-2-safety-net/spec-3.md` (Slice 3 — **active**)
- `harness/docs/plans/active/phase-2-safety-net/progress.md`
- `harness/docs/plans/active/phase-2-safety-net/evaluation-report.md`

## Binding constraints (from the guide)

- **Architecture & contract are immutable** without permission (§0.2, §3): DDD layers `api/core/application/infrastructure`, CQRS, REST+GraphQL coexistence, RealWorld API contract, base `http://localhost:8080` **without** `/api` prefix.
- **Permission protocol §14**: STOP and open/use `needs-decision` issue for SQLite Hibernate dialect vs. DB swap, CQRS read-model strategy in JPA, Joda→`java.time` vs `AttributeConverter`, dependency bumps for SB4/Gradle9, reading-time cache mechanism, `record` conversions that change the contract, and anything not listed in §4/§5.
- **Safety net first** (§11, §15): freeze REST+GraphQL contract with tests **before** any 4.x migration.
- **Issue before code** (§6): every commit references `#<id>` — use `#7` for Slice 3 commits; Conventional Commits (§7).
- **JDK 17 + Gradle 7.4**: executar `./gradlew test` com `JAVA_HOME=C:\JAVA\jdk17.0.19_10`.

## Next action — Evaluator (Slice 3 — integration-tests, Turno 21)

1. Ler `spec-3.md` (ACs, escopo do flow) e este briefing.
2. Verificar XML determinístico em `build/test-results/test/TEST-io.spring.api.RealworldFlowIntegrationTest.xml`:
   - Confirmar `tests="1" failures="0" errors="0" skipped="0"`.
3. Verificar agregação total: 27 XMLs, 76 testes, 0 falhas, 0 erros.
4. Verificar `git diff 71a6456~1 71a6456 -- src/main/` — deve ser vazio (nenhum arquivo de produção tocado).
5. Verificar `git diff 71a6456~1 71a6456 -- src/test/` — apenas `RealworldFlowIntegrationTest.java` criado.
6. Emitir veredicto dos 5 ACs em `evaluation-report.md` (§ Slice 3).
7. Atualizar `task-card.md`, `phase-state.md` e `next-agent-briefing.md` com estado DONE do Slice 3.
8. Se todos os ACs PASS → Phase 2 Safety Net DONE. Indicar próxima fase no briefing.

> **Cross-family evaluator**: `spec-3.md` declara `google/gemini-3.5-flash` como evaluator (S1, fact-sensitive). Se indisponível, registrar divergência e prosseguir com avaliador disponível.

## Do not

- Do not change architecture, the RealWorld contract, or the GraphQL schema without §14 permission.
- Do not start a 4.x migration before the Phase 2 safety net is green on REST **and** GraphQL.
- Do not store real secrets, credentials, or sensitive payloads.
