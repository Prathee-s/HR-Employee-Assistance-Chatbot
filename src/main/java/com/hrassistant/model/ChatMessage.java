package com.hrassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_session", columnList = "sessionId"))
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String sessionId;
    @Column(nullable = false)
    private Long employeeId;
    @Column(nullable = false, length = 16)
    private String role; // "user" or "assistant"
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {}
    public ChatMessage(String sessionId, Long employeeId, String role, String content) {
        this.sessionId = sessionId; this.employeeId = employeeId; this.role = role; this.content = content;
    }
    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public Long getEmployeeId() { return employeeId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
