# DummyBackend (Generator Service)

Spring Boot API that generates realistic dummy data from a JSON schema, backed by selectable LLM providers (OpenAI, Claude, Grok, Groq).

## Tech Stack

- **Java 17**
- **Spring Boot 4.1** (Web, Validation)
- **Maven**
- **Lombok**
- **LLM providers** via HTTP (`RestClient`) — OpenAI, Claude (Anthropic), Grok (xAI), Groq (free-tier, recommended for local testing)

## Project Structure

```
src/main/java/com/dummybackend/generatorservice/
├── GeneratorserviceApplication.java
├── config/
│   └── LlmConfig.java                 # Enables LLM configuration properties
├── controller/
│   ├── GenerateController.java        # POST /generate
│   └── HealthController.java          # GET /health
├── dto/
│   ├── GenerateRequest.java           # provider + count + schema
│   └── GenerateResponse.java          # count + data
├── llm/
│   ├── LlmProvider.java               # Provider strategy interface
│   ├── LlmProviderRegistry.java       # Resolves provider per request
│   ├── LlmProvidersProperties.java    # Per-provider config
│   ├── OpenAiLlmProvider.java
│   ├── ClaudeLlmProvider.java
│   ├── GrokLlmProvider.java           # xAI Grok
│   ├── GroqLlmProvider.java           # Groq (free tier)
│   └── ChatCompletionsSupport.java    # Shared OpenAI-compatible helpers
├── service/
│   ├── GeneratorService.java          # Orchestrates prompt + LLM + parse
│   ├── PromptBuilder.java
│   └── SchemaValidator.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── SchemaValidationException.java
    ├── UnknownLlmProviderException.java
    ├── LlmGenerationException.java
    └── ErrorResponse.java
```

## Prerequisites

- JDK 17+
- Maven 3.9+ (or use the included Maven Wrapper)
- API key(s) for the provider(s) you use:
  - `GROQ_API_KEY` (default provider; free tier, good for local testing)
  - `OPENAI_API_KEY`
  - `ANTHROPIC_API_KEY` (Claude)
  - `GROK_API_KEY` (xAI Grok — not the same as Groq)

Unused providers do not need keys; the key is checked only when that provider is selected.

## Secrets (`.env`)

API keys are read from environment variables (see `application.properties`). Locally, put them in a project-root **`.env`** file — Spring Boot loads it via `springboot4-dotenv` (real OS env vars still take precedence).

1. Copy `.env.example` → `.env`
2. Fill in the keys you need (at least `GROQ_API_KEY` for the default provider)
3. Keep `.env` out of git (already in `.gitignore`)

## Configuration

Set in `src/main/resources/application.properties`, `.env`, or OS environment variables:

| Property | Description | Default |
|---|---|---|
| `server.port` | HTTP port | `8000` |
| `llm.default-provider` | Used when request omits `provider` | `groq` |
| `llm.openai.*` | `base-url`, `api-key`, `model`, `max-tokens` | see `application.properties` |
| `llm.claude.*` | same | … |
| `llm.grok.*` | same (xAI) | … |
| `llm.groq.*` | same (Groq free tier) | … |

API keys are env-backed (from `.env` or the real environment):

```properties
llm.groq.api-key=${GROQ_API_KEY:}
llm.openai.api-key=${OPENAI_API_KEY:}
llm.claude.api-key=${ANTHROPIC_API_KEY:}
llm.grok.api-key=${GROK_API_KEY:}
```

**Note:** `groq` (Groq) and `grok` (xAI) are different providers.

## Run

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
.\mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8000**.

## API

### Health check

```http
GET /health
```

Response: `Ok`

### Generate dummy data

```http
POST /generate
Content-Type: application/json
```

**Request body**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `provider` | string | no (defaults to `groq`) | `openai`, `claude`, `grok`, or `groq` |
| `count` | integer | no (defaults to `1`) | 1–50; response always contains exactly this many records |
| `schema` | object | yes | 1–30 fields |

**Exact count:** If the LLM returns too many records, extras are truncated. If it returns too few, the service retries once with a corrective prompt; if still short, the API responds with **502**.

**Example**

```json
{
  "provider": "groq",
  "count": 3,
  "schema": {
    "name": "string",
    "email": "string",
    "age": {
      "type": "integer",
      "min": 18,
      "max": 65
    },
    "status": {
      "type": "enum",
      "values": ["active", "inactive", "pending"]
    },
    "dob": "date",
    "verified": "boolean",
    "score": {
      "type": "float",
      "min": 0,
      "max": 100
    }
  }
}
```

**Response**

```json
{
  "count": 3,
  "data": [
    {
      "name": "Aisha Khan",
      "email": "aisha.khan@example.com",
      "age": 29,
      "status": "active",
      "dob": "1996-03-14",
      "verified": true,
      "score": 87.5
    }
  ]
}
```

## Schema Format

Each field in `schema` can be a shorthand type string or an object:

- Shorthand: `"age": "integer"` → `{ "type": "integer" }`
- Full: `"age": { "type": "integer", "min": 18, "max": 65 }`

**Allowed types:** `string`, `integer`, `float`, `boolean`, `date`, `enum`

| Constraint | Applies to | Notes |
|---|---|---|
| `type` | all | required |
| `min` / `max` | `integer`, `float` | `min` must not exceed `max` |
| `values` | `enum` | required non-empty array |
| `description` | any | optional hint for the LLM |

Max **30** fields per schema.

## Error Responses

All errors use:

```json
{
  "message": "...",
  "details": ["..."]
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Invalid request body, schema, or unknown `provider` |
| `502 Bad Gateway` | LLM call or parse failure |

## Adding a new provider

1. Add a `@Component` implementing `LlmProvider` (`id()` + `complete(prompt)`).
2. Add its config slice under `llm.<id>.*` in `application.properties`.
3. No changes needed to `GeneratorService` or `LlmProviderRegistry`.
