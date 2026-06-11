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
| 1 — Fundação | 2026-06-10 | commit `0b441cf`; templates em `.github/ISSUE_TEMPLATE/`; DoR/DoD e CC em `docs/` |

## Active phase

Phase 2 — Safety Net. See `harness/docs/harness/phases/phase-2.md`.
Active plan: `harness/docs/plans/active/phase-2-safety-net/`.
Tracking issue: `#1`.
Slice 2 issue: `#4` — `feat: contract-tests — REST @WebMvcTest estendidos + testes DGS GraphQL`.
Resolved decision issue: `#2` — JDK 17 localizado em `C:\JAVA\jdk17.0.19_10`.
Resolved decision issue: `#3` — wrapper revertido para `gradle-7.4-bin.zip`. Commit `fa60c47`.

## Completed slices (Phase 2)

| Slice | Completed at | Evidence |
|-------|-------------|---------|
| 1 — springdoc-openapi | 2026-06-11 (Turno 14 sign-off) | `evaluation-report.md` state=DONE; XML: 69/69 testes, `OpenApiContractTest` PASS |

## Active slice (Phase 2)

| Slice | Started at | State | Issue | Spec |
|-------|-----------|-------|-------|------|
| 2 — contract-tests | 2026-06-11 (Turno 15 planner) | EVALUATING | #4 | `spec-2.md` |

## Notes

- **Slice 1 DONE** — sign-off formal emitido (Turno 14). XML confirmado: 21 suites, 69 testes, 0 falhas. Gradle 7.4 + JDK 17 baseline guia §1.
- **Slice 2 EVALUATING** — Generator (Turno 16) criou 5 suites DGS. 75 testes, 0 falhas. Commit `f9fdf74`. Aguarda sign-off do evaluator.
- **AC-1 gap**: `graphql-dgs-spring-boot-starter-test:4.9.21` não existe no Maven Central; `DgsQueryExecutor` vem do starter principal.
- **Decisão #3 resolvida (Opção A)**: wrapper em `gradle-7.4-bin.zip`. Commit `fa60c47`.
- **JDK 17** disponível em `C:\JAVA\jdk17.0.19_10`; aplicar `JAVA_HOME`/`PATH` temporários por comando.
- **Harness-internal** `phase-17-router-delegation` plan não relacionado ao produto Spring Boot;
  completo pendente sign-off Category-C humano.
- **Git identity**: committer name/email foram inferidos automaticamente. Configurar com
  `git config --global user.name` / `user.email` se desejado.

## Next-agent instruction

Resume via `next-agent-briefing.md`. O constraint de ordenação (guide §15) é vinculante:
**Phase 2 (safety net) deve ser concluída antes de qualquer migração 4.x.**

**Próximo passo imediato**: Evaluator (cross-family, Turno 17) verifica XMLs de teste, confirma AC-2/AC-3, avalia AC-1 gap e emite sign-off do Slice 2.

Phase 2 foco: testes de contrato REST e GraphQL que garantam que nenhuma migração
quebre o contrato RealWorld sem ser detectada.
