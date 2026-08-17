package com.doclock.backend.controller;

import com.doclock.backend.entity.ChatConversation;
import com.doclock.backend.entity.ChatMessage;
import com.doclock.backend.repository.ChatConversationRepository;
import com.doclock.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatConversationRepository conversationRepository;

    private final ChatMessageRepository messageRepository;


    // =====================================================
    // GET ALL CONVERSATIONS
    // =====================================================

    @GetMapping
    public List<ChatConversation> getAllConversations() {

        return conversationRepository.findAll();
    }


    // =====================================================
    // GET MESSAGES OF ONE CONVERSATION
    // =====================================================

    @GetMapping("/{id}/messages")
    public List<ChatMessage> getConversationMessages(
            @PathVariable Long id) {

        if (!conversationRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Conversation not found"
            );
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(id);
    }


    // =====================================================
    // DELETE CONVERSATION
    // =====================================================

    @DeleteMapping("/{id}")
    public String deleteConversation(
            @PathVariable Long id) {

        if (!conversationRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Conversation not found"
            );
        }

        conversationRepository.deleteById(id);

        return "Conversation deleted successfully";
    }
}