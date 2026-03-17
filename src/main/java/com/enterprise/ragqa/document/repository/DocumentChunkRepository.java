package com.enterprise.ragqa.document.repository;

import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkRecord, UUID> {

    long countByDocument_Id(UUID documentId);

    void deleteByDocument_Id(UUID documentId);

    List<DocumentChunkRecord> findByDocument_Id(UUID documentId);

    @Query("""
            select chunk
            from DocumentChunkRecord chunk
            order by cosine_distance(chunk.embedding, :queryEmbedding)
            """)
    List<DocumentChunkRecord> findNearestNeighbors(
            @Param("queryEmbedding") float[] queryEmbedding,
            Pageable pageable
    );
}
