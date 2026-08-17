package com.doclock.backend.repository;

import com.doclock.backend.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository
        extends JpaRepository<ChatConversation, Long> {
}