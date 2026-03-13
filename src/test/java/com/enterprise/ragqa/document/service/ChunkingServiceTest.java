package com.enterprise.ragqa.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void createsMultipleChunksForLongDocuments() {
        String paragraph = "Spring Boot powers document ingestion and semantic retrieval. ";
        String text = paragraph.repeat(80);

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(900));
    }

    @Test
    void returnsEmptyListWhenInputHasNoText() {
        assertThat(chunkingService.chunk("   \n\t")).isEmpty();
    }
}
