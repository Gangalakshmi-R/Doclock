package com.doclock.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    private Long fileSize;

    private String filePath;

    @Column(unique = true, length = 64)
    @JsonIgnore
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(length = 600)
    private String summary;

    @Column(length = 1000)
    private String processingError;

    @Lob
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String extractedText;
}
