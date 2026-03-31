package com.enterprise.ragqa.api.chunked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state for one chunked upload. Each chunk is stored as a byte array
 * keyed by its zero-based index. The session is discarded after finalization or
 * on error.
 */
public class ChunkedUploadSession {

    private final String sessionId;
    private final String filename;
    private final int totalChunks;
    private final Map<Integer, byte[]> chunks = new ConcurrentHashMap<>();

    public ChunkedUploadSession(String sessionId, String filename, int totalChunks) {
        this.sessionId = sessionId;
        this.filename = filename;
        this.totalChunks = totalChunks;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getFilename() {
        return filename;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void storeChunk(int index, byte[] data) {
        if (index < 0 || index >= totalChunks) {
            throw new IllegalArgumentException(
                    "Chunk index " + index + " is out of range for session with " + totalChunks + " total chunks.");
        }
        chunks.put(index, data);
    }

    public boolean isComplete() {
        return chunks.size() == totalChunks;
    }

    public int receivedChunks() {
        return chunks.size();
    }

    /**
     * Assembles received chunks in order into a single byte array.
     * Throws if the session is not yet complete.
     */
    public byte[] assemble() {
        if (!isComplete()) {
            throw new IllegalStateException(
                    "Cannot assemble: received " + chunks.size() + " of " + totalChunks + " chunks.");
        }

        int totalLength = 0;
        for (int i = 0; i < totalChunks; i++) {
            totalLength += chunks.get(i).length;
        }

        byte[] assembled = new byte[totalLength];
        int offset = 0;
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunk = chunks.get(i);
            System.arraycopy(chunk, 0, assembled, offset, chunk.length);
            offset += chunk.length;
        }
        return assembled;
    }
}
