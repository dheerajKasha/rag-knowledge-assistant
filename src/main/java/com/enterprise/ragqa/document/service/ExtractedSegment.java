package com.enterprise.ragqa.document.service;

public record ExtractedSegment(
        String text,
        int pageNumber,
        int paragraphNumber
) {
}
