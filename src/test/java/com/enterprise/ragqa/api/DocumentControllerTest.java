package com.enterprise.ragqa.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.ragqa.api.dto.AskQuestionResponse;
import com.enterprise.ragqa.api.dto.CitationDto;
import com.enterprise.ragqa.api.dto.DocumentSummaryDto;
import com.enterprise.ragqa.api.dto.DocumentUploadResponse;
import com.enterprise.ragqa.api.dto.RefreshIndexResponse;
import com.enterprise.ragqa.document.service.DocumentIngestionService;
import com.enterprise.ragqa.document.service.IndexRefreshResult;
import com.enterprise.ragqa.document.service.RepositoryDocumentSyncService;
import com.enterprise.ragqa.qa.QuestionAnswerService;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService documentIngestionService;

    @MockBean
    private RepositoryDocumentSyncService repositoryDocumentSyncService;

    @MockBean
    private QuestionAnswerService questionAnswerService;

    @Test
    void listDocumentsReturnsDocumentArray() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentSummaryDto summary = new DocumentSummaryDto(
                docId, "report.pdf", "analyst-1", "application/pdf", 12, OffsetDateTime.now()
        );
        when(documentIngestionService.listDocuments()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/documents").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("report.pdf"))
                .andExpect(jsonPath("$[0].chunkCount").value(12));
    }

    @Test
    void listDocumentsReturnsEmptyArray() throws Exception {
        when(documentIngestionService.listDocuments()).thenReturn(List.of());

        mockMvc.perform(get("/api/documents").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void uploadDocumentReturnsUploadResponse() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentIngestionService.ingest(any()))
                .thenReturn(new DocumentUploadResponse(docId, "notes.txt", 5));

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "Sample content here.".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("notes.txt"))
                .andExpect(jsonPath("$.chunksIndexed").value(5));
    }

    @Test
    void uploadReturnsBadRequestForEmptyFile() throws Exception {
        when(documentIngestionService.ingest(any()))
                .thenThrow(new IllegalArgumentException("Uploaded file is empty."));

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askReturnsAnswerWithCitations() throws Exception {
        UUID queryId = UUID.randomUUID();
        CitationDto citation = new CitationDto(
                UUID.randomUUID(), "handbook.pdf", 0, 1, 1, 1, 2, "Benefits start after 30 days.", 0.95
        );
        AskQuestionResponse response = new AskQuestionResponse(queryId, "Benefits start after 30 days.", List.of(citation));
        when(questionAnswerService.answer(any())).thenReturn(response);

        String requestBody = """
                {
                    "question": "When do benefits start?",
                    "userId": "user-1",
                    "maxResults": 3
                }
                """;

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Benefits start after 30 days."))
                .andExpect(jsonPath("$.citations[0].documentName").value("handbook.pdf"))
                .andExpect(jsonPath("$.citations[0].score").value(0.95));
    }

    @Test
    void askReturnsBadRequestWhenQuestionIsBlank() throws Exception {
        String requestBody = """
                {
                    "question": "",
                    "userId": "user-1"
                }
                """;

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askReturnsBadRequestWhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshDocumentsReturnsIndexStats() throws Exception {
        IndexRefreshResult result = new IndexRefreshResult(3, 1, 0);
        when(repositoryDocumentSyncService.refreshRepositoryDocuments()).thenReturn(result);
        when(repositoryDocumentSyncService.repositoryPath()).thenReturn(Path.of("data/documents"));

        mockMvc.perform(post("/api/documents/refresh").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexedCount").value(3))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.removedCount").value(0));
    }
}
