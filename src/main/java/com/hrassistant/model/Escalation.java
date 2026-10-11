package com.hrassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalations")
public class Escalation {
    public enum Status { OPEN, IN_PROGRESS, RESOLVED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long employeeId;
    @Column(nullable = false, length = 64)
    private String sessionId;
    @Column(nullable = false, length = 1000)
    private String query;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long v) { this.employeeId = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getQuery() { return query; }
    public void setQuery(String v) { this.query = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
