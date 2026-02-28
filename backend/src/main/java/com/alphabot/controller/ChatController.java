package com.alphabot.controller;

import com.alphabot.service.FinancialAssistant;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final FinancialAssistant assistant;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = assistant.chat(request.getMessage());
        return new ChatResponse(answer);
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String response;

        public ChatResponse(String response) {
            this.response = response;
        }
    }
}
