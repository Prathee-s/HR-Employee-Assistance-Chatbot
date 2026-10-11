package com.hrassistant.controller;

import com.hrassistant.repository.HolidayRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {
    private final HolidayRepository holidays;

    public HolidayController(HolidayRepository holidays) { this.holidays = holidays; }

    @GetMapping
    public List<?> list(@RequestParam(required = false) Integer year) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        return holidays.findByYear(y);
    }
}
