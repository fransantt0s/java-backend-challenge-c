-- Document Management Service - Database Schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS documents (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id       VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    minio_path    VARCHAR(500) NOT NULL,
    file_size     BIGINT       NOT NULL,
    file_type     VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_documents PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS document_tags (
    document_id UUID         NOT NULL,
    tag         VARCHAR(255) NOT NULL,

    CONSTRAINT pk_document_tags PRIMARY KEY (document_id, tag),
    CONSTRAINT fk_document_tags_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);

-- Indices for common query patterns
CREATE INDEX IF NOT EXISTS idx_documents_user_id      ON documents (user_id);
CREATE INDEX IF NOT EXISTS idx_documents_document_name ON documents (document_name);
CREATE INDEX IF NOT EXISTS idx_documents_created_at   ON documents (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_document_tags_tag       ON document_tags (tag);
CREATE INDEX IF NOT EXISTS idx_document_tags_doc_id    ON document_tags (document_id);
