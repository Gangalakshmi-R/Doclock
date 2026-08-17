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
    // Get all conversations
    // =====================================================

    @GetMapping
    public List<ChatConversation> getAllConversations() {

        return conversationRepository.findAll();
    }


    // =====================================================
    // Get messages of one conversation
    // =====================================================

    @GetMapping("/{id}")
    public List<ChatMessage> getConversation(
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
    // Delete conversation
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
