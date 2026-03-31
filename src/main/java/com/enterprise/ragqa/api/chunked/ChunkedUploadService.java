package com.enterprise.ragqa.api.chunked;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Manages in-memory chunked upload sessions.
 * Sessions are created, populated chunk-by-chunk, then finalized (assembled and removed).
 */
@Service
public class ChunkedUploadService {

    /** Maximum size in bytes for any single chunk: 10 MB. */
    public static final int MAX_CHUNK_BYTES = 10 * 1024 * 1024;

    /** Maximum number of concurrent in-flight sessions. */
    private static final int MAX_SESSIONS = 50;

    private final Map<String, ChunkedUploadSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new upload session and returns its ID.
     *
     * @param filename    original filename
     * @param totalChunks number of chunks the client will send
     * @return the newly created session ID
     */
    public String createSession(String filename, int totalChunks) {
        if (totalChunks < 1 || totalChunks > 1000) {
            throw new IllegalArgumentException("totalChunks must be between 1 and 1000.");
        }
        if (sessions.size() >= MAX_SESSIONS) {
            throw new IllegalStateException("Too many concurrent upload sessions. Please try again later.");
        }
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new ChunkedUploadSession(sessionId, filename, totalChunks));
        return sessionId;
    }

    /**
     * Stores a chunk for the given session.
     *
     * @param sessionId  the session ID returned by {@link #createSession}
     * @param chunkIndex zero-based index of this chunk
     * @param data       raw bytes of this chunk
     * @return the updated session
     */
    public ChunkedUploadSession addChunk(String sessionId, int chunkIndex, byte[] data) {
        ChunkedUploadSession session = getSessionOrThrow(sessionId);
        if (data.length > MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException(
                    "Chunk size " + data.length + " bytes exceeds the maximum of " + MAX_CHUNK_BYTES + " bytes.");
        }
        session.storeChunk(chunkIndex, data);
        return session;
    }

    /**
     * Returns the session if all chunks have been received, otherwise throws.
     * The session is removed from memory after this call.
     */
    public ChunkedUploadSession finalizeSession(String sessionId) {
        ChunkedUploadSession session = getSessionOrThrow(sessionId);
        if (!session.isComplete()) {
            throw new IllegalStateException(
                    "Upload is not complete: received " + session.receivedChunks()
                    + " of " + session.getTotalChunks() + " chunks.");
        }
        sessions.remove(sessionId);
        return session;
    }

    private ChunkedUploadSession getSessionOrThrow(String sessionId) {
        ChunkedUploadSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Upload session not found: " + sessionId);
        }
        return session;
    }
}
