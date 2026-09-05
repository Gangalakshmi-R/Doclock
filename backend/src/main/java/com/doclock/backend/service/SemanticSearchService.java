package com.doclock.backend.service;

import com.doclock.backend.entity.DocumentChunk;
import com.doclock.backend.entity.DocumentStatus;
import com.doclock.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Hybrid RAG: dense semantic similarity plus lexical matching for exact terms. */
@Service
@RequiredArgsConstructor
public class SemanticSearchService {
    private static final double MIN_RELEVANCE = 0.18;

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    @Transactional
    public List<Map<String, Object>> search(String question, int limit) {
        if (question == null || question.isBlank() || limit < 1) return List.of();

        float[] queryEmbedding = embeddingService.generateEmbedding(question);
        Set<String> queryTerms = terms(question);
        List<SearchResult> ranked = new ArrayList<>();

        for (DocumentChunk chunk : documentChunkRepository.findAll()) {
            if (chunk.getDocument().getStatus() != DocumentStatus.PROCESSED) continue;
            float[] chunkEmbedding = deserialize(chunk.getEmbedding());
            // Existing documents from before embedding-on-chunk storage are
            // upgraded lazily on their first question, without re-uploading.
            if (chunkEmbedding.length == 0) {
                chunkEmbedding = embeddingService.generateEmbedding(chunk.getContent());
                chunk.setEmbedding(serialize(chunkEmbedding));
            }
            double semantic = cosineSimilarity(queryEmbedding, chunkEmbedding);
            double lexical = lexicalScore(queryTerms, terms(chunk.getContent()));
            // Dense vectors handle paraphrases; lexical matching preserves names and dates.
            double relevance = (semantic * 0.80) + (lexical * 0.20);
            if (relevance >= MIN_RELEVANCE || lexical > 0) {
                ranked.add(new SearchResult(chunk, semantic, lexical, relevance));
            }
        }

        return ranked.stream()
                .sorted(Comparator.comparingDouble(SearchResult::relevance).reversed())
                .limit(Math.min(limit, 8))
                .map(result -> Map.<String, Object>of(
                        "documentId", result.chunk().getDocument().getId(),
                        "documentName", result.chunk().getDocument().getFileName(),
                        "chunkNumber", result.chunk().getChunkNumber(),
                        "content", result.chunk().getContent(),
                        "semanticScore", round(result.semantic()),
                        "keywordScore", round(result.lexical()),
                        "relevance", round(result.relevance())
                ))
                .toList();
    }

    private float[] deserialize(String storedEmbedding) {
        if (storedEmbedding == null || storedEmbedding.isBlank()) return new float[0];
        String[] values = storedEmbedding.split(",");
        float[] embedding = new float[values.length];
        try {
            for (int index = 0; index < values.length; index++) embedding[index] = Float.parseFloat(values[index]);
            return embedding;
        } catch (NumberFormatException exception) {
            return new float[0];
        }
    }

    private String serialize(float[] embedding) {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) value.append(',');
            value.append(embedding[index]);
        }
        return value.toString();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length == 0 || left.length != right.length) return 0;
        double dot = 0, leftMagnitude = 0, rightMagnitude = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftMagnitude += left[index] * left[index];
            rightMagnitude += right[index] * right[index];
        }
        if (leftMagnitude == 0 || rightMagnitude == 0) return 0;
        return Math.max(0, dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude)));
    }

    private Set<String> terms(String value) {
        return java.util.Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
    }

    private double lexicalScore(Set<String> queryTerms, Set<String> contentTerms) {
        if (queryTerms.isEmpty()) return 0;
        long matches = queryTerms.stream().filter(contentTerms::contains).count();
        return (double) matches / queryTerms.size();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record SearchResult(DocumentChunk chunk, double semantic, double lexical, double relevance) { }
}
