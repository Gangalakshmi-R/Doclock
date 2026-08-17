package com.doclock.backend.controller;

import com.doclock.backend.entity.Document;
import com.doclock.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


    // ==========================================
    // UPLOAD PDF
    // ==========================================

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file")
            MultipartFile file
    ) throws IOException {

        Document document =
                documentService.uploadDocument(file);

        return ResponseEntity.ok(document);
    }


    // ==========================================
    // GET ALL DOCUMENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<Document>>
    getAllDocuments() {

        return ResponseEntity.ok(
                documentService.getAllDocuments()
        );
    }


    // ==========================================
    // GET DOCUMENT BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Document>
    getDocumentById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                documentService
                        .getDocumentById(id)
        );
    }


    // ==========================================
    // DELETE DOCUMENT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String>
    deleteDocument(
            @PathVariable Long id
    ) {

        documentService.deleteDocument(id);

        return ResponseEntity.ok(
                "Document deleted successfully"
        );
    }
}