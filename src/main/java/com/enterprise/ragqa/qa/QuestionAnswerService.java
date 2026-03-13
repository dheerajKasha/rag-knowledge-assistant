package com.enterprise.ragqa.qa;

import com.enterprise.ragqa.api.dto.AskQuestionRequest;
import com.enterprise.ragqa.api.dto.AskQuestionResponse;
import com.enterprise.ragqa.api.dto.CitationDto;
import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.QueryHistoryRecord;
import com.enterprise.ragqa.document.repository.QueryHistoryRepository;
import com.enterprise.ragqa.search.SearchResult;
import com.enterprise.ragqa.search.SemanticSearchService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionAnswerService {

    private final SemanticSearchService semanticSearchService;
    private final AnswerGenerator answerGenerator;
    private final QueryHistoryRepository queryHistoryRepository;

    public QuestionAnswerService(
            SemanticSearchService semanticSearchService,
            AnswerGenerator answerGenerator,
            QueryHistoryRepository queryHistoryRepository
    ) {
        this.semanticSearchService = semanticSearchService;
        this.answerGenerator = answerGenerator;
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @Transactional
    public AskQuestionResponse answer(AskQuestionRequest request) {
        List<SearchResult> searchResults = semanticSearchService.search(request.question(), request.resolvedMaxResults());
        List<CitationDto> citations = searchResults.stream()
                .map(result -> toCitation(result.chunk(), result.score()))
                .toList();
        String answer = appendCitationSummary(answerGenerator.generateAnswer(request.question(), searchResults), citations);

        QueryHistoryRecord query = queryHistoryRepository.save(new QueryHistoryRecord(
                UUID.randomUUID(),
                request.resolvedUserId(),
                request.question(),
                answer,
                OffsetDateTime.now()
        ));

        return new AskQuestionResponse(query.getId(), answer, citations);
    }

    private CitationDto toCitation(DocumentChunkRecord chunk, double score) {
        return new CitationDto(
                chunk.getDocument().getId(),
                chunk.getDocument().getFilename(),
                chunk.getChunkIndex(),
                abbreviate(chunk.getChunkText(), 240),
                score
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String appendCitationSummary(String answer, List<CitationDto> citations) {
        if (citations.isEmpty()) {
            return answer;
        }

        String references = citations.stream()
                .limit(3)
                .map(citation -> "[" + citation.documentName() + " chunk " + citation.chunkIndex() + "]")
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");

        return answer + " Sources: " + references;
    }
}
