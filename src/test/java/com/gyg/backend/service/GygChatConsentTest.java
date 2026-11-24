package com.gyg.backend.service;

import com.gyg.backend.dto.ChatRequest;
import com.gyg.backend.model.LogChat;
import com.gyg.backend.repository.LogChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GygChatConsentTest {

    @Mock
    private ChatEngine chatEngine;

    @Mock
    private LogChatRepository logChatRepository;

    @InjectMocks
    private GygService service;

    @BeforeEach
    void enableExternalProvider() {
        ReflectionTestUtils.setField(service, "geminiEnabled", true);
        ReflectionTestUtils.setField(service, "policyVersion", "policy-test-v2");
    }

    @Test
    void shouldRequireConsentBeforeCallingTheExternalProvider() {
        ChatRequest request = new ChatRequest("Consulta SST");

        assertThrows(ChatConsentRequiredException.class, () -> service.responderChat(request));
        verifyNoInteractions(chatEngine, logChatRepository);
    }

    @Test
    void shouldRecordConsentVersionAndTimestampAfterAProviderResponse() {
        ChatRequest request = new ChatRequest("Consulta SST");
        request.setAceptaEnvioProveedor(true);
        when(chatEngine.responder("Consulta SST")).thenReturn("Respuesta general");

        service.responderChat(request);

        ArgumentCaptor<LogChat> savedLog = ArgumentCaptor.forClass(LogChat.class);
        verify(logChatRepository).save(savedLog.capture());
        assertTrue(savedLog.getValue().isAceptaEnvioProveedor());
        assertEquals("policy-test-v2", savedLog.getValue().getVersionPolitica());
        assertNotNull(savedLog.getValue().getFechaConsentimiento());
        assertEquals(savedLog.getValue().getFecha(), savedLog.getValue().getFechaConsentimiento());
    }

    @Test
    void shouldPreserveTheConsentRecordWhenTheProviderFails() {
        ChatRequest request = new ChatRequest("Consulta SST");
        request.setAceptaEnvioProveedor(true);
        when(chatEngine.responder("Consulta SST")).thenThrow(new ChatProviderUnavailableException());

        assertThrows(ChatProviderUnavailableException.class, () -> service.responderChat(request));

        ArgumentCaptor<LogChat> savedLog = ArgumentCaptor.forClass(LogChat.class);
        verify(logChatRepository).save(savedLog.capture());
        assertTrue(savedLog.getValue().isAceptaEnvioProveedor());
        assertEquals("policy-test-v2", savedLog.getValue().getVersionPolitica());
    }
}
