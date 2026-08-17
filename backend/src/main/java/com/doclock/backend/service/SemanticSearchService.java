package com.doclock.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;

    private final JdbcTemplate jdbcTemplate;


    // =========================================================
    // HYBRID SEARCH
    // =========================================================

    public List<Map<String, Object>> search(
            String question,
            int limit) {

        if (question == null || question.isBlank()) {
            return List.of();
        }


        /*
         * We retrieve more candidates than the final limit.
         *
         * Example:
         *
         * limit = 3
         *
         * Semantic candidates = 6
         * Keyword candidates  = 6
         *
         * Then merge and return the best results.
         */

        int candidateLimit =
                Math.max(limit * 2, 6);


        // =====================================================
        // 1. SEMANTIC SEARCH
        // =====================================================

        List<Map<String, Object>> semanticResults =
                semanticSearch(
                        question,
                        candidateLimit
                );


        // =====================================================
        // 2. KEYWORD / FULL-TEXT SEARCH
        // =====================================================

        List<Map<String, Object>> keywordResults =
                keywordSearch(
                        question,
                        candidateLimit
                );


        // =====================================================
        // 3. MERGE RESULTS
        // =====================================================

        /*
         * LinkedHashMap prevents duplicate chunks.
         *
         * Key = embedding row ID.
         */

        Map<Object, Map<String, Object>> merged =
                new LinkedHashMap<>();


        // Add semantic results first
        for (Map<String, Object> result :
                semanticResults) {

            merged.put(
                    result.get("id"),
                    result
            );
        }


        // Add keyword results
        for (Map<String, Object> result :
                keywordResults) {

            Object id =
                    result.get("id");

            /*
             * If the chunk already exists,
             * keep the semantic result.
             *
             * Otherwise add the keyword result.
             */

            merged.putIfAbsent(
                    id,
                    result
            );
        }


        // =====================================================
        // 4. RETURN TOP RESULTS
        // =====================================================

        List<Map<String, Object>> finalResults =
                new ArrayList<>(
                        merged.values()
                );


        if (finalResults.size() > limit) {

            return finalResults.subList(
                    0,
                    limit
            );
        }


        return finalResults;
    }


    // =========================================================
    // SEMANTIC VECTOR SEARCH
    // =========================================================

    private List<Map<String, Object>> semanticSearch(
            String question,
            int limit) {


        // -----------------------------------------------------
        // Generate question embedding
        // -----------------------------------------------------

        float[] embedding =
                embeddingService.generateEmbedding(
                        question
                );


        // -----------------------------------------------------
        // Convert to pgvector format
        // -----------------------------------------------------

        String vector =
                convertToVectorString(
                        embedding
                );


        // -----------------------------------------------------
        // Vector similarity search
        // -----------------------------------------------------

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


        /*
         * Previous threshold:
         *
         *     0.30
         *
         * We lower it slightly because certificate
         * questions can use wording different from
         * the actual document.
         */

        double similarityThreshold =
                0.20;


        return jdbcTemplate.queryForList(
                sql,

                vector,

                vector,

                similarityThreshold,

                vector,

                limit
        );
    }


    // =========================================================
    // KEYWORD / FULL-TEXT SEARCH
    // =========================================================

    private List<Map<String, Object>> keywordSearch(
            String question,
            int limit) {


        /*
         * PostgreSQL full-text search.
         *
         * Example:
         *
         * Question:
         *
         * "Give me the issue date of Linux certification"
         *
         * PostgreSQL searches for important terms such as:
         *
         * Linux
         * issue
         * date
         * certification
         */

        String sql = """
                SELECT
                    id,
                    document_id,
                    chunk_number,
                    content,

                    ts_rank(
                        to_tsvector(
                            'simple',
                            content
                        ),
                        plainto_tsquery(
                            'simple',
                            ?
                        )
                    ) AS similarity

                FROM document_embeddings

                WHERE to_tsvector(
                        'simple',
                        content
                      )
                      @@ plainto_tsquery(
                          'simple',
                          ?
                      )

                ORDER BY similarity DESC

                LIMIT ?
                """;


        return jdbcTemplate.queryForList(
                sql,

                question,

                question,

                limit
        );
    }


    // =========================================================
    // CONVERT EMBEDDING TO PGVECTOR STRING
    // =========================================================

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

            builder.append(
                    embedding[i]
            );
        }


        builder.append("]");

        return builder.toString();
    }
}