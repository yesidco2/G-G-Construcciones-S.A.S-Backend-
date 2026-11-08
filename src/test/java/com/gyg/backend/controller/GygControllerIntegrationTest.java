package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.dto.ChatRequest;
import com.gyg.backend.dto.ContactoRequest;
import com.gyg.backend.repository.SolicitudContactoRepository;
import com.gyg.backend.repository.NewsletterRepository;
import com.gyg.backend.dto.NewsletterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Test
    void shouldNeverSetCookiesOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/servicios"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));

        ContactoRequest contact = new ContactoRequest(
                "Ana Gomez",
                "G&G Construcciones",
                uniqueEmail(),
                "",
                "Consultoría SST",
                "Necesito una cotización de servicios");
        contact.setAceptaPolitica(true);
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contact)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

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
    @WithMockUser(roles = "INGENIERO")
    void shouldDenyEngineerFromChatConsultationEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/consultas-chat"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToReadPagedChatConsultations() throws Exception {
        mockMvc.perform(get("/api/admin/consultas-chat").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10));
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
        request.setAceptaPolitica(true);

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(0));
        var savedContact = solicitudContactoRepository.findTopByCorreoOrderByFechaDesc(correo)
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(savedContact.isAceptaPolitica());
        org.junit.jupiter.api.Assertions.assertEquals("test-policy-v1", savedContact.getVersionPolitica());
        org.junit.jupiter.api.Assertions.assertNotNull(savedContact.getFechaConsentimiento());

        request.setCorreo(correo.toUpperCase());
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "Ya recibimos una solicitud con este correo. Intenta nuevamente en 30 minutos."));
    }

    @Test
    void shouldEnforceContactCooldownForConcurrentRequests() throws Exception {
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez", "G&G Construcciones", uniqueEmail(), null,
                "Consultoría SST", "Necesito asesoría en seguridad");
        request.setAceptaPolitica(true);
        String body = objectMapper.writeValueAsString(request);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(() -> mockMvc.perform(post("/api/contacto")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn());
            Future<MvcResult> second = executor.submit(() -> mockMvc.perform(post("/api/contacto")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn());

            int firstStatus = first.get().getResponse().getStatus();
            int secondStatus = second.get().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertEquals(
                    1, (firstStatus == 201 ? 1 : 0) + (secondStatus == 201 ? 1 : 0));
            org.junit.jupiter.api.Assertions.assertEquals(
                    1, (firstStatus == 429 ? 1 : 0) + (secondStatus == 429 ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
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
    void shouldRequireExplicitPrivacyConsentForContacto() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Gomez\",\"empresa\":\"Empresa\","
                                + "\"correo\":\"" + uniqueEmail() + "\",\"telefono\":\"\","
                                + "\"tipoServicio\":\"Obra Civil\","
                                + "\"mensaje\":\"Necesito una cotizacion\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldStoreNewsletterConsentAndRequireIt() throws Exception {
        String email = uniqueEmail();
        NewsletterRequest request = new NewsletterRequest();
        request.setCorreo(email);
        request.setAceptaPolitica(true);

        mockMvc.perform(post("/api/newsletter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        var saved = newsletterRepository.findByCorreo(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(saved.isAceptaPolitica());
        org.junit.jupiter.api.Assertions.assertEquals("test-policy-v1", saved.getVersionPolitica());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getFechaConsentimiento());

        mockMvc.perform(post("/api/newsletter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + uniqueEmail() + "\"}"))
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
        request.setAceptaPolitica(true);

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
        request.setAceptaPolitica(true);
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
    void shouldRejectUnknownDtoProperties() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Gomez\",\"empresa\":\"Empresa\","
                                + "\"correo\":\"" + uniqueEmail() + "\",\"telefono\":\"\","
                                + "\"tipoServicio\":\"Obra Civil\",\"mensaje\":\"Necesito cotizacion\","
                                + "\"aceptaPolitica\":true,\"id\":123}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no tiene un formato valido."));
    }

    @Test
    void shouldRejectRequestBodiesAboveConfiguredLimit() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[1024 * 1024 + 1]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud supera el tamaño permitido."));
    }

    @Test
    void shouldSetRestrictiveSecurityHeadersOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/institucional").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"));
    }

    @Test
    void shouldAllowCaptchaHeaderInCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/contacto")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Captcha-Token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("X-Captcha-Token")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void shouldValidateQueryAndPathParameters() throws Exception {
        mockMvc.perform(get("/api/proyectos").param("categoria", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos no válidos."));

        mockMvc.perform(get("/api/riesgos/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos no válidos."));
    }

    @Test
    void shouldReturnGenericResponsesForUnsupportedRequests() throws Exception {
        mockMvc.perform(get("/api/contacto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("El metodo HTTP no esta permitido."));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("pregunta"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value(
                        "El tipo de contenido de la solicitud no esta soportado."));

        mockMvc.perform(get("/api/recurso-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No se encontro el recurso solicitado."));
    }

    @Test
    void shouldReturnGenericMessagesForServiceErrors() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Gomez\",\"empresa\":\"Empresa\","
                                + "\"correo\":\"" + uniqueEmail() + "\",\"telefono\":\"\","
                                + "\"tipoServicio\":\"Obra Civil\","
                                + "\"mensaje\":\"https://uno.example https://dos.example www.tres.example\","
                                + "\"aceptaPolitica\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos no válidos."));
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
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
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