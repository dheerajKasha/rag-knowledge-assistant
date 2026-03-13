package com.enterprise.ragqa.api;

import com.enterprise.ragqa.api.dto.AskQuestionRequest;
import com.enterprise.ragqa.api.dto.AskQuestionResponse;
import com.enterprise.ragqa.api.dto.DocumentUploadResponse;
import com.enterprise.ragqa.document.service.DocumentIngestionService;
import com.enterprise.ragqa.qa.QuestionAnswerService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final QuestionAnswerService questionAnswerService;

    public DocumentController(
            DocumentIngestionService documentIngestionService,
            QuestionAnswerService questionAnswerService
    ) {
        this.documentIngestionService = documentIngestionService;
        this.questionAnswerService = questionAnswerService;
    }

    @PostMapping(path = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "uploadedBy", defaultValue = "system") String uploadedBy
    ) throws IOException {
        return documentIngestionService.ingest(file, uploadedBy);
    }

    @PostMapping(path = "/qa/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AskQuestionResponse ask(@Valid @RequestBody AskQuestionRequest request) {
        return questionAnswerService.answer(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException exception) {
        return exception.getMessage();
    }
}
