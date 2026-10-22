package com.hrms.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    SseEmitter chat(String message, String conversationId);

    String buildHrContext();
}
