package com.enterprise.ragqa.document.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChunkingService {

    private static final int MAX_CHUNK_SIZE = 900;

    public List<ChunkCandidate> chunk(ExtractedDocumentContent content) {
        if (content == null || content.segments().isEmpty()) {
            return List.of();
        }

        List<ChunkCandidate> chunks = new ArrayList<>();
        List<ExtractedSegment> currentSegments = new ArrayList<>();
        int chunkIndex = 0;
        int currentSize = 0;

        for (ExtractedSegment segment : content.segments()) {
            String normalizedText = normalize(segment.text());
            if (normalizedText.isBlank()) {
                continue;
            }

            for (String piece : splitLargeSegment(normalizedText)) {
                ExtractedSegment normalizedSegment = new ExtractedSegment(
                        piece,
                        Math.max(segment.pageNumber(), 1),
                        Math.max(segment.paragraphNumber(), 1)
                );

                if (!currentSegments.isEmpty() && currentSize + piece.length() > MAX_CHUNK_SIZE) {
                    chunks.add(toChunk(chunkIndex++, currentSegments));
                    currentSegments = new ArrayList<>();
                    currentSize = 0;
                }

                currentSegments.add(normalizedSegment);
                currentSize += piece.length();
            }
        }

        if (!currentSegments.isEmpty()) {
            chunks.add(toChunk(chunkIndex, currentSegments));
        }

        return chunks;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ExtractedDocumentContent content = new ExtractedDocumentContent(
                normalize(text),
                List.of(new ExtractedSegment(text, 1, 1))
        );
        return chunk(content).stream().map(ChunkCandidate::text).toList();
    }

    private ChunkCandidate toChunk(int chunkIndex, List<ExtractedSegment> segments) {
        ExtractedSegment first = segments.getFirst();
        ExtractedSegment last = segments.getLast();
        String text = segments.stream()
                .map(ExtractedSegment::text)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        return new ChunkCandidate(
                chunkIndex,
                text,
                first.pageNumber(),
                last.pageNumber(),
                first.paragraphNumber(),
                last.paragraphNumber()
        );
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replaceAll("[\\t\\x0B\\f]+", " ").trim();
    }

    private List<String> splitLargeSegment(String text) {
        if (text.length() <= MAX_CHUNK_SIZE) {
            return List.of(text);
        }

        List<String> pieces = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            int end = Math.min(cursor + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int splitPoint = text.lastIndexOf(". ", end);
                if (splitPoint > cursor + (MAX_CHUNK_SIZE / 2)) {
                    end = splitPoint + 1;
                }
            }
            pieces.add(text.substring(cursor, end).trim());
            cursor = end;
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
        }
        return pieces;
    }
}
