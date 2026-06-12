# Phase State

State: DONE (Phase 2)

Current phase: 2 — Safety Net (DONE — 2026-06-12)
Last completed phase: 2 — Safety Net
Next recommended phase: 3 — conforme roadmap §15.3 do guia

> Source of truth for scope: `docs/GUIA-DE-REFATORACAO.md` (§15 roadmap).
> Phase map materialized 2026-06-10 via bootstrap-intake Path A+C.

## Completed phases

| Phase | Completed at | Evidence |
|---|---|---|
| 1 — Fundação | 2026-06-10 | commit `0b441cf` #5; templates em `.github/ISSUE_TEMPLATE/`; DoR/DoD e CC em `docs/` |
| 2 — Safety Net | 2026-06-12 (Turno 21 sign-off) | `evaluation-report.md` Slice 3 DONE; XML: 27 suites, 76/76 testes, 0 falhas; commits `980059d`+`f9fdf74`+`71a6456`; issues `#1`,`#4`,`#7` |

## Active phase

**Nenhuma fase ativa.** Phase 2 concluída. Próximo: Planner inicia Phase 3.

Active plan: nenhum (aguardando Planner Phase 3).

## Completed slices (Phase 2)

| Slice | Completed at | Evidence |
|-------|-------------|---------|
| 1 — springdoc-openapi | 2026-06-11 (Turno 14 sign-off) | `evaluation-report.md` state=DONE; XML: 69/69 testes, `OpenApiContractTest` PASS; commits `4cc546a`+`980059d` #6 |
| 2 — contract-tests | 2026-06-12 (Turno 18 sign-off) | `evaluation-report.md` Slice 2 DONE; XML: 26 suites, 75/75 testes, 5 suites DGS (6 ops GraphQL) PASS; commit `f9fdf74` #4 |
| 3 — integration-tests | 2026-06-12 (Turno 21 sign-off) | `evaluation-report.md` Slice 3 DONE; XML: `RealworldFlowIntegrationTest` tests=1 failures=0; agregação 27 suites / 76 testes / 0 falhas; commit `71a6456` #7 |

## Housekeeping pendente (usuário)

- **Fechar issue `#4`** (Slice 2 OPEN desde Turno 19; classifier bloqueou auto-close — referência: sign-off Turno 18).
- **Fechar issue `#7`** (Slice 3 DONE — sign-off Turno 21).
- **Fechar issue `#1`** (tracking Phase 2 — Phase 2 DONE).
- Mover `harness/docs/plans/active/phase-2-safety-net/` → `harness/docs/plans/completed/phase-2-safety-net/` (opcional).

## Notes

- **Slice 1 DONE** — sign-off formal emitido (Turno 14). XML confirmado: 21 suites, 69 testes, 0 falhas. Gradle 7.4 + JDK 17 baseline guia §1.
- **Slice 2 DONE** — sign-off formal emitido (Turno 18). XML confirmado: 26 suites, 75 testes, 0 falhas; 5 suites DGS cobrem 6 operações GraphQL. Commit `f9fdf74`. AC-1 reclassificado PASS-por-intenção.
- **Slice 3 DONE** — sign-off formal emitido (Turno 21). XML: `RealworldFlowIntegrationTest` PASS; 9 endpoints REST encadeados. Commit `71a6456`.
- **AC-1 gap (resolvido como defeito de spec)**: `graphql-dgs-spring-boot-starter-test:4.9.21` não existe no Maven Central; `DgsQueryExecutor` vem do starter principal (`build.gradle:39`). Intenção funcional do AC satisfeita; nenhuma ação de código necessária.
- **Decisão #3 resolvida (Opção A)**: wrapper em `gradle-7.4-bin.zip`. Commit `fa60c47`.
- **JDK 17** disponível em `C:\JAVA\jdk17.0.19_10`; aplicar `JAVA_HOME`/`PATH` temporários por comando.
- **Issues retroativas (Turno 17)**: `#5` (Phase 1 bootstrap, commit `0b441cf`) e `#6` (Phase 2 Slice 1, commits `4cc546a`+`980059d`) criadas e fechadas em 2026-06-12 para sanar violações de disciplina — GitHub Issues estava desabilitado quando esses commits foram feitos.
- **Harness-internal** `phase-17-router-delegation` plan não relacionado ao produto Spring Boot;
  completo pendente sign-off Category-C humano.
- **Git identity**: committer name/email foram inferidos automaticamente. Configurar com
  `git config --global user.name` / `user.email` se desejado.

## Next-agent instruction

Phase 2 **DONE**. Pré-condição §15.2 do guia satisfeita: safety net REST + GraphQL + flow integrado congelada.

**Próximo passo**: Planner inicia Phase 3 conforme roadmap §15.3.

1. Ler `AGENTS.md` + `docs/GUIA-DE-REFATORACAO.md` §15.3.
2. Executar Pre-Slice Checklist (9 itens) para Phase 3.
3. Abrir issue de tracking antes de qualquer código.
4. Criar `harness/docs/plans/active/phase-3-<slug>/spec.md` com Model Profile.
