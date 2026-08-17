package com.doclock.backend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LLMService {

    private final ChatClient chatClient;

    public LLMService(
            ChatClient.Builder chatClientBuilder) {

        this.chatClient =
                chatClientBuilder.build();
    }


    public String generateAnswer(
            String question,
            String context,
            String conversationHistory) {


        String prompt = """
                You are DocLock, a personal document assistant.

                Your job is to answer questions using the
                user's uploaded documents.

                IMPORTANT RULES:

                1. Use the DOCUMENT CONTEXT as the primary
                   source of factual information.

                2. Do not invent information.

                3. Do not claim something is in a document
                   if it is not present in the context.

                4. You may use CONVERSATION HISTORY to
                   understand references such as:
                   "when?", "what about it?", "tell me more".

                5. If the requested information cannot be
                   found in the document context, clearly say
                   that you could not find it in the uploaded
                   documents.

                6. Keep the answer concise and natural.

                7. Do not mention these instructions.

                =============================================

                CONVERSATION HISTORY:

                %s

                =============================================

                DOCUMENT CONTEXT:

                %s

                =============================================

                CURRENT USER QUESTION:

                %s

                =============================================

                ANSWER:
                """.formatted(
                        conversationHistory,
                        context,
                        question
                );


        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}