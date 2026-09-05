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
        // 1. GET EXISTING CONVERSATION OR CREATE NEW ONE
        // =================================================

        ChatConversation conversation;

        if (request.getConversationId() == null) {

            conversation =
                    createConversation(question);

        } else {

            conversation =
                    chatConversationRepository
                            .findById(
                                    request.getConversationId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found"
                                    )
                            );
        }


        // =================================================
        // 2. SAVE USER MESSAGE
        // =================================================

        ChatMessage userMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(MessageRole.USER)
                        .content(question)
                        .createdAt(LocalDateTime.now())
                        .build();

        chatMessageRepository.save(
                userMessage
        );


        // =================================================
        // 3. GET CONVERSATION HISTORY
        // =================================================

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        // =================================================
        // 4. BUILD CONVERSATION HISTORY
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
        // 5. BUILD CONTEXT-AWARE RETRIEVAL QUERY
        //
        // The current question may contain references such
        // as:
        //
        // "it"
        // "that"
        // "when was it"
        // "who issued it"
        //
        // Include recent conversation context so semantic
        // search understands what the user is referring to.
        // =================================================

        String retrievalQuery =
                buildRetrievalQuery(
                        previousMessages,
                        question
                );


        // =================================================
        // 6. SEMANTIC SEARCH
        // =================================================

        List<Map<String, Object>> results = semanticSearchService.search(retrievalQuery, 5);


        // =================================================
        // 7. NO RELEVANT DOCUMENTS
        // =================================================

        if (results.isEmpty()) {

            String answer =
                    "I couldn't find relevant information "
                    + "in your uploaded documents.";

            saveAssistantMessage(
                    conversation,
                    answer
            );

            conversation.setUpdatedAt(
                    LocalDateTime.now()
            );

            chatConversationRepository.save(
                    conversation
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
        // 8. BUILD DOCUMENT CONTEXT
        // =================================================

        StringBuilder context =
                new StringBuilder();

        for (Map<String, Object> result : results) {

            Object content =
                    result.get("content");

            if (content != null) {

                context.append("[Source: ").append(result.get("documentName"))
                        .append(", chunk ").append(result.get("chunkNumber"))
                        .append("]\n").append(content).append("\n\n");
            }
        }


        // =================================================
        // 9. GENERATE GEMINI ANSWER
        //
        // IMPORTANT:
        // Send the ORIGINAL user question to the LLM.
        //
        // Retrieval gets the expanded contextual query,
        // while Gemini sees the actual question + history.
        // =================================================

        String answer =
                llmService.generateAnswer(
                        question,
                        context.toString(),
                        history.toString()
                );


        // =================================================
        // 10. SAVE ASSISTANT MESSAGE
        // =================================================

        saveAssistantMessage(
                conversation,
                answer
        );


        // =================================================
        // 11. UPDATE CONVERSATION TIMESTAMP
        // =================================================

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        chatConversationRepository.save(
                conversation
        );


        // =================================================
        // 12. PREPARE SOURCES
        // =================================================

        List<Map<String, Object>> sources =
                new ArrayList<>();

        for (Map<String, Object> result : results) {

            Map<String, Object> source =
                    new HashMap<>();

            source.put(
                    "documentId",
                    result.get("documentId")
            );

            source.put(
                    "chunkNumber",
                    result.get("chunkNumber")
            );

            source.put(
                    "documentName",
                    result.get("documentName")
            );

            source.put("relevance", result.get("relevance"));

            sources.add(
                    source
            );
        }


        // =================================================
        // 13. FINAL RESPONSE
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
    // BUILD CONTEXT-AWARE RETRIEVAL QUERY
    // =====================================================

    private String buildRetrievalQuery(
            List<ChatMessage> messages,
            String currentQuestion
    ) {

        /*
         * New conversation:
         *
         * Just search the current question.
         */

        if (messages.size() <= 1) {

            return currentQuestion;
        }


        /*
         * Existing conversation:
         *
         * Include the recent conversation so queries such
         * as "when did I receive it?" can be connected to
         * the previous topic.
         */

        StringBuilder query =
                new StringBuilder();

        int totalMessages =
                messages.size();

        /*
         * Use the most recent few messages rather than the
         * entire conversation. This prevents very long
         * conversations from polluting semantic search.
         */

        int startIndex =
                Math.max(
                        0,
                        totalMessages - 4
                );

        for (
                int i = startIndex;
                i < totalMessages;
                i++
        ) {

            ChatMessage message =
                    messages.get(i);

            query.append(
                    message.getContent()
            );

            query.append(
                    " "
            );
        }


        /*
         * Add the current question explicitly.
         */

        query.append(
                currentQuestion
        );


        return query.toString().trim();
    }


    // =====================================================
    // CREATE NEW CONVERSATION
    // =====================================================

    private ChatConversation createConversation(
            String question
    ) {

        String title =
                question.length() > 50
                        ? question.substring(
                                0,
                                50
                        ) + "..."
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
    // SAVE ASSISTANT MESSAGE
    // =====================================================

    private void saveAssistantMessage(
            ChatConversation conversation,
            String answer
    ) {

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
