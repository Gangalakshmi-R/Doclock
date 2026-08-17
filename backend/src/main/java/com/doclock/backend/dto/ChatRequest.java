package com.doclock.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    private Long conversationId;

    private String question;
}