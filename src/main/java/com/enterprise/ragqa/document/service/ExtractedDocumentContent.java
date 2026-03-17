package com.enterprise.ragqa.document.service;

import java.util.List;

public record ExtractedDocumentContent(
        String text,
        List<ExtractedSegment> segments
) {
}
