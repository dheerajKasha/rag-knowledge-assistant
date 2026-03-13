package com.enterprise.ragqa.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunkRecord {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id")
    private DocumentRecord document;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false, length = 4000)
    private String chunkText;

    @Column(nullable = false, length = 12000)
    private String embeddingJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected DocumentChunkRecord() {
    }

    public DocumentChunkRecord(
            UUID id,
            DocumentRecord document,
            int chunkIndex,
            String chunkText,
            String embeddingJson,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.embeddingJson = embeddingJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public DocumentRecord getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
