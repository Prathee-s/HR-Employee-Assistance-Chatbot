package com.hrassistant.controller;

import com.hrassistant.dto.ChatRequest;
import com.hrassistant.dto.ChatResponse;
import com.hrassistant.model.ChatMessage;
import com.hrassistant.repository.ChatMessageRepository;
import com.hrassistant.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatMessageRepository chats;

    public ChatController(ChatService chatService, ChatMessageRepository chats) {
        this.chatService = chatService; this.chats = chats;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, HttpServletRequest http) {
        Long employeeId = (Long) http.getAttribute("employeeId");
        return chatService.chat(req.sessionId(), employeeId, req.message());
    }

    @GetMapping("/{sessionId}/history")
    public List<ChatMessage> history(@PathVariable String sessionId) {
        return chats.findBySessionIdOrderByIdAsc(sessionId);
    }
}
