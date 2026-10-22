package com.hrms.controller;

import com.hrms.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        String conversationId = body.getOrDefault("conversationId", "");
        return aiChatService.chat(message, conversationId);
    }

    @GetMapping("/context")
    public String getHrContext() {
        return aiChatService.buildHrContext();
    }
}
