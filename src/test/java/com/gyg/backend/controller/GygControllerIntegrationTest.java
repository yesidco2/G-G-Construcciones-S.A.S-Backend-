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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GygControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRequireAuthenticationForProfileAndAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/autorizados"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "INGENIERO")
    void shouldDenyIngenieroFromAuthorizationManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/autorizados"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToListAuthorizationAndUsers() throws Exception {
        mockMvc.perform(get("/api/admin/autorizados"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreateContactoAndRejectRepeatedEmail() throws Exception {
        String correo = "integration-" + UUID.randomUUID() + "@example.com";
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez",
                "G&G Construcciones",
                correo,
                "3001234567",
                "SST",
                "Necesito una cotización urgente"
        );

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        request.setCorreo(correo.toUpperCase());
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "Ya recibimos una solicitud con este correo. Intenta nuevamente en 30 minutos."));
    }

    @Test
    void shouldRejectContactoWithInvalidEmail() throws Exception {
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez", "G&G Construcciones", "correo-invalido", "3001234567", "SST", "Solicitud"
        );

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateChatAndRejectEmptyQuestion() throws Exception {
        ChatRequest request = new ChatRequest("¿Qué normas de seguridad debo cumplir?");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListServices() throws Exception {
        mockMvc.perform(get("/api/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThan(0))));
    }

    @Test
    void shouldListCivilProjects() throws Exception {
        mockMvc.perform(get("/api/proyectos")
                        .param("categoria", "CIVIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void shouldReturnSeededProjectTrackingAndNotFoundForUnknownCode() throws Exception {
        mockMvc.perform(get("/api/obras/seguimiento/GG-2026-00125"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeAvance").value(74))
                .andExpect(jsonPath("$.fotos", hasSize(3)));

        mockMvc.perform(get("/api/obras/seguimiento/NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListRiskLaborAndReturnLaborDetails() throws Exception {
        mockMvc.perform(get("/api/riesgos/labores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThan(0))));

        mockMvc.perform(get("/api/riesgos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}