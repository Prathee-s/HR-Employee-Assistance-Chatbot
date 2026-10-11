package com.hrassistant.controller;

import com.hrassistant.repository.AttendanceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceRepository attendance;

    public AttendanceController(AttendanceRepository attendance) { this.attendance = attendance; }

    @GetMapping
    public List<?> list(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                         HttpServletRequest http) {
        Long employeeId = (Long) http.getAttribute("employeeId");
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();
        return attendance.findByEmployeeAndMonth(employeeId, y, m);
    }
}
