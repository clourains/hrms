package com.hrms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com/v1";
    private String model = "deepseek-chat";
    private Integer maxTokens = 2000;
    private Double temperature = 0.7;
    private Boolean enabled = false;

    public String getChatUrl() {
        return baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
    }
}
