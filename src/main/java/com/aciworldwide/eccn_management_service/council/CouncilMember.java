package com.aciworldwide.eccn_management_service.council;

import org.springframework.ai.chat.client.ChatClient;

public record CouncilMember(String name, ChatClient client, String provider) {}
