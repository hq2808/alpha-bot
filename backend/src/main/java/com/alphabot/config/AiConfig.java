package com.alphabot.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${langchain4j.openai.chat-model.api-key:gsk_placeholder}")
    private String apiKey;

    @Value("${langchain4j.openai.chat-model.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${langchain4j.openai.chat-model.model-name:llama-3.3-70b-versatile}")
    private String modelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.1)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
