# Phase State

State: IMPLEMENTING

Current phase: 2 — Safety Net
Last completed phase: 1 — Fundação
Next recommended phase: 2 — Safety Net (retomar após decisão #3 e reconciliação do Gradle wrapper)

> Source of truth for scope: `docs/GUIA-DE-REFATORACAO.md` (§15 roadmap).
> Phase map materialized 2026-06-10 via bootstrap-intake Path A+C.

## Completed phases

| Phase | Completed at | Evidence |
|---|---|---|
| 1 — Fundação | 2026-06-10 | commit `0b441cf` #5; templates em `.github/ISSUE_TEMPLATE/`; DoR/DoD e CC em `docs/` |

## Active phase

Phase 2 — Safety Net. See `harness/docs/harness/phases/phase-2.md`.
Active plan: `harness/docs/plans/active/phase-2-safety-net/`.
Tracking issue: `#1`.
Slice 2 issue: `#4` — `feat: contract-tests …` (Slice 2 DONE; **issue ainda OPEN — fechar manualmente**, classifier bloqueou o close automático no Turno 19).
Slice 3 issue: `#7` — `feat: integration-tests — @SpringBootTest + RestAssured standalone dos flows principais`.
Resolved decision issue: `#2` — JDK 17 localizado em `C:\JAVA\jdk17.0.19_10`.
Resolved decision issue: `#3` — wrapper revertido para `gradle-7.4-bin.zip`. Commit `fa60c47`.

## Completed slices (Phase 2)

| Slice | Completed at | Evidence |
|-------|-------------|---------|
| 1 — springdoc-openapi | 2026-06-11 (Turno 14 sign-off) | `evaluation-report.md` state=DONE; XML: 69/69 testes, `OpenApiContractTest` PASS; commits `4cc546a`+`980059d` #6 |
| 2 — contract-tests | 2026-06-12 (Turno 18 sign-off) | `evaluation-report.md` Slice 2 DONE; XML: 26 suites, 75/75 testes, 5 suites DGS (6 ops GraphQL) PASS; commit `f9fdf74` #4 |

## Active slice (Phase 2)

| Slice | Started at | State | Issue | Spec |
|-------|-----------|-------|-------|------|
| 3 — integration-tests | 2026-06-12 (Turno 19 planner) | EVALUATING | #7 | `spec-3.md` |

## Notes

- **Slice 1 DONE** — sign-off formal emitido (Turno 14). XML confirmado: 21 suites, 69 testes, 0 falhas. Gradle 7.4 + JDK 17 baseline guia §1.
- **Slice 2 DONE** — sign-off formal emitido (Turno 18). XML confirmado: 26 suites, 75 testes, 0 falhas; 5 suites DGS cobrem 6 operações GraphQL. Commit `f9fdf74`. AC-1 reclassificado PASS-por-intenção.
- **AC-1 gap (resolvido como defeito de spec)**: `graphql-dgs-spring-boot-starter-test:4.9.21` não existe no Maven Central; `DgsQueryExecutor` vem do starter principal (`build.gradle:39`). Intenção funcional do AC satisfeita; nenhuma ação de código necessária.
- **Decisão #3 resolvida (Opção A)**: wrapper em `gradle-7.4-bin.zip`. Commit `fa60c47`.
- **JDK 17** disponível em `C:\JAVA\jdk17.0.19_10`; aplicar `JAVA_HOME`/`PATH` temporários por comando.
- **Issues retroativas (Turno 17)**: `#5` (Phase 1 bootstrap, commit `0b441cf`) e `#6` (Phase 2 Slice 1, commits `4cc546a`+`980059d`) criadas e fechadas em 2026-06-12 para sanar violações de disciplina — GitHub Issues estava desabilitado quando esses commits foram feitos.
- **Harness-internal** `phase-17-router-delegation` plan não relacionado ao produto Spring Boot;
  completo pendente sign-off Category-C humano.
- **Git identity**: committer name/email foram inferidos automaticamente. Configurar com
  `git config --global user.name` / `user.email` se desejado.

## Next-agent instruction

Resume via `next-agent-briefing.md`. O constraint de ordenação (guide §15) é vinculante:
**Phase 2 (safety net) deve ser concluída antes de qualquer migração 4.x.**

**Próximo passo imediato**: Evaluator (Turno 21) verifica evidência XML do Turno 20 (27 suites / 76 testes / 0 falhas; `RealworldFlowIntegrationTest` PASS) e emite sign-off formal do Slice 3. Se aprovado, Phase 2 Safety Net estará DONE.

Phase 2 foco: testes de contrato REST e GraphQL que garantam que nenhuma migração
quebre o contrato RealWorld sem ser detectada.
