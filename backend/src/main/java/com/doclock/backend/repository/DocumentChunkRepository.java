package com.doclock.backend.repository;

import com.doclock.backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocument_IdOrderByChunkNumber(
            Long documentId
    );

    List<DocumentChunk> findByDocumentId(Long documentId);
}