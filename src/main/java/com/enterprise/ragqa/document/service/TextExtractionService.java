package com.enterprise.ragqa.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractionService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md", "markdown");

    private final Tika tika = new Tika();

    public boolean supports(String filename) {
        String extension = filename == null || !filename.contains(".")
                ? ""
                : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    public ExtractedDocumentContent extract(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            return extract(filename, inputStream);
        }
    }

    public ExtractedDocumentContent extract(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return extract(path.getFileName().toString(), inputStream);
        }
    }

    private ExtractedDocumentContent extract(String filename, InputStream inputStream) throws IOException {
        String extension = filename == null || !filename.contains(".")
                ? ""
                : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!supports(filename)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }

        try {
            return switch (extension) {
                case "pdf" -> extractPdf(inputStream);
                default -> extractStructuredText(tika.parseToString(inputStream));
            };
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Failed to extract text from file " + filename, exception);
        }
    }

    private ExtractedDocumentContent extractPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedSegment> segments = new ArrayList<>();
            int paragraphNumber = 1;

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                List<ExtractedSegment> pageSegments = toSegments(pageText, page, paragraphNumber);
                segments.addAll(pageSegments);
                paragraphNumber += pageSegments.size();
            }

            return new ExtractedDocumentContent(joinSegments(segments), segments);
        }
    }

    private ExtractedDocumentContent extractStructuredText(String text) {
        List<ExtractedSegment> segments = toSegments(text, 1, 1);
        return new ExtractedDocumentContent(joinSegments(segments), segments);
    }

    private List<ExtractedSegment> toSegments(String text, int pageNumber, int paragraphStart) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").strip();
        if (normalized.isBlank()) {
            return List.of();
        }

        String[] paragraphs = normalized.split("\\n\\s*\\n");
        List<ExtractedSegment> segments = new ArrayList<>();
        int paragraphNumber = paragraphStart;
        for (String paragraph : paragraphs) {
            String cleaned = paragraph.replaceAll("\\s+", " ").trim();
            if (!cleaned.isBlank()) {
                segments.add(new ExtractedSegment(cleaned, pageNumber, paragraphNumber++));
            }
        }

        if (segments.isEmpty()) {
            segments.add(new ExtractedSegment(normalized.replaceAll("\\s+", " ").trim(), pageNumber, paragraphStart));
        }

        return segments;
    }

    private String joinSegments(List<ExtractedSegment> segments) {
        return segments.stream()
                .map(ExtractedSegment::text)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
