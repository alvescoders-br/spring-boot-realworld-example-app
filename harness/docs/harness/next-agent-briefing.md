# Next Agent Briefing

State: DONE (Phase 3 / Slice 2) — Turno 38 human sign-off; commit + close #11; issues #13/#14 abertas

## Resume order

1. Ler `AGENTS.md`.
2. Se usar Claude Code, ler `CLAUDE.md`.
3. Ler `harness/docs/harness/START_HERE.md`.
4. Ler `docs/GUIA-DE-REFATORACAO.md` — **the project source of truth** (precedence over generic best practices; §0).
5. Ler `harness/docs/harness/source-map.md` e `harness/docs/harness/phase-state.md`.
6. Ler o plano concluído (referência): `harness/docs/plans/active/phase-3-plataforma/spec.md`, `task-card.md`, `progress.md`, `evaluation-report.md`.

## Current situation — Phase 3 DONE

**Phase 2 — Safety Net: DONE** (Turno 21, evaluator sign-off 2026-06-12).
**Phase 3 — Plataforma: DONE** (Turno 38, human sign-off 2026-06-15).

### Phase 3 slices — resumo

| Slice | Status | Evidência |
|-------|--------|-----------| 
| 1 — Gradle 9 (revertido/re-seq ADR-0003) | DONE (Turno 30) | Gradle 7.4 restaurado; XML 27/76/0; ADR-0003 accepted |
| 2 — Spring Boot 4 + Java 25 + Gradle 9 | DONE (Turno 38) | 8 gates; 27/76/0; Evaluator + Reviewer + Security Reviewer PASS; commit #11; #11 fechada |

### Evidência Slice 2 — resumo de papéis

| Papel | Turno | Decisão |
|-------|-------|---------|
| Generator | 34 | Implementação completa (SB4/Java25/Gradle9/Jakarta/SecurityFilterChain) |
| Evaluator (cross-family, Google) | 35 | PASS — 8 gates; 27/76/0 |
| Reviewer (Anthropic) | 36 | PASS — migração multi-arquivo; matriz autorização preservada |
| Security Reviewer (cross-family, Google) | 37 | PASS — 5 itens auditados; sem regressões |
| Human sign-off | 38 | APROVADO — `Aprovado. Conclua` |

## Issues open

| # | Título | Tipo |
|---|--------|------|
| #4 | Phase 2 Slice 2 tracking | OPEN (housekeeping pendente — sign-off foi Turno 18) |
| #7 | Phase 2 Slice 3 tracking | OPEN (housekeeping pendente — sign-off foi Turno 21) |
| #1 | Phase 2 tracking | OPEN (housekeeping pendente — Phase 2 DONE) |
| #13 | Rate limiting em `/users/login` | OPEN (future-work, security) |
| #14 | `/graphiql` hardening para ambientes não-demo | OPEN (future-work, enhancement) |

## Next action — aguardando instrução humana

Não há slice ativo. O próximo agente deve aguardar instrução explícita do humano para:
- Iniciar o próximo slice/fase conforme `docs/GUIA-DE-REFATORACAO.md` §15 roadmap.
- Trabalhar nas issues futuras #13 ou #14.
- Housekeeping de issues antigas (#1, #4, #7).

## Do not

- Do not start a new slice without explicit human instruction.
- Do not commit without referencing the relevant issue.
- Do not change architecture, DDD/CQRS layering, RealWorld REST contract, GraphQL schema, or auth semantics without a new §14 decision.
- Do not store real secrets, credentials, personal data, private prompts, or sensitive payloads.
