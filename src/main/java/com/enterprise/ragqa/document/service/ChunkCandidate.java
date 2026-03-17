package com.enterprise.ragqa.document.service;

public record ChunkCandidate(
        int chunkIndex,
        String text,
        int pageStart,
        int pageEnd,
        int paragraphStart,
        int paragraphEnd
) {
}
