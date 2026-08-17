package com.doclock.backend.service;

import com.doclock.backend.dto.ChatRequest;
import com.doclock.backend.entity.ChatConversation;
import com.doclock.backend.entity.ChatMessage;
import com.doclock.backend.entity.MessageRole;
import com.doclock.backend.repository.ChatConversationRepository;
import com.doclock.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SemanticSearchService semanticSearchService;

    private final LLMService llmService;

    private final ChatConversationRepository chatConversationRepository;

    private final ChatMessageRepository chatMessageRepository;


    public Map<String, Object> chat(ChatRequest request) {

        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be empty"
            );
        }


        // =================================================
        // 1. Get existing conversation or create new one
        // =================================================

        ChatConversation conversation;

        if (request.getConversationId() == null) {

            conversation = createConversation(question);

        } else {

            conversation =
                    chatConversationRepository
                            .findById(request.getConversationId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found"
                                    )
                            );
        }


        // =================================================
        // 2. Save USER message
        // =================================================

        ChatMessage userMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(MessageRole.USER)
                        .content(question)
                        .createdAt(LocalDateTime.now())
                        .build();

        chatMessageRepository.save(userMessage);


        // =================================================
        // 3. Get previous conversation history
        // =================================================

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        // =================================================
        // 4. Semantic search
        // =================================================

        List<Map<String, Object>> results =
                semanticSearchService.search(
                        question,
                        3
                );


        // =================================================
        // 5. No relevant documents
        // =================================================

        if (results.isEmpty()) {

            String answer =
                    "I couldn't find relevant information "
                    + "in your uploaded documents.";


            saveAssistantMessage(
                    conversation,
                    answer
            );


            return Map.of(
                    "conversationId",
                    conversation.getId(),

                    "answer",
                    answer,

                    "sources",
                    List.of()
            );
        }


        // =================================================
        // 6. Build document context
        // =================================================

        StringBuilder context =
                new StringBuilder();

        for (Map<String, Object> result : results) {

            context.append(
                    result.get("content")
            );

            context.append("\n\n");
        }


        // =================================================
        // 7. Build conversation history
        // =================================================

        StringBuilder history =
                new StringBuilder();

        for (ChatMessage message : previousMessages) {

            history.append(
                    message.getRole()
            );

            history.append(": ");

            history.append(
                    message.getContent()
            );

            history.append("\n");
        }


        // =================================================
        // 8. Generate Gemini answer
        // =================================================

        String answer =
                llmService.generateAnswer(
                        question,
                        context.toString(),
                        history.toString()
                );


        // =================================================
        // 9. Save ASSISTANT message
        // =================================================

        saveAssistantMessage(
                conversation,
                answer
        );


        // =================================================
        // 10. Update conversation timestamp
        // =================================================

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        chatConversationRepository.save(
                conversation
        );


        // =================================================
        // 11. Prepare sources
        // =================================================

        List<Map<String, Object>> sources =
                new ArrayList<>();

        for (Map<String, Object> result : results) {

            Map<String, Object> source =
                    new HashMap<>();

            source.put(
                    "documentId",
                    result.get("document_id")
            );

            source.put(
                    "chunkNumber",
                    result.get("chunk_number")
            );

            source.put(
                    "similarity",
                    result.get("similarity")
            );

            sources.add(source);
        }


        // =================================================
        // 12. Final response
        // =================================================

        return Map.of(
                "conversationId",
                conversation.getId(),

                "answer",
                answer,

                "sources",
                sources
        );
    }


    // =====================================================
    // Create new conversation
    // =====================================================

    private ChatConversation createConversation(
            String question) {

        String title = question.length() > 50
                ? question.substring(0, 50) + "..."
                : question;

        LocalDateTime now =
                LocalDateTime.now();

        ChatConversation conversation =
                ChatConversation.builder()
                        .title(title)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        return chatConversationRepository.save(
                conversation
        );
    }


    // =====================================================
    // Save assistant message
    // =====================================================

    private void saveAssistantMessage(
            ChatConversation conversation,
            String answer) {

        ChatMessage assistantMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(MessageRole.ASSISTANT)
                        .content(answer)
                        .createdAt(LocalDateTime.now())
                        .build();

        chatMessageRepository.save(
                assistantMessage
        );
    }
}