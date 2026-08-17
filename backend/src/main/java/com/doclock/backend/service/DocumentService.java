package com.doclock.backend.service;

import com.doclock.backend.entity.Document;
import com.doclock.backend.entity.DocumentStatus;
import com.doclock.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final DocumentTextExtractor documentTextExtractor;

    private final DocumentChunkingService documentChunkingService;

    private final Path uploadDirectory = Paths.get("uploads");


    // =========================================================
    // UPLOAD DOCUMENT
    // =========================================================

    public Document uploadDocument(MultipartFile file) throws IOException {

        // 1. Check whether file exists
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }


        // 2. Get original file name
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid file name"
            );
        }


        // 3. Validate PDF
        String fileType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(fileType)) {

            throw new IllegalArgumentException(
                    "Only PDF files are allowed"
            );
        }


        // 4. Create uploads directory
        Files.createDirectories(uploadDirectory);


        // 5. Generate unique file name
        String storedFileName =
                UUID.randomUUID()
                        + "_"
                        + originalFileName;


        // 6. Create physical file path
        Path filePath =
                uploadDirectory.resolve(storedFileName);


        // 7. Save PDF to uploads folder
        Files.copy(
                file.getInputStream(),
                filePath
        );


        // 8. Create Document object
        Document document =
                Document.builder()
                        .fileName(originalFileName)
                        .fileType(fileType)
                        .fileSize(file.getSize())
                        .filePath(filePath.toString())
                        .status(DocumentStatus.PROCESSING)
                        .uploadedAt(LocalDateTime.now())
                        .build();


        try {

            // =================================================
            // 9. EXTRACT TEXT USING APACHE TIKA
            // =================================================

            String extractedText =
                    documentTextExtractor
                            .extractText(filePath);


            if (extractedText == null ||
                    extractedText.isBlank()) {

                throw new IOException(
                        "No readable text found in document"
                );
            }


            // =================================================
            // 10. STORE EXTRACTED TEXT
            // =================================================

            document.setExtractedText(
                    extractedText
            );


            // =================================================
            // 11. UPDATE STATUS
            // =================================================

            document.setStatus(
                    DocumentStatus.PROCESSED
            );


            // =================================================
            // 12. SAVE DOCUMENT TO MYSQL
            // =================================================

            Document savedDocument =
                    documentRepository.save(document);


            // =================================================
            // 13. CREATE DOCUMENT CHUNKS
            // =================================================

            documentChunkingService
                    .createChunks(savedDocument);


            // =================================================
            // 14. RETURN DOCUMENT
            // =================================================

            return savedDocument;

        } catch (Exception e) {

            // =================================================
            // PROCESSING FAILED
            // =================================================

            document.setStatus(
                    DocumentStatus.FAILED
            );

            // Save failed status
            documentRepository.save(document);

            throw new IOException(
                    "Failed to process document: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // GET ALL DOCUMENTS
    // =========================================================

    public List<Document> getAllDocuments() {

        return documentRepository.findAll();
    }


    // =========================================================
    // GET DOCUMENT BY ID
    // =========================================================

    public Document getDocumentById(Long id) {

        return documentRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found with id: "
                                        + id
                        )
                );
    }


    // =========================================================
    // DELETE DOCUMENT
    // =========================================================

    public void deleteDocument(Long id) {

        // 1. Find document
        Document document =
                getDocumentById(id);


        // 2. Delete physical PDF
        try {

            if (document.getFilePath() != null) {

                Path filePath =
                        Paths.get(
                                document.getFilePath()
                        );

                Files.deleteIfExists(filePath);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to delete physical file",
                    e
            );
        }


        // 3. Delete database record
        documentRepository.delete(document);
    }
}