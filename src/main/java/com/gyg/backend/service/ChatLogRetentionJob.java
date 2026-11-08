package com.gyg.backend.service;

import com.gyg.backend.repository.LogChatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ChatLogRetentionJob {

    private final LogChatRepository repository;
    private final int retentionMonths;

    public ChatLogRetentionJob(
            LogChatRepository repository,
            @Value("${app.privacy.chat-retention-months:12}") int retentionMonths) {
        if (retentionMonths < 1) {
            throw new IllegalArgumentException("La retención de chat debe ser al menos un mes.");
        }
        this.repository = repository;
        this.retentionMonths = retentionMonths;
    }

    @Scheduled(cron = "${app.privacy.chat-retention-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredLogs() {
        repository.deleteByFechaBefore(LocalDateTime.now().minusMonths(retentionMonths));
    }
}
