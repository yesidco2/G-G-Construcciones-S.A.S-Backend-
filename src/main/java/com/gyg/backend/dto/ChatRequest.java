package com.gyg.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @NotBlank(message = "La pregunta es requerida")
    @Size(max = 500, message = "La pregunta no puede superar 500 caracteres")
    private String pregunta;

    public ChatRequest() {
    } 
    
    public ChatRequest(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }
}
