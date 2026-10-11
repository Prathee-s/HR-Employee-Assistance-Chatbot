package com.hrassistant.dto;

import com.hrassistant.model.Employee;

public record LoginResponse(String token, Employee employee) {}
