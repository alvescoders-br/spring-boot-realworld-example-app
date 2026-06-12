---
slug: phase-2-safety-net
phase: 2
title: "Phase 2 — Safety Net: OpenAPI + contract tests REST + GraphQL + integration tests"
status: DONE  # Todos os slices concluídos; sign-off Turno 21 (evaluator)
risk_category: B
path: standard_path_B
created_at: 2026-06-10
completed_at: 2026-06-12
---

## Objetivo

Congelar o comportamento observável atual do contrato REST **e** GraphQL como rede
de segurança automatizada **antes** de qualquer migração 4.x. Pré-condição vinculante
para as Phases 3–6 (§15.2 do GUIA).

## Slices planejados

| # | Slice | Escopo | Risco | Status |
|---|-------|--------|-------|--------|
| 1 | `springdoc-openapi` | Adicionar dep + verificar `/v3/api-docs` expõe contrato completo | B | **DONE** (sign-off 2026-06-11) |
| 2 | `contract-tests` | Estender @WebMvcTest REST + novos testes DGS GraphQL | B | **DONE** (sign-off 2026-06-12, Turno 18) |
| 3 | `integration-tests` | @SpringBootTest + RestAssured standalone, flows principais | B | **DONE** (sign-off 2026-06-12, Turno 21) |

## Invariants to preserve (§3)

- Rotas, métodos, regras de auth, header `Authorization: Token <jwt>`,
  envelopes `@JsonRootName`/`UNWRAP_ROOT_VALUE` e shapes de resposta: **tudo congelado exatamente como está**.

## Out of scope

- Soft delete, reading-time, cache (Phase 6).
- Qualquer migração (Gradle/Java/Spring/JPA).
