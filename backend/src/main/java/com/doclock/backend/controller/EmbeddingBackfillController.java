package com.doclock.backend.controller;

import com.doclock.backend.service.EmbeddingBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test/embeddings")
@RequiredArgsConstructor
public class EmbeddingBackfillController {

    private final EmbeddingBackfillService embeddingBackfillService;

    @GetMapping("/hello")
    public String hello() {
        return "Embedding controller is working";
    }

    @PostMapping("/generate/{documentId}")
    public Map<String, Object> generateEmbeddings(
            @PathVariable Long documentId) {

        int count =
                embeddingBackfillService
                        .generateEmbeddings(documentId);

        return Map.of(
                "documentId", documentId,
                "embeddingsGenerated", count,
                "message", "Embeddings generated successfully"
        );
    }
}