package com.enterprise.ragqa.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.ragqa.config.DocumentRepositoryProperties;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryDocumentSyncServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentIngestionService documentIngestionService;

    @Mock
    private DocumentHashingService documentHashingService;

    @Mock
    private TextExtractionService textExtractionService;

    @TempDir
    Path tempDir;

    @Test
    void refreshIndexesNewAndChangedDocumentsAndRemovesMissingOnes() throws Exception {
        Path repoPath = tempDir.resolve("documents");
        Files.createDirectories(repoPath);
        Path newDoc = Files.writeString(repoPath.resolve("new-policy.md"), "new policy");
        Path changedDoc = Files.writeString(repoPath.resolve("changed-policy.txt"), "changed policy");

        DocumentRecord changedRecord = new DocumentRecord(
                UUID.randomUUID(),
                "changed-policy.txt",
                "text/plain",
                "repository-scanner",
                "changed-policy.txt",
                DocumentSourceType.REPOSITORY,
                "old-hash",
                "old content",
                OffsetDateTime.now()
        );
        DocumentRecord removedRecord = new DocumentRecord(
                UUID.randomUUID(),
                "removed.txt",
                "text/plain",
                "repository-scanner",
                "removed.txt",
                DocumentSourceType.REPOSITORY,
                "removed-hash",
                "removed content",
                OffsetDateTime.now()
        );

        when(documentRepository.findBySourceType(DocumentSourceType.REPOSITORY)).thenReturn(List.of(changedRecord, removedRecord));
        when(textExtractionService.supports("new-policy.md")).thenReturn(true);
        when(textExtractionService.supports("changed-policy.txt")).thenReturn(true);
        when(textExtractionService.extract(newDoc)).thenReturn(new ExtractedDocumentContent("new policy", List.of(new ExtractedSegment("new policy", 1, 1))));
        when(textExtractionService.extract(changedDoc)).thenReturn(new ExtractedDocumentContent("changed policy", List.of(new ExtractedSegment("changed policy", 1, 1))));
        when(documentHashingService.sha256("new policy")).thenReturn("new-hash");
        when(documentHashingService.sha256("changed policy")).thenReturn("new-changed-hash");
        when(documentRepository.findBySourceTypeAndSourcePath(DocumentSourceType.REPOSITORY, "new-policy.md")).thenReturn(Optional.empty());
        when(documentRepository.findBySourceTypeAndSourcePath(DocumentSourceType.REPOSITORY, "changed-policy.txt")).thenReturn(Optional.of(changedRecord));

        RepositoryDocumentSyncService service = new RepositoryDocumentSyncService(
                new DocumentRepositoryProperties(repoPath.toString(), true),
                documentRepository,
                documentIngestionService,
                documentHashingService,
                textExtractionService
        );

        IndexRefreshResult result = service.refreshRepositoryDocuments();

        assertThat(result.indexedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.removedCount()).isEqualTo(1);
        verify(documentIngestionService).reindexRepositoryDocument(eq(newDoc), eq("new-policy.md"), eq("new-hash"), eq(false));
        verify(documentIngestionService).reindexRepositoryDocument(eq(changedDoc), eq("changed-policy.txt"), eq("new-changed-hash"), eq(true));
        verify(documentIngestionService).removeDocument(removedRecord.getId());
    }

    @Test
    void refreshSkipsUnchangedRepositoryDocuments() throws Exception {
        Path repoPath = tempDir.resolve("documents");
        Files.createDirectories(repoPath);
        Path unchangedDoc = Files.writeString(repoPath.resolve("unchanged.md"), "same content");

        DocumentRecord existing = new DocumentRecord(
                UUID.randomUUID(),
                "unchanged.md",
                "text/markdown",
                "repository-scanner",
                "unchanged.md",
                DocumentSourceType.REPOSITORY,
                "same-hash",
                "same content",
                OffsetDateTime.now()
        );

        when(documentRepository.findBySourceType(DocumentSourceType.REPOSITORY)).thenReturn(List.of(existing));
        when(textExtractionService.supports("unchanged.md")).thenReturn(true);
        when(textExtractionService.extract(unchangedDoc)).thenReturn(new ExtractedDocumentContent("same content", List.of(new ExtractedSegment("same content", 1, 1))));
        when(documentHashingService.sha256("same content")).thenReturn("same-hash");
        when(documentRepository.findBySourceTypeAndSourcePath(DocumentSourceType.REPOSITORY, "unchanged.md")).thenReturn(Optional.of(existing));

        RepositoryDocumentSyncService service = new RepositoryDocumentSyncService(
                new DocumentRepositoryProperties(repoPath.toString(), true),
                documentRepository,
                documentIngestionService,
                documentHashingService,
                textExtractionService
        );

        IndexRefreshResult result = service.refreshRepositoryDocuments();

        assertThat(result.indexedCount()).isZero();
        assertThat(result.updatedCount()).isZero();
        assertThat(result.removedCount()).isZero();
        verify(documentIngestionService, never()).reindexRepositoryDocument(any(), any(), any(), anyBoolean());
        verify(documentIngestionService, never()).removeDocument(any());
    }
}
