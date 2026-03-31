package com.enterprise.ragqa.api.chunked;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * Wraps an assembled byte array as a {@link MultipartFile} so it can be passed
 * directly to {@link com.enterprise.ragqa.document.service.DocumentIngestionService#ingest}.
 */
public class AssembledMultipartFile implements MultipartFile {

    private final String filename;
    private final byte[] content;

    public AssembledMultipartFile(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return null; // resolved from filename extension inside DocumentIngestionService
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        throw new UnsupportedOperationException("transferTo is not supported for assembled uploads.");
    }
}
