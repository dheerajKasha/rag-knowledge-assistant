# RAG Knowledge Assistant

Production-style Spring Boot MVP for document ingestion, semantic retrieval, and citation-backed Q&A, with a React + TypeScript chat UI.

## Stack

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- pgvector via Docker Compose
- React 18
- TypeScript
- Vite

## Local startup

1. Copy `.env.example` to `.env`.
2. Start Postgres with pgvector:

```powershell
docker compose up -d
```

3. Ensure Java 21 is available.
4. Install frontend dependencies and build the UI:

```powershell
cd ui
npm.cmd install
npm.cmd run build
cd ..
```

5. Run the backend:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$PWD\.tools\apache-maven-3.9.9\bin;$env:Path"
$repo=(Resolve-Path .m2\repository).Path
mvn "-Dmaven.repo.local=$repo" spring-boot:run
```

The app starts on `http://localhost:8080`.

## Repo-based documents

You can place source documents directly in [data/documents](C:/Users/dheer/Documents/repos/rag-knowledge-assistant/data/documents).

- Spring Boot scans that folder on startup when `DOCUMENT_STARTUP_REFRESH_ENABLED=true`.
- Supported files are `pdf`, `docx`, `txt`, `md`, and `markdown`.
- You can force a reindex with `POST /api/documents/refresh`.

## Frontend development

For local frontend iteration with hot reload:

```powershell
cd ui
npm.cmd install
npm.cmd run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to the Spring backend on `http://localhost:8080`.

## RAG flow

Typical flow in this application:

1. File is uploaded.
2. The app extracts text locally.
3. The app chunks the text locally.
4. Each chunk is sent to the embedding model.
5. Embeddings are stored in `pgvector`.
6. At question time, the user question is embedded.
7. Similar chunks are retrieved from `pgvector`.
8. Only the top retrieved chunks are sent to the chat model.
9. The chat model returns the answer.

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
- `AI_EMBEDDING_BASE_URL`
- `AI_CHAT_BASE_URL`
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

## Frontend build

```powershell
cd ui
npm.cmd run build
cd ..
```

## Current scope

- `POST /api/documents/upload` ingests one file and indexes chunks.
- `GET /api/documents` lists indexed documents for the knowledge-base UI.
- `POST /api/documents/refresh` rescans the repo document folder and refreshes the vector index.
- `POST /api/qa/ask` returns an answer with citations.
- The UI is a React + TypeScript chat workspace with a knowledge-base upload panel.
- Citations include the source document plus page and paragraph metadata.
- Chunk embeddings are stored in Postgres as native `pgvector` columns and queried with vector similarity search.
- Embeddings and answer generation are currently stubbed behind abstractions so a real OpenAI-compatible provider can replace them later.
