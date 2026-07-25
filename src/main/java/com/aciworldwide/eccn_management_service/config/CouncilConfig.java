package com.aciworldwide.eccn_management_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "eccn.council")
public class CouncilConfig {

    private List<String> members = List.of(
        "openai/gpt-4.1",
        "google/gemini-2.5-pro",
        "anthropic/claude-sonnet-4.5"
    );

    private String chairman = "anthropic/claude-sonnet-4.5";

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getChairman() {
        return chairman;
    }

    public void setChairman(String chairman) {
        this.chairman = chairman;
    }
}
