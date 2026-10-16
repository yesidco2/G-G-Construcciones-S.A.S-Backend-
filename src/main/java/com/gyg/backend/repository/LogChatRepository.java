package com.gyg.backend.repository;

import com.gyg.backend.model.LogChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogChatRepository extends JpaRepository<LogChat, Long> {
}
