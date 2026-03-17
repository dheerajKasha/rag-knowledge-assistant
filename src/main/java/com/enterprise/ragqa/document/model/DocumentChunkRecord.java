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
    private int pageStart;

    @Column(nullable = false)
    private int pageEnd;

    @Column(nullable = false)
    private int paragraphStart;

    @Column(nullable = false)
    private int paragraphEnd;

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
            int pageStart,
            int pageEnd,
            int paragraphStart,
            int paragraphEnd,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.embeddingJson = embeddingJson;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.paragraphStart = paragraphStart;
        this.paragraphEnd = paragraphEnd;
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

    public int getPageStart() {
        return pageStart;
    }

    public int getPageEnd() {
        return pageEnd;
    }

    public int getParagraphStart() {
        return paragraphStart;
    }

    public int getParagraphEnd() {
        return paragraphEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
