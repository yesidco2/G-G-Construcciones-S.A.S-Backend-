package com.gyg.backend.dto;

import com.gyg.backend.model.LogChat;

import java.time.LocalDateTime;

public record ChatConsultaDTO(LocalDateTime fecha, String pregunta, String respuesta) {

    public static ChatConsultaDTO from(LogChat log) {
        return new ChatConsultaDTO(log.getFecha(), log.getPregunta(), log.getRespuesta());
    }
}
