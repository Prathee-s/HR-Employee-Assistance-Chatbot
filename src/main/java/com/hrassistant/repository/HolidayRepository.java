package com.hrassistant.repository;

import com.hrassistant.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    @org.springframework.data.jpa.repository.Query(
        "SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date ASC")
    List<Holiday> findByYear(int year);
}
