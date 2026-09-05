package com.doclock.backend.controller;

import com.doclock.backend.dto.ChatRequest;
import com.doclock.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @PostMapping
    public Map<String, Object> chat(
            @Valid @RequestBody ChatRequest request) {

        return chatService.chat(request);
    }
}
