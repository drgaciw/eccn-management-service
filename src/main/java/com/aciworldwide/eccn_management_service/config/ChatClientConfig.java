package com.aciworldwide.eccn_management_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    @Qualifier("openAiClient")
    @ConditionalOnBean(name = "openAiChatModel")
    public ChatClient openAiClient(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("geminiClient")
    @ConditionalOnBean(name = "geminiChatModel")
    public ChatClient geminiClient(@Qualifier("geminiChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Qualifier("claudeClient")
    @ConditionalOnBean(name = "anthropicChatModel")
    public ChatClient claudeClient(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
