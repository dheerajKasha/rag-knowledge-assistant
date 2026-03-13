package com.enterprise.ragqa.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "query_history")
public class QueryHistoryRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 4000)
    private String answer;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected QueryHistoryRecord() {
    }

    public QueryHistoryRecord(UUID id, String userId, String question, String answer, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
