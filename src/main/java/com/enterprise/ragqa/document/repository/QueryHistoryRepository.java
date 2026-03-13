package com.enterprise.ragqa.document.repository;

import com.enterprise.ragqa.document.model.QueryHistoryRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryHistoryRepository extends JpaRepository<QueryHistoryRecord, UUID> {
}
