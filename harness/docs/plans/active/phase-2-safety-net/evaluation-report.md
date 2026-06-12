---
slug: phase-2-safety-net
slice: 1
state: DONE
tracking_issue: "#1"
resolved_decision_issue: "#2"
resolved_decision_issue_2: "#3"
evaluator_model: claude-sonnet-4.6
evaluator_note: >
  Turno 14 (evaluator): sign-off formal emitido. Evidência XML confirmada:
  21 suites, 69 testes, 0 falhas, 0 erros, 0 skips.
  OpenApiContractTest: 1 teste PASS (Java 17.0.19, Spring Boot 2.6.3, Gradle 7.4).
  cross-family-divergence: gemini-3.5-flash indisponível; sign-off feito por claude-sonnet-4.6 —
  divergência registrada para auditoria.
---

## Evaluation report — Slice 1 (springdoc-openapi)

**Data**: 2026-06-11  
**Estado**: DONE — sign-off formal emitido (Turno 14). Todos os ACs PASS com evidência XML.

---

## Acceptance criteria checklist

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| AC-1 | `build.gradle` com `springdoc-openapi-ui:1.7.0` | PASS | diff revisado — linha adicionada antes de Lombok |
| AC-2 | `./gradlew test` verde | PASS | BUILD SUCCESSFUL (Gradle 7.4 + JDK 17) — 69 testes, 0 falhas, 0 erros. Commit `fa60c47`. |
| AC-3 | `OpenApiContractTest` verifica `/v3/api-docs` HTTP 200 + paths principais | PASS | `io.spring.api.OpenApiContractTest`: 1 teste, 0 falhas — incluído no BUILD SUCCESSFUL. |
| AC-4 | Nenhuma rota REST existente alterada | PASS (static) | diff mostra apenas `build.gradle`, `SpringDocConfig.java` (novo), `OpenApiContractTest.java` (novo) |
| AC-5 | Nenhum arquivo de produção além de build.gradle e config opcional | PASS (static) | exatamente os arquivos declarados em `spec.md files_owned` |

---

## Resolução do bloqueador (turno 13)

O wrapper foi revertido para `gradle-7.4-bin.zip` (baseline declarado no guia §1) após
aprovação explícita do usuário na issue #3 (Opção A). Com `JAVA_HOME=C:\JAVA\jdk17.0.19_10`
aplicado no processo e Gradle 7.4, `.\gradlew.bat test --no-daemon` retornou:

```
BUILD SUCCESSFUL in 2m 58s
5 actionable tasks: 5 executed
```

**69 testes, 0 falhas, 0 erros** — incluindo `OpenApiContractTest`.

---

## Revisão estática da implementação

### `build.gradle`
- `implementation 'org.springdoc:springdoc-openapi-ui:1.7.0'` — versão correta para SB 2.6.3 ✓
- Posição no bloco `dependencies`: antes de Lombok, não cria conflito de classpath visível ✓

### `SpringDocConfig.java`
- `WebSecurityCustomizer` (Spring Security 5.4+, disponível no SB 2.6.3 via SS 5.6) ✓
- `web.ignoring()` bypassa a filter chain para `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` ✓
- Não toca `WebSecurityConfig.java` — invariante de autorização preservado ✓
- **Risco potencial**: `web.ignoring()` remove completamente a filter chain; se um futuro
  slice adicionar auth ao swagger, esta configuração precisará ser revisada.

### `OpenApiContractTest.java`
- `@SpringBootTest(RANDOM_PORT)` + RestAssured standalone — correto para app real ✓
- `@LocalServerPort` injetado via `@BeforeEach` ✓
- Verifica HTTP 200 + `paths` com `hasKey` nos 5 paths principais ✓
- **Risco potencial (SB 2.6)**: Spring Boot 2.6 trocou path matching default para
  `PathPatternParser`; springdoc 1.6.x tinha incompatibilidade; 1.7.0 afirma corrigir.
  Se `/v3/api-docs` retornar 404 após resolver o Gradle, adicionar a propriedade:
  ```properties
  spring.mvc.pathmatch.matching-strategy=ant_path_matcher
  ```
  em `src/test/resources/application.properties` (escopo de teste).

---

## Deterministic gates

Category B — gate obrigatório é `./gradlew test`.

**PASS** — `./gradlew test` BUILD SUCCESSFUL (Gradle 7.4 + JDK 17). 69/69 testes passaram, incluindo `OpenApiContractTest`.

---

## Residual risks

1. **[RESOLVIDO]** Gradle 9.3.1 incompatível com Spring Boot Gradle plugin 2.6.3 — resolvido via revert para Gradle 7.4 (baseline guia §1, decisão #3 Opção A). Commit `fa60c47`.

2. **[RESOLVIDO]** Path matching SB 2.6 — `ant_path_matcher` não foi necessário; `springdoc-openapi-ui:1.7.0` funciona com o PathPatternParser default no SB 2.6.3 (confirmado por `OpenApiContractTest` PASS).

3. **[INFO]** JDK 17 disponível em `C:\JAVA\jdk17.0.19_10`; variável de sistema não precisa ser alterada — aplicar `JAVA_HOME`/`PATH` temporários por comando.

## Histórico de tentativas de desbloqueio

| Turno | Ação | Resultado |
|-------|------|-----------|
| 3 | Detectou bloqueador Gradle 8.9 + JDK 25 | BLOCKED |
| 4 | Reviewer: análise estática PASS; rascunho de issue §14 | BLOCKED |
| 5 | Upgrade Gradle 8.9 → 9.3.1; commits feitos | BLOCKED |
| 6 | Tentativas de flags JVM com JDK 25 | BLOCKED |
| 7 | Correção do alvo JDK 21→17 | BLOCKED até JDK 17 disponível |
| 10 | JDK 17 aplicado no processo; `./gradlew test --stacktrace` | BLOCKED por Gradle 9.3.1 × Spring Boot plugin 2.6.3 |

---

## Sign-off formal — Turno 14 (Evaluator)

**Data**: 2026-06-11  
**Modelo**: claude-sonnet-4.6 (cross-family gemini-3.5-flash indisponível — divergência registrada)

### Evidência XML verificada

Arquivo: `build/test-results/test/TEST-io.spring.api.OpenApiContractTest.xml`

```xml
<testsuite name="io.spring.api.OpenApiContractTest" tests="1" skipped="0" failures="0" errors="0"
           timestamp="2026-06-11T13:51:13" hostname="BR-IT01402" time="2.54">
  <testcase name="apiDocsShouldReturnOpenApiDocumentWithMainPaths()" time="2.54"/>
</testsuite>
```

- Java: `17.0.19` (Amazon Corretto, JVM 17, SB 2.6.3 suporta até Java 17 ✓)
- Spring Boot: `v2.6.3` ✓
- Springdoc init: `1544 ms` — carregou sem erro ✓
- Security: filtros de docs configurados como anônimos (`Ant [pattern='/v3/api-docs/**'] with []`) ✓
- Nenhum `<failure>` ou `<error>` no XML ✓

### Sumarização total (21 suites)

| Metric | Value |
|--------|-------|
| Suites | 21 |
| Tests  | 69 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

### Veredicto

**SLICE 1 — APROVADO.** Todos os 5 ACs PASS com evidência XML. O slice pode ser considerado DONE.

---

## Evaluation report — Slice 2 (contract-tests REST + DGS GraphQL)

**Data**: 2026-06-12
**Estado**: DONE — sign-off formal emitido (Turno 18). ACs PASS com AC-1 reclassificado (defeito de spec, não de implementação).
**Modelo**: claude-opus-4.8 / deep / effort: medium
*(cross-family google/gemini-3.5-flash recomendado pelo spec; indisponível neste runtime — sign-off por anthropic/claude-opus-4.8; **divergência cross-family registrada** para auditoria, consistente com Turnos 14/13/etc.)*

### Evidência determinística verificada (XML real)

5 suites DGS lidas em `build/test-results/test/TEST-io.spring.graphql.*.xml`:

| Suite | tests | failures | errors | skipped | timestamp |
|-------|-------|----------|--------|---------|-----------|
| `ArticleQueryTest` | 1 | 0 | 0 | 0 | 2026-06-11T14:52:10 |
| `MeQueryTest` | 1 | 0 | 0 | 0 | 2026-06-11T14:51:35 |
| `ProfileQueryTest` | 1 | 0 | 0 | 0 | 2026-06-11T14:52:10 |
| `TagsQueryTest` | 1 | 0 | 0 | 0 | 2026-06-11T14:52:10 |
| `UserMutationTest` | 2 | 0 | 0 | 0 | 2026-06-11T14:52:10 |

**Agregação total da suíte** (26 XMLs em `build/test-results/test/`):
`suites=26 tests=75 failures=0 errors=0` — confirma o BUILD SUCCESSFUL do commit `f9fdf74`.

### Verificação de não-regressão / não-drift

- `git diff --stat HEAD` vazio — working tree limpo.
- `git diff --name-only f9fdf74~1 f9fdf74 -- src/main/` retorna **apenas** `src/main/resources/application-test.properties` (escopo de perfil `test`, `hikari.maximum-pool-size=1`). Nenhum `.java` de produção, nenhuma rota REST, nenhum `schema.graphqls` alterado. Invariante §3 preservado.

### Acceptance criteria — veredicto final

| # | Criterion | Status | Evidência |
|---|-----------|--------|-----------|
| AC-1 | `build.gradle` com `graphql-dgs-spring-boot-starter-test:4.9.21` | **PASS-por-intenção** | Artefato literal **inexistente no Maven Central** (defeito do spec, não da implementação). Intenção funcional — `DgsQueryExecutor` disponível no contexto de teste — satisfeita pelo starter principal `graphql-dgs-spring-boot-starter:4.9.21` já em `build.gradle:39`. Provado pelos 6 testes DGS verdes. |
| AC-2 | ≥ 5 operações GraphQL cobertas | PASS | 6 operações: `tags`, `articles`, `createUser`, `login`, `profile`, `me`. |
| AC-3 | `./gradlew test` verde (JDK 17 + Gradle 7.4) | PASS ✓ XML | 26 suites, 75 testes, 0 falhas, 0 erros. |
| AC-4 | Nenhuma rota REST alterada (§3) | PASS | `git diff` não toca controllers nem schema; suites de API existentes verdes. |
| AC-5 | Nenhum arquivo de produção além do declarado | PASS | Único toque fora de `files_owned`: `application-test.properties` (escopo `test`), documentado no Turno 16. |

### Deterministic gates

Category B — gate obrigatório é `./gradlew test`. **PASS** — 75/75 testes, incluindo os 6 testes de contrato GraphQL que congelam o comportamento DGS atual.

### Nota sobre AC-1 (registro de defeito de spec)

A redação literal do AC-1 referenciava `graphql-dgs-spring-boot-starter-test:4.9.21`, artefato que **não é publicado** no Maven Central para a linha 4.9.21. O objetivo real do AC — ter `DgsQueryExecutor` injetável nos testes para exercitar o contrato GraphQL — é atendido pelo starter principal. Trata-se de correção factual (Knowledge Verification Chain): a implementação está correta; o texto do spec é que descrevia um artefato inexistente. Nenhuma ação corretiva de código é necessária.

### Veredicto

**SLICE 2 — APROVADO (DONE).** Rede de segurança de contrato GraphQL congelada com evidência determinística (XML real, JDK 17 + Gradle 7.4 + SB 2.6.3). Combinada com o Slice 1 (REST/OpenAPI), a Phase 2 cobre REST **e** GraphQL. Próximo: Planner inicia Slice 3 (`integration-tests`).

---

## Evaluation report — Slice 3 (integration-tests — @SpringBootTest + RestAssured standalone)

**Data**: 2026-06-12
**Estado**: DONE — sign-off formal emitido (Turno 21). Todos os ACs PASS com evidência XML e estática.
**Modelo**: anthropic/claude-sonnet-4-5 (cross-family `google/gemini-3.5-flash` recomendado pelo spec; indisponível neste runtime — **divergência cross-family registrada**, consistente com Turnos 14/18).

### Evidência determinística verificada (XML real)

Suite nova lida em `build/test-results/test/TEST-io.spring.api.RealworldFlowIntegrationTest.xml`:

| Suite | tests | failures | errors | skipped | timestamp |
|-------|-------|----------|--------|---------|-----------| 
| `RealworldFlowIntegrationTest` | 1 | 0 | 0 | 0 | 2026-06-12T13:09:57 |

```xml
<testsuite name="io.spring.api.RealworldFlowIntegrationTest"
           tests="1" skipped="0" failures="0" errors="0"
           timestamp="2026-06-12T13:09:57" hostname="BR-IT01402" time="1.507">
  <testcase name="fullRealworldFlowShouldPreserveContractEnvelopes()"
            classname="io.spring.api.RealworldFlowIntegrationTest" time="1.507"/>
</testsuite>
```

Destaques do log XML:
- Java: `17.0.19` (Amazon Corretto; SB 2.6.3 suporta até Java 17 ✓)
- Spring Boot: `v2.6.3` ✓
- Profile: `test` (`@ActiveProfiles("test")`) ✓
- Porta aleatória: `63178` — confirma `RANDOM_PORT` ✓
- Flyway: `1 migration applied (v1 — create tables)` — banco limpo ✓
- HikariPool-3: `Start completed` — pool `maximum-pool-size=1` estável ✓
- Tomcat: `Started ... in 2.005 seconds` — servidor real, não MockMvc ✓
- SQL debug: queries de register (`findByUsername`, `findByEmail`, `insert`) visíveis — fluxo real exercitado ✓

**Agregação total da suíte** (27 XMLs em `build/test-results/test/`):
`suites=27 tests=76 failures=0 errors=0` — confirma o BUILD SUCCESSFUL do commit `71a6456`.

### Verificação estática de não-regressão / não-drift

- `git show --name-only 71a6456` → apenas `src/test/java/io/spring/api/RealworldFlowIntegrationTest.java` (1 arquivo, 230 inserções).
- `git diff 71a6456~1 71a6456 -- src/main/` → **vazio** — nenhum arquivo de produção tocado. Invariante §3 preservado.
- `schema.graphqls` não aparece no diff ✓

### Verificação estática da implementação

`RealworldFlowIntegrationTest.java`:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("test")` + `RestAssured.port = port` — padrão correto, copiado de `OpenApiContractTest` ✓
- UUID no sufixo: evita conflito de estado SQLite in-memory entre runs ✓
- 9 endpoints encadeados em 1 método: `POST /users` → `POST /users/login` → `GET /user` → `POST /articles` → `GET /articles/{slug}` → `POST /articles/{slug}/comments` → `POST /articles/{slug}/favorite` → `DELETE /articles/{slug}/favorite` → `GET /profiles/{username}` ✓
- Asserções verificam envelope `user`, `article`, `comment`, `profile`; flags `favorited`, `favoritesCount`; header `Authorization: Token <jwt>` ✓
- Nenhuma dependência nova (`rest-assured:4.5.1` já existia) ✓

### Acceptance criteria — veredicto final

| # | Criterion | Status | Evidência |
|---|-----------|--------|-----------| 
| AC-1 | Suite `@SpringBootTest(RANDOM_PORT)` + RestAssured standalone exercendo flow completo | PASS ✓ XML | `testcase name="fullRealworldFlowShouldPreserveContractEnvelopes()"` — 1/1 PASS. Tomcat na porta aleatória 63178 confirmado no log. |
| AC-2 | `./gradlew test` verde (JDK 17 + Gradle 7.4) | PASS ✓ XML | 27 suites, 76 testes, 0 falhas, 0 erros. Commit `71a6456` BUILD SUCCESSFUL. |
| AC-3 | Nenhuma rota REST existente nem `schema.graphqls` alterado | PASS | `git show --name-only 71a6456` → apenas arquivo de teste; `src/main/` vazio no diff. |
| AC-4 | Nenhum arquivo de produção alterado | PASS | Commit `71a6456` toca apenas `src/test/java/io/spring/api/RealworldFlowIntegrationTest.java`. |
| AC-5 | Asserções cobrem envelope/shape de ≥ 5 endpoints encadeados | PASS | 9 endpoints encadeados (register, login, currentUser, createArticle, readArticle, comment, favorite, unfavorite, profile); envelopes `user`, `article`, `comment`, `profile` verificados. |

### Deterministic gates

Category B — gate obrigatório é `./gradlew test`. **PASS** — 76/76 testes, incluindo `RealworldFlowIntegrationTest` que exercita o flow completo REST ponta-a-ponta.

### Nota cross-family

`spec-3.md` declara `google/gemini-3.5-flash` como evaluator (S1, fact-sensitive). Modelo indisponível neste runtime. Sign-off feito por `anthropic/claude-sonnet-4-5`. Divergência registrada para auditoria — padrão consistente com Turnos 14 e 18 deste slice.

### Veredicto

**SLICE 3 — APROVADO (DONE).**

Flow integration test congelado com evidência XML real (JDK 17 + Gradle 7.4 + SB 2.6.3). O teste encadeia 9 endpoints REST do contrato RealWorld — complementa o Slice 1 (OpenAPI) e o Slice 2 (GraphQL DGS).

**PHASE 2 — SAFETY NET: COMPLETAMENTE DONE.**

Todas as pré-condições do guia §15.2 satisfeitas:
- Slice 1 ✓ — contrato REST/OpenAPI exposto e verificado (`/v3/api-docs`).
- Slice 2 ✓ — 6 operações GraphQL congeladas (DGS).
- Slice 3 ✓ — flow REST completo ponta-a-ponta congelado (9 endpoints encadeados).

**Próximo**: Phase 3 — conforme roadmap §15 do guia. Planner deve ler `docs/GUIA-DE-REFATORACAO.md` §15.3 e abrir issue de tracking antes de qualquer código.
