package com.hrassistant.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"employeeId", "date"}))
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long employeeId;
    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false, length = 20)
    private String status; // PRESENT, ABSENT, ON_LEAVE, HALF_DAY
    private String checkIn;
    private String checkOut;

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long v) { this.employeeId = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String v) { this.checkIn = v; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String v) { this.checkOut = v; }
}
