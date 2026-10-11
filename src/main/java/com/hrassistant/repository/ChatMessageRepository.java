package com.hrassistant.repository;

import com.hrassistant.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);
    List<ChatMessage> findTop8BySessionIdOrderByIdDesc(String sessionId);
}
