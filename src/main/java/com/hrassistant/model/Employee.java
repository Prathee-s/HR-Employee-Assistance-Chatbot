package com.hrassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {
    public enum Role { EMPLOYEE, HR }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @JsonIgnore
    @Column(nullable = false, length = 200)
    private String passwordHash;
    private String department;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;
    @Column(nullable = false)
    private Double annualLeaveBalance = 18.0;
    @Column(nullable = false)
    private Double sickLeaveBalance = 10.0;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String p) { this.passwordHash = p; }
    public String getDepartment() { return department; }
    public void setDepartment(String d) { this.department = d; }
    public Role getRole() { return role; }
    public void setRole(Role r) { this.role = r; }
    public Double getAnnualLeaveBalance() { return annualLeaveBalance; }
    public void setAnnualLeaveBalance(Double v) { this.annualLeaveBalance = v; }
    public Double getSickLeaveBalance() { return sickLeaveBalance; }
    public void setSickLeaveBalance(Double v) { this.sickLeaveBalance = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
