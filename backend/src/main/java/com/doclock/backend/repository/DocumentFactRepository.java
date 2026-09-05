package com.doclock.backend.repository;

import com.doclock.backend.entity.DocumentFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;

public interface DocumentFactRepository extends JpaRepository<DocumentFact, Long> {
    @EntityGraph(attributePaths = "document")
    List<DocumentFact> findByFactTypeIn(Collection<String> factTypes);
    void deleteByDocumentId(Long documentId);
}
