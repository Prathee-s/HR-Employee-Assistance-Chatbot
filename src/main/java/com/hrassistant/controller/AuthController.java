package com.hrassistant.controller;

import com.hrassistant.dto.LoginRequest;
import com.hrassistant.dto.LoginResponse;
import com.hrassistant.model.Employee;
import com.hrassistant.repository.EmployeeRepository;
import com.hrassistant.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(EmployeeRepository employees, PasswordEncoder encoder, JwtService jwtService) {
        this.employees = employees; this.encoder = encoder; this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Employee emp = employees.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));
        if (!encoder.matches(req.password(), emp.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password");
        }
        String token = jwtService.generateToken(emp.getId(), emp.getRole().name());
        return new LoginResponse(token, emp);
    }
}
