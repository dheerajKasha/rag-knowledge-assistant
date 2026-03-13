# RAG Knowledge Assistant

Production-style Spring Boot MVP for document ingestion, semantic retrieval, and citation-backed Q&A.

## Stack

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- pgvector via Docker Compose

## Local startup

1. Copy `.env.example` to `.env`.
2. Start Postgres with pgvector:

```powershell
docker compose up -d
```

3. Ensure Java 21 is available.
4. Run the backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$PWD\.tools\apache-maven-3.9.9\bin;$env:Path"
$repo=(Resolve-Path .m2\repository).Path
mvn "-Dmaven.repo.local=$repo" spring-boot:run
```

The API starts on `http://localhost:8080`.

## Environment variables

The application reads configuration from environment variables with sensible local defaults:

- `APP_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO`
- `MULTIPART_MAX_FILE_SIZE`
- `MULTIPART_MAX_REQUEST_SIZE`
- `AI_PROVIDER`
- `AI_BASE_URL`
- `AI_API_KEY`
- `AI_EMBEDDING_MODEL`
- `AI_CHAT_MODEL`

## Docker Compose

`docker-compose.yml` provisions a local PostgreSQL 16 instance using the `pgvector/pgvector` image and runs an init script that enables the `vector` extension automatically.

## Test

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$PWD\.tools\apache-maven-3.9.9\bin;$env:Path"
$repo=(Resolve-Path .m2\repository).Path
mvn "-Dmaven.repo.local=$repo" test
```

## Current scope

- `POST /api/documents/upload` ingests one file and indexes chunks.
- `POST /api/qa/ask` returns an answer with citations.
- Embeddings and answer generation are currently stubbed behind abstractions so a real OpenAI-compatible provider can replace them later.
