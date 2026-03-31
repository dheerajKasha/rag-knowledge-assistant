package com.enterprise.ragqa.api;

import com.enterprise.ragqa.api.dto.ErrorResponse;
import com.enterprise.ragqa.api.exception.DocumentParseException;
import com.enterprise.ragqa.api.exception.EmbeddingException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return new ErrorResponse(400, "Bad Request", message, request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return new ErrorResponse(
                413,
                "Payload Too Large",
                "File size exceeds the 25 MB limit. Use the chunked upload API (/api/documents/upload/session) for larger files.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DocumentParseException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleDocumentParse(DocumentParseException ex, HttpServletRequest request) {
        log.warn("Document parse failure at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(422, "Unprocessable Entity", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(EmbeddingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleEmbedding(EmbeddingException ex, HttpServletRequest request) {
        log.error("Embedding service failure at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return new ErrorResponse(
                503,
                "Service Unavailable",
                "The embedding service is temporarily unavailable. Please try again later.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleIo(IOException ex, HttpServletRequest request) {
        log.warn("IO failure at {}: {}", request.getRequestURI(), ex.getMessage());
        return new ErrorResponse(
                422,
                "Unprocessable Entity",
                "Failed to read or process the uploaded file: " + ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again.",
                request.getRequestURI()
        );
    }
}
