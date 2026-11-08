package com.gyg.backend.repository;

import com.gyg.backend.model.LogChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LogChatRepository extends JpaRepository<LogChat, Long> {

    Page<LogChat> findAllByOrderByFechaDesc(Pageable pageable);

    @Modifying
    @Query("DELETE FROM LogChat log WHERE log.fecha < :cutoff")
    int deleteByFechaBefore(@Param("cutoff") LocalDateTime cutoff);
}
