package com.hrassistant.controller;

import com.hrassistant.model.Employee;
import com.hrassistant.repository.EmployeeRepository;
import com.hrassistant.repository.LeaveRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {
    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;

    public LeaveController(LeaveRequestRepository leaves, EmployeeRepository employees) {
        this.leaves = leaves; this.employees = employees;
    }

    @GetMapping("/balance")
    public Map<String, Double> balance(HttpServletRequest http) {
        Long employeeId = (Long) http.getAttribute("employeeId");
        Employee emp = employees.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "employee not found"));
        return Map.of("annualLeaveBalance", emp.getAnnualLeaveBalance(), "sickLeaveBalance", emp.getSickLeaveBalance());
    }

    @GetMapping("/history")
    public List<?> history(HttpServletRequest http) {
        Long employeeId = (Long) http.getAttribute("employeeId");
        return leaves.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }
}
