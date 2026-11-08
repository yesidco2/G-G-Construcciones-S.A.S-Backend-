package com.gyg.backend.service;

import com.gyg.backend.repository.LogChatRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatLogRetentionJobTest {

    @Test
    void shouldDeleteLogsOlderThanConfiguredRetentionPeriod() {
        LogChatRepository repository = mock(LogChatRepository.class);
        ChatLogRetentionJob job = new ChatLogRetentionJob(repository, 12);
        LocalDateTime beforeRun = LocalDateTime.now();

        job.purgeExpiredLogs();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByFechaBefore(cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(!cutoff.isBefore(beforeRun.minusMonths(12)));
        assertTrue(!cutoff.isAfter(LocalDateTime.now().minusMonths(12)));
    }

    @Test
    void shouldRejectInvalidRetentionPeriod() {
        boolean rejected = false;
        try {
            new ChatLogRetentionJob(mock(LogChatRepository.class), 0);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected);
    }
}
