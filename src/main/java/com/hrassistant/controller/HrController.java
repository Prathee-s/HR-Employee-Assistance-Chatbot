package com.hrassistant.controller;

import com.hrassistant.model.Employee;
import com.hrassistant.model.LeaveRequest;
import com.hrassistant.repository.EmployeeRepository;
import com.hrassistant.repository.EscalationRepository;
import com.hrassistant.repository.LeaveRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Every route here is HR-only - enforced centrally by AuthFilter for the /api/hr/** prefix. */
@RestController
@RequestMapping("/api/hr")
public class HrController {
    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;
    private final EscalationRepository escalations;

    public HrController(LeaveRequestRepository leaves, EmployeeRepository employees, EscalationRepository escalations) {
        this.leaves = leaves; this.employees = employees; this.escalations = escalations;
    }

    @GetMapping("/leave/pending")
    public List<LeaveRequest> pending() {
        return leaves.findByStatusOrderByCreatedAtAsc(LeaveRequest.Status.PENDING);
    }

    @PatchMapping("/leave/{id}/decision")
    public Map<String, Boolean> decide(@PathVariable Long id, @RequestParam String status) {
        LeaveRequest.Status decision;
        try {
            decision = LeaveRequest.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be APPROVED or REJECTED");
        }
        if (decision != LeaveRequest.Status.APPROVED && decision != LeaveRequest.Status.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be APPROVED or REJECTED");
        }
        LeaveRequest lr = leaves.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "leave request not found"));
        lr.setStatus(decision);
        leaves.save(lr);
        if (decision == LeaveRequest.Status.APPROVED) {
            Employee emp = employees.findById(lr.getEmployeeId()).orElseThrow();
            if ("Sick".equalsIgnoreCase(lr.getLeaveType())) {
                emp.setSickLeaveBalance(emp.getSickLeaveBalance() - lr.getDays());
            } else {
                emp.setAnnualLeaveBalance(emp.getAnnualLeaveBalance() - lr.getDays());
            }
            employees.save(emp);
        }
        return Map.of("ok", true);
    }

    @GetMapping("/escalations")
    public List<?> escalations() {
        return escalations.findAllByOrderByCreatedAtDesc();
    }

    @PatchMapping("/escalations/{id}/resolve")
    public Map<String, Boolean> resolve(@PathVariable Long id) {
        var esc = escalations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "escalation not found"));
        esc.setStatus(com.hrassistant.model.Escalation.Status.RESOLVED);
        escalations.save(esc);
        return Map.of("ok", true);
    }
}
