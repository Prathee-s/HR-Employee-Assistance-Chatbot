package com.hrassistant.repository;

import com.hrassistant.model.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findAllByOrderByCreatedAtDesc();
}
