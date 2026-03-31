package com.enterprise.ragqa.api.chunked;

import com.enterprise.ragqa.api.dto.DocumentUploadResponse;
import com.enterprise.ragqa.document.service.DocumentIngestionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Supports chunked file uploads for documents larger than the standard 25 MB limit.
 *
 * <h3>Protocol</h3>
 * <ol>
 *   <li>POST /api/documents/upload/session — initiate; returns {@code sessionId} and {@code maxChunkBytes}.</li>
 *   <li>POST /api/documents/upload/session/{sessionId}/chunk?chunkIndex=N — repeat for each chunk.</li>
 *   <li>POST /api/documents/upload/session/{sessionId}/finalize — assemble and ingest.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/documents/upload/session")
public class ChunkedUploadController {

    private final ChunkedUploadService chunkedUploadService;
    private final DocumentIngestionService documentIngestionService;

    public ChunkedUploadController(
            ChunkedUploadService chunkedUploadService,
            DocumentIngestionService documentIngestionService
    ) {
        this.chunkedUploadService = chunkedUploadService;
        this.documentIngestionService = documentIngestionService;
    }

    /**
     * Initiates a chunked upload session.
     *
     * @param filename    original file name (required)
     * @param totalChunks total number of chunks the client will send (required)
     * @return {@code {"sessionId": "...", "maxChunkBytes": 10485760}}
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> initiateSession(
            @RequestParam String filename,
            @RequestParam int totalChunks
    ) {
        String sessionId = chunkedUploadService.createSession(filename, totalChunks);
        return Map.of(
                "sessionId", sessionId,
                "maxChunkBytes", ChunkedUploadService.MAX_CHUNK_BYTES
        );
    }

    /**
     * Uploads one chunk. Send chunks in any order; the server assembles them by index.
     *
     * @param sessionId  the session ID from {@link #initiateSession}
     * @param chunkIndex zero-based index of this chunk
     * @param data       the raw chunk bytes as a multipart file part named {@code data}
     * @return {@code {"received": N, "total": M}}
     */
    @PostMapping(path = "/{sessionId}/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> uploadChunk(
            @PathVariable String sessionId,
            @RequestParam int chunkIndex,
            @RequestPart("data") MultipartFile data
    ) throws IOException {
        byte[] bytes = data.getBytes();
        ChunkedUploadSession session = chunkedUploadService.addChunk(sessionId, chunkIndex, bytes);
        return Map.of(
                "received", session.receivedChunks(),
                "total", session.getTotalChunks()
        );
    }

    /**
     * Finalizes the session: assembles all chunks and ingests the document.
     *
     * @param sessionId the session ID
     * @return standard {@link DocumentUploadResponse}
     */
    @PostMapping(path = "/{sessionId}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentUploadResponse finalizeUpload(
            @PathVariable String sessionId
    ) throws IOException {
        ChunkedUploadSession session = chunkedUploadService.finalizeSession(sessionId);
        byte[] assembled = session.assemble();
        AssembledMultipartFile file = new AssembledMultipartFile(session.getFilename(), assembled);
        return documentIngestionService.ingest(file);
    }
}
