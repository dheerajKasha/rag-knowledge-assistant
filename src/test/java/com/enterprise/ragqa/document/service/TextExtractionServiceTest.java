package com.enterprise.ragqa.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class TextExtractionServiceTest {

    private final TextExtractionService textExtractionService = new TextExtractionService();

    @Test
    void supportsKnownExtensions() {
        assertThat(textExtractionService.supports("document.pdf")).isTrue();
        assertThat(textExtractionService.supports("notes.docx")).isTrue();
        assertThat(textExtractionService.supports("readme.txt")).isTrue();
        assertThat(textExtractionService.supports("guide.md")).isTrue();
        assertThat(textExtractionService.supports("notes.markdown")).isTrue();
    }

    @Test
    void doesNotSupportUnknownExtensions() {
        assertThat(textExtractionService.supports("image.png")).isFalse();
        assertThat(textExtractionService.supports("data.xlsx")).isFalse();
        assertThat(textExtractionService.supports("archive.zip")).isFalse();
        assertThat(textExtractionService.supports(null)).isFalse();
        assertThat(textExtractionService.supports("noextension")).isFalse();
    }

    @Test
    void extractsTextFromPlainTextFile() throws IOException {
        String content = "First paragraph.\n\nSecond paragraph with more text.";
        MultipartFile file = mockMultipartFile("test.txt", "text/plain", content);

        ExtractedDocumentContent result = textExtractionService.extract(file);

        assertThat(result.text()).contains("First paragraph");
        assertThat(result.text()).contains("Second paragraph");
        assertThat(result.segments()).isNotEmpty();
    }

    @Test
    void extractsSegmentsFromMultipleParagraphs() throws IOException {
        String content = "Alpha paragraph here.\n\nBeta paragraph here.\n\nGamma paragraph here.";
        MultipartFile file = mockMultipartFile("content.txt", "text/plain", content);

        ExtractedDocumentContent result = textExtractionService.extract(file);

        assertThat(result.segments()).hasSize(3);
        assertThat(result.segments().get(0).text()).contains("Alpha");
        assertThat(result.segments().get(1).text()).contains("Beta");
        assertThat(result.segments().get(2).text()).contains("Gamma");
    }

    @Test
    void throwsForUnsupportedFileType() throws IOException {
        MultipartFile file = mockMultipartFile("image.png", "image/png", "fake binary content");

        assertThatThrownBy(() -> textExtractionService.extract(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void segmentsCarryCorrectPageAndParagraphNumbers() throws IOException {
        String content = "Introduction paragraph.\n\nMain body paragraph.";
        MultipartFile file = mockMultipartFile("notes.txt", "text/plain", content);

        ExtractedDocumentContent result = textExtractionService.extract(file);

        assertThat(result.segments()).allSatisfy(segment -> {
            assertThat(segment.pageNumber()).isGreaterThanOrEqualTo(1);
            assertThat(segment.paragraphNumber()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void extractsMarkdownContent() throws IOException {
        String content = "# Heading\n\nSome body text under the heading.\n\nAnother section.";
        MultipartFile file = mockMultipartFile("guide.md", "text/markdown", content);

        ExtractedDocumentContent result = textExtractionService.extract(file);

        assertThat(result.text()).isNotBlank();
        assertThat(result.segments()).isNotEmpty();
    }

    private MultipartFile mockMultipartFile(String filename, String contentType, String content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(contentType);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(bytes));
        return file;
    }
}
