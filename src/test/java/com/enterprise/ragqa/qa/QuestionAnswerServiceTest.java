package com.enterprise.ragqa.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.ragqa.api.dto.AskQuestionRequest;
import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.model.QueryHistoryRecord;
import com.enterprise.ragqa.document.repository.QueryHistoryRepository;
import com.enterprise.ragqa.document.service.DocumentSourceType;
import com.enterprise.ragqa.search.SearchResult;
import com.enterprise.ragqa.search.SemanticSearchService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerServiceTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private AnswerGenerator answerGenerator;

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @InjectMocks
    private QuestionAnswerService questionAnswerService;

    @Test
    void answersQuestionAndReturnsCitations() {
        DocumentRecord document = new DocumentRecord(
                UUID.randomUUID(),
                "employee-handbook.pdf",
                "application/pdf",
                "tester",
                null,
                DocumentSourceType.UPLOAD,
                "hash-1",
                "Benefits are available after 30 days.",
                OffsetDateTime.now()
        );
        DocumentChunkRecord chunk = new DocumentChunkRecord(
                UUID.randomUUID(),
                document,
                0,
                "Benefits are available after 30 days of employment.",
                new float[]{0.1f, 0.2f},
                12,
                12,
                4,
                4,
                OffsetDateTime.now()
        );
        List<SearchResult> searchResults = List.of(new SearchResult(chunk, 0.91));

        when(semanticSearchService.search("When do benefits start?", 5)).thenReturn(searchResults);
        when(answerGenerator.generateAnswer("When do benefits start?", searchResults))
                .thenReturn("Benefits start after 30 days of employment.");
        when(queryHistoryRepository.save(any(QueryHistoryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = questionAnswerService.answer(new AskQuestionRequest("When do benefits start?", "user-1", 5));

        assertThat(response.answer()).contains("30 days");
        assertThat(response.answer()).contains("employee-handbook.pdf");
        assertThat(response.answer()).contains("p. 12");
        assertThat(response.answer()).contains("para. 4");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().get(0).documentName()).isEqualTo("employee-handbook.pdf");
        assertThat(response.citations().get(0).pageStart()).isEqualTo(12);
        verify(queryHistoryRepository).save(any(QueryHistoryRecord.class));
    }
}
