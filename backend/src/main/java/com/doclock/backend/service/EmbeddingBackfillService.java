package com.doclock.backend.service;

import com.doclock.backend.entity.DocumentChunk;
import com.doclock.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingBackfillService {

    private final EmbeddingService embeddingService;

    private final DocumentChunkRepository documentChunkRepository;

    private final JdbcTemplate jdbcTemplate;


    // =========================================================
    // GENERATE EMBEDDINGS
    // =========================================================

    public int generateEmbeddings(Long documentId) {

        // Get chunks from PostgreSQL
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocument_IdOrderByChunkNumber(
                                documentId
                        );


        if (chunks.isEmpty()) {

            throw new IllegalArgumentException(
                    "No chunks found for document ID: "
                            + documentId
            );
        }


        // Delete old embeddings
        jdbcTemplate.update(
                """
                DELETE FROM document_embeddings
                WHERE document_id = ?
                """,
                documentId
        );


        int count = 0;


        // Generate embedding for each chunk
        for (DocumentChunk chunk : chunks) {

            String content =
                    chunk.getContent();


            if (content == null ||
                    content.isBlank()) {

                continue;
            }


            float[] embedding =
                    embeddingService
                            .generateEmbedding(content);


            String vector =
                    convertToVectorString(
                            embedding
                    );


            jdbcTemplate.update(
                    """
                    INSERT INTO document_embeddings
                    (
                        document_id,
                        chunk_number,
                        content,
                        embedding
                    )
                    VALUES (?, ?, ?, ?::vector)
                    """,

                    documentId,
                    chunk.getChunkNumber(),
                    content,
                    vector
            );


            count++;
        }


        return count;
    }


    // =========================================================
    // VECTOR CONVERSION
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

            builder.append(embedding[i]);
        }


        builder.append("]");

        return builder.toString();
    }
}