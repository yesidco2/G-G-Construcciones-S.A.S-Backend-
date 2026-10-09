package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.dto.ChatRequest;
import com.gyg.backend.dto.ContactoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GygControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateContactoEndpoint() throws Exception {
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez",
                "G&G Construcciones",
                "ana@example.com",
                "3001234567",
                "SST",
                "Necesito una cotización urgente"
        );

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCreateChatEndpoint() throws Exception {
        ChatRequest request = new ChatRequest("¿Qué normas de seguridad debo cumplir?");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldListProyectosEndpoint() throws Exception {
        mockMvc.perform(get("/api/proyectos")
                        .param("categoria", "CIVIL"))
                .andExpect(status().isOk());
    }
}
