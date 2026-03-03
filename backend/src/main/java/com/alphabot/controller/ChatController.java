package com.alphabot.controller;

import com.alphabot.service.FinancialAssistant;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "AI Chat Assistant", description = "Endpoints for interacting with the AI intelligence assistant")
public class ChatController {

    private final FinancialAssistant assistant;

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Chat with AI assistant", description = "Sends a message to the AI financial assistant and returns its response.")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.getMessage());
        return new ChatResponse(answer);
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "Chat message request body")
    public static class ChatRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "User's message to the assistant", example = "Dự báo giá cổ phiếu FPT?")
        private String message;
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "AI assistant response body")
    public static class ChatResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "Assistant's answer in Markdown")
        private String response;

        public ChatResponse(String response) {
            this.response = response;
        }
    }
}
