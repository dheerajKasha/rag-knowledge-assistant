package com.enterprise.ragqa.document.repository;

import com.enterprise.ragqa.document.model.DocumentRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentRecord, UUID> {

    List<DocumentRecord> findBySourceType(String sourceType);

    Optional<DocumentRecord> findBySourceTypeAndSourcePath(String sourceType, String sourcePath);

    Optional<DocumentRecord> findByContentHash(String contentHash);
}
