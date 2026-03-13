package com.enterprise.ragqa.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false, length = 32000)
    private String extractedText;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected DocumentRecord() {
    }

    public DocumentRecord(UUID id, String filename, String contentType, String uploadedBy, String extractedText, OffsetDateTime createdAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.uploadedBy = uploadedBy;
        this.extractedText = extractedText;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
