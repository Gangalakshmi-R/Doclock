package com.doclock.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

    private final EmbeddingService embeddingService;

    private final JdbcTemplate jdbcTemplate;

    public void storeEmbedding(
            Long documentId,
            Integer chunkNumber,
            String content) {

        if (content == null || content.isBlank()) {
            return;
        }

        // Generate embedding
        float[] embedding =
                embeddingService.generateEmbedding(content);

        // Convert to pgvector format
        String vector =
                convertToVectorString(embedding);

        String sql = """
                INSERT INTO document_embeddings
                (
                    document_id,
                    chunk_number,
                    content,
                    embedding
                )
                VALUES (?, ?, ?, ?::vector)
                """;

        jdbcTemplate.update(
                sql,
                documentId,
                chunkNumber,
                content,
                vector
        );
    }

    private String convertToVectorString(
            float[] embedding) {

        StringBuilder builder =
                new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {

            if (i > 0) {
                builder.append(",");
            }

            builder.append(embedding[i]);
        }

        builder.append("]");

        return builder.toString();
    }
}