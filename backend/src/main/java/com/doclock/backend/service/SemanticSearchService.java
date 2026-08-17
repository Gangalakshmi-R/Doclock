package com.doclock.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;

    private final JdbcTemplate jdbcTemplate;


    public List<Map<String, Object>> search(
            String question,
            int limit) {

        // -------------------------------------------------
        // 1. Convert question into embedding
        // -------------------------------------------------

        float[] embedding =
                embeddingService.generateEmbedding(
                        question
                );


        // -------------------------------------------------
        // 2. Convert embedding to pgvector format
        // -------------------------------------------------

        String vector =
                convertToVectorString(embedding);


        // -------------------------------------------------
        // 3. Semantic similarity search
        // -------------------------------------------------

       String sql = """
        SELECT
            id,
            document_id,
            chunk_number,
            content,
            1 - (embedding <=> ?::vector)
                AS similarity
        FROM document_embeddings
        WHERE 1 - (embedding <=> ?::vector) >= ?
        ORDER BY embedding <=> ?::vector
        LIMIT ?
        """;

       return jdbcTemplate.queryForList(
        sql,
        vector,
        vector,
        0.30,
        vector,
        limit
);
    }


    private String convertToVectorString(
            float[] embedding) {

        StringBuilder builder =
                new StringBuilder("[");


        for (int i = 0;
             i < embedding.length;
             i++) {

            if (i > 0) {
                builder.append(",");
            }

            builder.append(embedding[i]);
        }


        builder.append("]");

        return builder.toString();
    }
}