package com.enterprise.ragqa.document.repository;

import com.enterprise.ragqa.document.model.DocumentRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentRecord, UUID> {
}
