package com.doclock.backend.service;

import com.doclock.backend.entity.DocumentChunk;
import com.doclock.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmbeddingBackfillService {
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    /** Rebuilds chunk vectors after an embedding-model change. */
    @Transactional
    public int generateEmbeddings(Long documentId) {
        var chunks = documentChunkRepository.findByDocument_IdOrderByChunkNumber(documentId);
        if (chunks.isEmpty()) throw new IllegalArgumentException("No chunks found for document ID: " + documentId);

        for (DocumentChunk chunk : chunks) {
            chunk.setEmbedding(serialize(embeddingService.generateEmbedding(chunk.getContent())));
        }
        documentChunkRepository.saveAll(chunks);
        return chunks.size();
    }

    private String serialize(float[] embedding) {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) value.append(',');
            value.append(embedding[index]);
        }
        return value.toString();
    }
}
