package com.hrassistant.repository;

import com.hrassistant.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Attendance a WHERE a.employeeId = :employeeId " +
        "AND YEAR(a.date) = :year AND MONTH(a.date) = :month ORDER BY a.date DESC")
    List<Attendance> findByEmployeeAndMonth(Long employeeId, int year, int month);

    List<Attendance> findTop5ByEmployeeIdOrderByDateDesc(Long employeeId);
}
