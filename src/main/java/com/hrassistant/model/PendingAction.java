package com.hrassistant.model;

import jakarta.persistence.*;

/**
 * Holds a draft leave request or escalation awaiting a yes/no confirmation from the
 * employee, so the bot never books a leave request or opens a ticket silently.
 * One row per employee (upserted), cleared once resolved.
 */
@Entity
@Table(name = "pending_actions")
public class PendingAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long employeeId;
    @Column(nullable = false, length = 30)
    private String actionType; // LEAVE_DRAFT, ESCALATION_DRAFT
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON blob

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long v) { this.employeeId = v; }
    public String getActionType() { return actionType; }
    public void setActionType(String v) { this.actionType = v; }
    public String getPayload() { return payload; }
    public void setPayload(String v) { this.payload = v; }
}
