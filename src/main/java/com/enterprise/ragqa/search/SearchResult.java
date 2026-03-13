package com.enterprise.ragqa.search;

import com.enterprise.ragqa.document.model.DocumentChunkRecord;

public record SearchResult(
        DocumentChunkRecord chunk,
        double score
) {
}
