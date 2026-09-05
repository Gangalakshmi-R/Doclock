package com.doclock.backend.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ChatRequest {

    private Long conversationId;

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 2000, message = "Question must be at most 2000 characters")
    private String question;
}
