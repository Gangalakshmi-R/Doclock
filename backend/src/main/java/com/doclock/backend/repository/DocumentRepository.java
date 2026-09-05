package com.doclock.backend.repository;

import com.doclock.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByChecksum(String checksum);
    List<Document> findByFileNameContainingIgnoreCaseOrderByUploadedAtDesc(String query);
    List<Document> findAllByOrderByUploadedAtDesc();
}
