# Document Management Service

REST API for uploading, searching, and downloading large PDF documents. Built with Spring Boot 3.2.5, PostgreSQL, and MinIO.

## Stack

- **Java 17** + Spring Boot 3.2.5
- **PostgreSQL 15** — document metadata
- **MinIO** — PDF binary storage (S3-compatible)
- **Docker + docker-compose** — full local stack

## Prerequisites

- Docker Desktop running
- Ports `8080`, `5432`, `9000`, `9001` available

## Running the stack

```bash
docker-compose up --build
```

That's it. The command builds the app image, starts PostgreSQL and MinIO, creates the bucket, and launches the service. Wait until you see the service healthcheck pass (~30 seconds on first build).

To stop:

```bash
docker-compose down
```

To stop and delete all data (volumes):

```bash
docker-compose down -v
```

## Verifying it's up

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP","components":{"db":{"status":"UP"},...}}
```

## API

Interactive documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/documents` | Upload a PDF document |
| `GET` | `/api/v1/documents` | Search documents with optional filters |
| `GET` | `/api/v1/documents/{id}/download` | Get a temporary presigned download URL |

### Upload a document

```bash
curl -X POST "http://localhost:8080/api/v1/documents" \
  -F "file=@/path/to/document.pdf;type=application/pdf" \
  -F "userId=alice" \
  -F "documentName=report.pdf" \
  -F "tags=finance" \
  -F "tags=2024"
```

Response `201 Created`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "alice",
  "documentName": "report.pdf",
  "tags": ["finance", "2024"],
  "fileSize": 1048576,
  "fileType": "application/pdf",
  "createdAt": "2026-04-26T10:00:00"
}
```

### Search documents

```bash
# All documents for a user
curl "http://localhost:8080/api/v1/documents?userId=alice"

# Filter by name (partial, case-insensitive) and tag
curl "http://localhost:8080/api/v1/documents?userId=alice&documentName=report&tags=finance"

# Pagination
curl "http://localhost:8080/api/v1/documents?page=0&size=20"
```

Response `200 OK`:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Get a download URL

```bash
curl "http://localhost:8080/api/v1/documents/550e8400-e29b-41d4-a716-446655440000/download"
```

Response `200 OK`:

```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "url": "http://localhost:9000/document-bucket/alice/.../report.pdf?X-Amz-Signature=...",
  "expiresInMinutes": 60
}
```

The URL can be opened directly in a browser or used with any HTTP client. It expires after 60 minutes.

## Running tests

Unit and controller tests (no Docker required):

```bash
./mvnw test -Dtest="DocumentServiceTest,DocumentControllerTest"
```

Full suite including integration tests (requires Docker):

```bash
./mvnw test
```

Integration tests use Testcontainers to spin up real PostgreSQL and MinIO containers. They are automatically skipped if Docker is not available.

Generate coverage report:

```bash
./mvnw jacoco:report
# open target/site/jacoco/index.html
```

Apply code formatting:

```bash
./mvnw spotless:apply
```

## Configuration

All settings are externalized via environment variables. Defaults work out of the box for local development.

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/documentdb` | JDBC connection URL |
| `DATABASE_USERNAME` | `postgres` | DB username |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `MINIO_ENDPOINT` | `http://localhost:9000` | Internal MinIO endpoint |
| `MINIO_PUBLIC_ENDPOINT` | _(same as endpoint)_ | Public-facing endpoint for presigned URLs |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `MINIO_BUCKET` | `document-bucket` | Target bucket name |
| `MINIO_PRESIGNED_EXPIRY_MINUTES` | `60` | Presigned URL expiry time |
| `SERVER_PORT` | `8080` | HTTP port |

## MinIO console

A web UI for browsing stored files is available at:

```
http://localhost:9001
```

Credentials: `minioadmin` / `minioadmin`

## Inspecting the database

Connect to the running PostgreSQL container:

```bash
docker exec -it document-postgres psql -U postgres -d documentdb
```

Useful queries once inside:

```sql
-- list all uploaded documents
SELECT id, user_id, document_name, file_size, created_at FROM documents;

-- list documents with their tags
SELECT d.document_name, t.tag
FROM documents d
JOIN document_tags t ON t.document_id = d.id;

-- count documents per user
SELECT user_id, COUNT(*) FROM documents GROUP BY user_id;

-- exit
\q
```

Or run a one-liner without entering the container:

```bash
docker exec document-postgres psql -U postgres -d documentdb \
  -c "SELECT id, user_id, document_name, created_at FROM documents;"
```

You can also connect with any GUI client (DBeaver, TablePlus, DataGrip) using:

| Field | Value |
|-------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `documentdb` |
| Username | `postgres` |
| Password | `postgres` |

---

## Additional Comments

### Memory constraint interpretation

The challenge specifies a 50 MB memory limit for the service container. In practice, the Spring Boot JVM with its dependencies (Spring MVC, JPA, MinIO SDK, Hibernate, etc.) requires roughly 200–300 MB of memory just to start — this accounts for JVM metaspace, code cache, thread stacks, and GC overhead. A hard 50 MB container limit would prevent the process from booting at all, regardless of the implementation.

The `docker-compose.yml` therefore uses `mem_limit: 512m`, which is the realistic minimum for this stack.

The **intent** of the constraint — and the actual engineering challenge — is that memory usage must not scale with the size of the uploaded file. A naive implementation that buffers the entire multipart body in heap would crash on a 500 MB upload with any reasonable memory setting. This service solves that problem:

1. **Multipart spooling to disk**: `spring.servlet.multipart.file-size-threshold=0B` forces Spring/Tomcat to write the incoming request body directly to a temporary file on disk before any application code runs. The JVM heap never sees the file contents.

2. **Streaming to MinIO**: the file is read from the temp file as an `InputStream` and forwarded to MinIO using the SDK's internal multipart upload. The SDK transfers the data in fixed-size chunks. Peak heap usage for a 500 MB upload is identical to peak heap usage for a 1 KB upload.

3. **Concurrent safety**: ten simultaneous 500 MB uploads means ten temp files on disk. Each request is isolated; there is no shared mutable state. Heap usage remains flat regardless of concurrency.

The result is a service where heap pressure is determined by request concurrency and query result sizes, not by file sizes — which is the correct behaviour for a production document storage service.

### MinIO path structure

The challenge example shows `user1/doc1.pdf`. This implementation uses `user1/{documentId}/doc1.pdf`. The UUID in the middle is a deliberate choice: without it, two uploads of `report.pdf` by the same user would overwrite each other in MinIO silently. The `documentId` guarantees uniqueness while preserving the user-based directory structure and keeping the original filename readable at the end of the path.
