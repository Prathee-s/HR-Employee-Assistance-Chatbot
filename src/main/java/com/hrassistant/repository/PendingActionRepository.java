package com.hrassistant.repository;

import com.hrassistant.model.PendingAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingActionRepository extends JpaRepository<PendingAction, Long> {
    Optional<PendingAction> findByEmployeeId(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}
