package com.doclock.backend.controller;

import com.doclock.backend.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/embedding")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final EmbeddingService embeddingService;

    @PostMapping
    public Map<String, Object> generateEmbedding(
            @RequestBody Map<String, String> request) {

        String text = request.get("text");

        float[] embedding =
                embeddingService.generateEmbedding(text);

        Map<String, Object> response =
                new HashMap<>();

        response.put("dimensions", embedding.length);

        response.put("embedding", embedding);

        return response;
    }
}