package com.enterprise.ragqa.document.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChunkingService {

    private static final int MAX_CHUNK_SIZE = 900;
    private static final int OVERLAP_SIZE = 150;

    public List<String> chunk(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int cursor = 0;
        while (cursor < normalized.length()) {
            int targetEnd = Math.min(cursor + MAX_CHUNK_SIZE, normalized.length());
            int splitPoint = findSplitPoint(normalized, cursor, targetEnd);
            String chunk = normalized.substring(cursor, splitPoint).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (splitPoint >= normalized.length()) {
                break;
            }
            cursor = Math.max(splitPoint - OVERLAP_SIZE, cursor + 1);
        }
        return chunks;
    }

    private int findSplitPoint(String text, int start, int targetEnd) {
        for (int i = targetEnd; i > start; i--) {
            char current = text.charAt(i - 1);
            if (current == '\n' || current == '.' || current == '!' || current == '?') {
                return i;
            }
        }
        return targetEnd;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replaceAll("[\\t\\x0B\\f]+", " ").trim();
    }
}
