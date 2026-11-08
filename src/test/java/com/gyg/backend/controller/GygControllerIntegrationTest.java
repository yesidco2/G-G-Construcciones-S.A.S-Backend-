package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.dto.ChatRequest;
import com.gyg.backend.dto.ContactoRequest;
import com.gyg.backend.repository.SolicitudContactoRepository;
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
@ActiveProfiles({"test", "dev"})
class GygControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SolicitudContactoRepository solicitudContactoRepository;

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
                "Consultoría SST",
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
    void shouldRejectContactoWithInvalidFieldsAndTooManyLinks() throws Exception {
        assertContactoBadRequest(new ContactoRequest(
                "Ana 2", "Empresa", uniqueEmail(), "3001234567", "Obra Civil", "Necesito una cotizacion"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", uniqueEmail(), "3001234567", "Servicio ilegal", "Necesito una cotizacion"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", uniqueEmail(), "3001234567", "Obra Civil", "Corto"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", uniqueEmail(), "3001234567", "Obra Civil",
                "Necesito una <b>cotizacion</b> de obra"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", uniqueEmail(), "30012ABC", "Obra Civil", "Necesito una cotizacion"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", "ana@localhost", "3001234567", "Obra Civil", "Necesito una cotizacion"));
        assertContactoBadRequest(new ContactoRequest(
                "Ana Gomez", "Empresa", uniqueEmail(), "3001234567", "Obra Civil",
                "Cotizacion para https://uno.example http://dos.example www.tres.example"));
    }

    @Test
    void shouldCreateContactoWithoutPhone() throws Exception {
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez", "G&G Construcciones", uniqueEmail(), null,
                "Consultoría SST", "Necesito asesoría en seguridad");

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void shouldReturnCreatedWithZeroIdForHoneypotWithoutSaving() throws Exception {
        String correo = uniqueEmail();
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez", "G&G Construcciones", correo, null,
                "Consultoría SST", "Necesito asesoría en seguridad");
        request.setSitioWeb("https://spam.example");

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(0));

        org.junit.jupiter.api.Assertions.assertTrue(
                solicitudContactoRepository.findTopByCorreoOrderByFechaDesc(correo).isEmpty());
    }

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/contacto"));
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
        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"GG-2026-00125\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeAvance").value(74))
                .andExpect(jsonPath("$.fotos", hasSize(3)))
                .andExpect(jsonPath("$.hitos").isArray());

        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NO-EXISTE\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ab\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldApplyFailedTrackingCodeLimit() throws Exception {
        String request = "{\"codigo\":\"INVALID-TRACKING-CODE-234567890123\"}";
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/obras/seguimiento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isTooManyRequests());
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

    @Test
    void shouldRequireAuthenticationForOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    private void assertContactoBadRequest(ContactoRequest request) throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private String uniqueEmail() {
        return "contact-" + UUID.randomUUID() + "@example.com";
    }
}