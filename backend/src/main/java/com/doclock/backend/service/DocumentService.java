package com.doclock.backend.service;

import com.doclock.backend.entity.Document;
import com.doclock.backend.entity.DocumentStatus;
import com.doclock.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:uploads}")
    private String uploadDirectoryPath;


    // =========================================================
    // UPLOAD DOCUMENT
    // =========================================================

    public Document uploadDocument(
            MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }


        // =====================================================
        // FILE NAME
        // =====================================================

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid file name"
            );
        }


        // =====================================================
        // VALIDATE PDF
        // =====================================================

        String fileType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(fileType)
                || !hasPdfSignature(file)) {

            throw new IllegalArgumentException(
                    "Only PDF files are allowed"
            );
        }


        // =====================================================
        // CREATE UPLOAD DIRECTORY
        // =====================================================

        Path uploadDirectory = Paths.get(uploadDirectoryPath).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);


        // =====================================================
        // UNIQUE FILE NAME
        // =====================================================

        String safeFileName = Paths.get(originalFileName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._ -]", "_");
        String storedFileName = UUID.randomUUID() + "_" + safeFileName;


        Path filePath =
                uploadDirectory.resolve(
                        storedFileName
                );


        // =====================================================
        // SAVE PHYSICAL FILE
        // =====================================================

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }


        // =====================================================
        // CREATE DOCUMENT
        // =====================================================

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
            // EXTRACT TEXT
            // =================================================

            String extractedText =
                    documentTextExtractor
                            .extractText(filePath);


            if (extractedText == null
                    || extractedText.isBlank()) {

                throw new IOException(
                        "No readable text found in document"
                );
            }


            // =================================================
            // STORE EXTRACTED TEXT
            // =================================================

            document.setExtractedText(
                    extractedText
            );


            // =================================================
            // SET STATUS
            // =================================================

            Document savedDocument = documentRepository.save(document);


            // =================================================
            // CREATE CHUNKS + EMBEDDINGS
            // =================================================

            documentChunkingService
                    .createChunks(
                            savedDocument
                    );

            savedDocument.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(savedDocument);


            return savedDocument;

        } catch (Exception e) {

            document.setStatus(
                    DocumentStatus.FAILED
            );

            documentRepository.save(
                    document
            );

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

    public Document getDocumentById(
            Long id
    ) {

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

    @Transactional
    public void deleteDocument(
            Long id
    ) {

        // =====================================================
        // 1. FIND DOCUMENT
        // =====================================================

        Document document =
                getDocumentById(id);


        // =====================================================
        // 2. DELETE EMBEDDINGS
        // =====================================================

        // =====================================================
        // 2. DELETE DOCUMENT CHUNKS (and their embeddings)
        // =====================================================

        documentChunkingService
                .deleteChunksByDocumentId(
                        id
                );


        // =====================================================
        // 3. DELETE PHYSICAL PDF
        // =====================================================

        try {

            if (document.getFilePath() != null) {

                Path filePath =
                        Paths.get(
                                document.getFilePath()
                        );

                Files.deleteIfExists(
                        filePath
                );
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to delete physical file",
                    e
            );
        }


        // =====================================================
        // 4. DELETE DOCUMENT
        // =====================================================

        documentRepository.delete(
                document
        );
    }

    public Path getDocumentFile(Long id) {
        Path path = Paths.get(getDocumentById(id).getFilePath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("The uploaded file is no longer available");
        }
        return path;
    }

    private boolean hasPdfSignature(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(5);
            return signature.length == 5
                    && signature[0] == '%'
                    && signature[1] == 'P'
                    && signature[2] == 'D'
                    && signature[3] == 'F'
                    && signature[4] == '-';
        }
    }
}
