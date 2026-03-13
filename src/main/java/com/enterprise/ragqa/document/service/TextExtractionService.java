package com.enterprise.ragqa.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractionService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md", "markdown");

    private final Tika tika = new Tika();

    public String extract(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String extension = filename == null || !filename.contains(".")
                ? ""
                : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }

        try (InputStream inputStream = file.getInputStream()) {
            String extracted = tika.parseToString(inputStream);
            return extracted == null ? "" : extracted.strip();
        } catch (Exception exception) {
            throw new IOException("Failed to extract text from file " + filename, exception);
        }
    }
}
