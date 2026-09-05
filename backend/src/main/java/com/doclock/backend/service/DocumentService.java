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
import java.util.Set;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final DocumentTextExtractor documentTextExtractor;

    private final DocumentChunkingService documentChunkingService;

    private final DocumentFactService documentFactService;

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
        // VALIDATE SUPPORTED DOCUMENT TYPE
        // =====================================================

        String fileType = detectContentType(file, originalFileName);

        if (!isSupportedDocument(fileType, originalFileName)) {

            throw new IllegalArgumentException(
                    "Supported formats: PDF, Word, Excel, PowerPoint, text, CSV, and OpenDocument files"
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

        String checksum = calculateChecksum(file);
        documentRepository.findByChecksum(checksum).ifPresent(existing -> {
            throw new IllegalArgumentException("This exact file is already in your vault: " + existing.getFileName());
        });


        // =====================================================
        // CREATE DOCUMENT
        // =====================================================

        Document document =
                Document.builder()
                        .fileName(originalFileName)
                        .fileType(fileType)
                        .fileSize(file.getSize())
                        .filePath(filePath.toString())
                        .checksum(checksum)
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
            document.setSummary(createSummary(extractedText));


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

            documentFactService.extractAndSaveFacts(savedDocument);

            savedDocument.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(savedDocument);


            return savedDocument;

        } catch (Exception e) {

            document.setStatus(
                    DocumentStatus.FAILED
            );
            document.setProcessingError("Document processing failed. Upload a readable, text-based document and try again.");

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

    public List<Document> getAllDocuments(String query) {
        if (query == null || query.isBlank()) {
            return documentRepository.findAllByOrderByUploadedAtDesc();
        }
        return documentRepository.findByFileNameContainingIgnoreCaseOrderByUploadedAtDesc(query.trim());
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

        documentFactService.deleteFactsByDocumentId(id);


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

    private String detectContentType(MultipartFile file, String fileName) throws IOException {
        try (var inputStream = file.getInputStream()) {
            String detected = new org.apache.tika.Tika().detect(inputStream, fileName);
            return detected == null || detected.isBlank() ? "application/octet-stream" : detected;
        }
    }

    private boolean isSupportedDocument(String contentType, String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt", "ods", "odp")
                .contains(extension)
                || Set.of("application/pdf", "text/plain", "text/csv", "application/rtf",
                        "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet",
                        "application/vnd.oasis.opendocument.presentation").contains(contentType);
    }

    private String calculateChecksum(MultipartFile file) throws IOException {
        try (var inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int read; (read = inputStream.read(buffer)) != -1;) digest.update(buffer, 0, read);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String createSummary(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) return normalized;
        int boundary = normalized.lastIndexOf(' ', 500);
        return normalized.substring(0, boundary > 100 ? boundary : 500) + "...";
    }
}
