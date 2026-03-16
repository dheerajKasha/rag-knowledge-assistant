package com.enterprise.ragqa.document.repository;

import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkRecord, UUID> {

    long countByDocument_Id(UUID documentId);

    List<DocumentChunkRecord> findByDocument_Id(UUID documentId);
}
