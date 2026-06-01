# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`qa-junior-ai-tool` is a multi-module Spring Boot 3 + Spring AI application that analyzes requirements documents using the [A2A (Agent-to-Agent) Protocol](https://github.com/google-a2a/A2A). A user submits a document; the Host Orchestrator fans it out in parallel to two AI agents running Ollama locally; results are aggregated and persisted.

**Modules:**
- `common` — shared DTOs, A2A protocol records, and custom exceptions
- `host-orchestrator` — entry point (:8080), orchestrates the pipeline, exposes REST + A2A endpoints, persists conversations
- `tester-agent` (:8081) — generates structured QA test cases via Ollama/Spring AI
- `analyst-agent` (:8082) — produces risk analysis via Ollama/Spring AI
- `a2a-ui/` — Next.js v15 + MUI v7 web UI (separate npm project, see `a2a-ui/CLAUDE.md`)

## Build & Run Commands

```bash
# Build all modules from root
mvn package -DskipTests

# Run tests (only host-orchestrator has tests; uses WireMock, no real Ollama needed)
mvn test -pl host-orchestrator

# Run a single test class
mvn test -pl host-orchestrator -Dtest=AnalysisFlowIntegrationTest

# Run the full stack (recommended: uses healthchecks, pulls qwen2.5:7b)
docker compose up --build

# Run with lighter phi3:mini model (faster startup, no healthchecks)
docker compose -f docker-compose-simple.yml up --build

# a2a-ui development (from a2a-ui/)
npm run dev      # dev server
npm run build    # production build
npm run lint     # ESLint
```

## Architecture

### Request Flow

```
POST /api/analyze
  → OrchestrationService
      → A2AAgentClient.sendTask(testerUrl) ─┐  (parallel CompletableFuture)
      → A2AAgentClient.sendTask(analystUrl) ─┘
  → aggregate + persist to Conversation (H2/PostgreSQL)
  → AnalysisResponse { conversationId, testerResponse, analystResponse, analyzedAt }
```

The Orchestrator also exposes:
- `GET /api/history` — paginated conversation history
- `GET /api/agents` — discovered agent cards
- `GET /.well-known/agent-card.json` — A2A protocol discovery endpoint
- `GET /swagger-ui` — OpenAPI docs

### A2A Protocol

Agents communicate via JSON-RPC 2.0 (`tasks/send` method). The `A2AAgentClient` in host-orchestrator builds the request manually rather than using the A2A SDK — see the comment in that file for the rationale. Agent metadata is fetched at startup from `/.well-known/agent-card.json` by `AgentDiscoveryService`.

### Resilience

Calls to agents use a Resilience4j chain (outer → inner): **CircuitBreaker → TimeLimiter (30s) → Retry (3×, exponential back-off)**. The fallback converts any exception to `AgentUnavailableException`. The thread pool for async calls is capped at 4 (`agentCallExecutor`) — relevant if you see blocked threads under load.

### Profiles

- `dev` — H2 in-memory database (used automatically by integration tests)
- `prod` — configure an external datasource via env vars

### Environment Variables (host-orchestrator)

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama endpoint |
| `AGENTS_TESTER_URL` | `http://localhost:8081` | Tester agent URL |
| `AGENTS_ANALYST_URL` | `http://localhost:8082` | Analyst agent URL |
| `SPRING_PROFILES_ACTIVE` | (none) | Set to `prod` in Docker |

### Integration Tests

`AnalysisFlowIntegrationTest` spins up two WireMock servers as stand-ins for the agents and uses `@DynamicPropertySource` to point the orchestrator at them. No Ollama process is needed. Tests run with `spring.profiles.active=dev` (H2).

## Key Dependencies

- Spring Boot 3.3.5, Spring AI 1.0.0-M6 (Ollama chat)
- Resilience4j 2.2.0 (circuit breaker / retry / time limiter)
- WireMock Spring Boot 3.1.0 (integration tests)
- Lombok, springdoc-openapi (Swagger UI), logstash-logback-encoder (prod JSON logs)
