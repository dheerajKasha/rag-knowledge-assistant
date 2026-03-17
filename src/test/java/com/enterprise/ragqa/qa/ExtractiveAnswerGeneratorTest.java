package com.enterprise.ragqa.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.service.DocumentSourceType;
import com.enterprise.ragqa.search.SearchResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExtractiveAnswerGeneratorTest {

    private final ExtractiveAnswerGenerator answerGenerator = new ExtractiveAnswerGenerator();

    @Test
    void generatesExtractiveAnswerFromTopContext() {
        DocumentRecord document = new DocumentRecord(
                UUID.randomUUID(),
                "policy.txt",
                "text/plain",
                "tester",
                null,
                DocumentSourceType.UPLOAD,
                "hash-1",
                "Remote work is allowed two days per week.",
                OffsetDateTime.now()
        );
        DocumentChunkRecord chunk = new DocumentChunkRecord(
                UUID.randomUUID(),
                document,
                1,
                "Remote work is allowed two days per week.",
                new float[]{0.1f, 0.2f},
                1,
                1,
                1,
                1,
                OffsetDateTime.now()
        );

        String answer = answerGenerator.generateAnswer("What is the remote work policy?", List.of(new SearchResult(chunk, 0.88)));

        assertThat(answer).contains("Answer based on indexed documents:");
        assertThat(answer).contains("Remote work is allowed two days per week.");
    }

    @Test
    void returnsFallbackWhenNoContextExists() {
        String answer = answerGenerator.generateAnswer("Unknown?", List.of());

        assertThat(answer).contains("could not find relevant information");
    }
}
