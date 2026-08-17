package com.doclock.backend.service;

import com.doclock.backend.entity.Document;
import com.doclock.backend.entity.DocumentChunk;
import com.doclock.backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkingService {

    private final DocumentChunkRepository documentChunkRepository;

    private final DocumentEmbeddingService documentEmbeddingService;


    private static final int CHUNK_SIZE = 1000;

    private static final int CHUNK_OVERLAP = 200;


    // =========================================================
    // CREATE CHUNKS
    // =========================================================

    public List<DocumentChunk> createChunks(Document document) {

        String text = document.getExtractedText();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Document contains no extractable text"
            );
        }

        List<DocumentChunk> chunks =
                new ArrayList<>();

        int start = 0;

        int chunkNumber = 1;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + CHUNK_SIZE,
                            text.length()
                    );

            String chunkText =
                    text.substring(start, end)
                            .trim();

            if (!chunkText.isBlank()) {

                DocumentChunk chunk =
                        DocumentChunk.builder()
                                .document(document)
                                .chunkNumber(chunkNumber)
                                .content(chunkText)
                                .build();

                DocumentChunk savedChunk =
                        documentChunkRepository.save(chunk);

                documentEmbeddingService.storeEmbedding(
                        document.getId(),
                        savedChunk.getChunkNumber(),
                        savedChunk.getContent()
                );

                chunks.add(savedChunk);
            }

            if (end == text.length()) {
                break;
            }

            start =
                    end - CHUNK_OVERLAP;

            chunkNumber++;
        }

        return chunks;
    }


    // =========================================================
    // DELETE ALL CHUNKS FOR DOCUMENT
    // =========================================================

    public void deleteChunksByDocumentId(Long documentId) {

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentId(documentId);

        if (chunks != null && !chunks.isEmpty()) {

            documentChunkRepository.deleteAll(chunks);
        }
    }
}