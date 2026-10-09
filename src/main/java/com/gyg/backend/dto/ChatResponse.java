package com.gyg.backend.dto;

import java.time.LocalDateTime;

public class ChatResponse {
    private String pregunta;
    private String respuesta;
    private LocalDateTime timestamp;

    public ChatResponse() {
    }

    public ChatResponse(String pregunta, String respuesta, LocalDateTime timestamp) {
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.timestamp = timestamp;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
