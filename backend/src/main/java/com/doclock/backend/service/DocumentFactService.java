package com.doclock.backend.service;

import com.doclock.backend.entity.Document;
import com.doclock.backend.entity.DocumentFact;
import com.doclock.backend.repository.DocumentFactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts high-value, source-linked fields for factual document questions. */
@Service
@RequiredArgsConstructor
public class DocumentFactService {
    private final DocumentFactRepository documentFactRepository;

    private static final Pattern AADHAAR = Pattern.compile("(?<!\\d)(\\d{4}[ -]?\\d{4}[ -]?\\d{4})(?!\\d)");
    private static final Pattern PAN = Pattern.compile("\\b([A-Z]{5}[0-9]{4}[A-Z])\\b");
    private static final Pattern CERTIFICATE = Pattern.compile("(?i)(?:certificate|certification|credential|registration)\\s*(?:no\\.?|number|#|id)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9/_-]{4,})");
    private static final Pattern IMPORTANT_DATE = Pattern.compile("(?i)(?:renewal|renew|expiry|expires|valid\\s*(?:until|through|till)?|validity)\\s*(?:date)?\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4}|[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4})");
    private static final Pattern EMAIL = Pattern.compile("\\b([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)([6-9]\\d{9})(?!\\d)");

    @Transactional
    public void extractAndSaveFacts(Document document) {
        documentFactRepository.deleteByDocumentId(document.getId());
        String text = document.getExtractedText();
        Set<String> seen = new HashSet<>();
        extract(document, text, AADHAAR, "AADHAAR_NUMBER", 0.88, 1, seen);
        extract(document, text, PAN, "PAN_NUMBER", 0.94, 1, seen);
        extract(document, text, CERTIFICATE, "CERTIFICATE_NUMBER", 0.82, 1, seen);
        extract(document, text, IMPORTANT_DATE, "RENEWAL_OR_EXPIRY_DATE", 0.86, 1, seen);
        extract(document, text, EMAIL, "EMAIL", 0.97, 1, seen);
        extract(document, text, PHONE, "PHONE_NUMBER", 0.78, 1, seen);
    }

    public List<DocumentFact> findRelevantFacts(String question) {
        String value = question.toLowerCase(Locale.ROOT);
        Set<String> types = new LinkedHashSet<>();
        if (value.matches(".*\\b(aadhaar|aadhar|uidai|uid)\\b.*")) types.add("AADHAAR_NUMBER");
        if (value.matches(".*\\b(certificate|certification|credential|registration)\\b.*")) types.add("CERTIFICATE_NUMBER");
        if (value.matches(".*\\b(renewal|renew|expiry|expire|validity|valid)\\b.*")) types.add("RENEWAL_OR_EXPIRY_DATE");
        if (value.matches(".*\\b(pan)\\b.*")) types.add("PAN_NUMBER");
        if (value.matches(".*\\b(email|mail)\\b.*")) types.add("EMAIL");
        if (value.matches(".*\\b(phone|mobile|contact)\\b.*")) types.add("PHONE_NUMBER");
        return types.isEmpty() ? List.of() : documentFactRepository.findByFactTypeIn(types);
    }

    @Transactional
    public void deleteFactsByDocumentId(Long documentId) {
        documentFactRepository.deleteByDocumentId(documentId);
    }

    public String toContext(List<DocumentFact> facts, boolean revealSensitive) {
        StringBuilder context = new StringBuilder();
        for (DocumentFact fact : facts) {
            context.append("[Structured fact | ").append(fact.getFactType()).append(" | Source: ")
                    .append(fact.getDocument().getFileName()).append("] ")
                    .append(maskIfSensitive(fact, revealSensitive)).append("\n");
        }
        return context.toString();
    }

    public String fallbackAnswer(List<DocumentFact> facts, boolean revealSensitive) {
        DocumentFact fact = facts.getFirst();
        return fact.getFactType().replace('_', ' ') + ": " + maskIfSensitive(fact, revealSensitive)
                + "\n[Source: " + fact.getDocument().getFileName() + "]";
    }

    private void extract(Document document, String text, Pattern pattern, String type, double confidence,
                         int group, Set<String> seen) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(group).trim().replaceAll("\\s+", " ");
            if (seen.add(type + ':' + value)) {
                int start = Math.max(0, matcher.start() - 100);
                int end = Math.min(text.length(), matcher.end() + 100);
                documentFactRepository.save(DocumentFact.builder().document(document).factType(type)
                        .value(value).confidence(confidence).evidence(text.substring(start, end).replaceAll("\\s+", " "))
                        .build());
            }
        }
    }

    private String maskIfSensitive(DocumentFact fact, boolean revealSensitive) {
        String value = fact.getValue();
        if (revealSensitive || !(fact.getFactType().equals("AADHAAR_NUMBER") || fact.getFactType().equals("PAN_NUMBER"))) return value;
        String compact = value.replaceAll("\\s|[-]", "");
        return "•••• •••• " + compact.substring(Math.max(0, compact.length() - 4));
    }
}
