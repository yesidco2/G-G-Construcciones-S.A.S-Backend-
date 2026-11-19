package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.gyg.backend.dto.ChatRequest;
import com.gyg.backend.dto.ContactoRequest;
import com.gyg.backend.repository.NewsletterRepository;
import com.gyg.backend.dto.NewsletterRequest;
import com.gyg.backend.model.Obra;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.SolicitudContactoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import com.gyg.backend.service.TrackingCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class GygControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private SolicitudContactoRepository solicitudContactoRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrackingCodeService trackingCodeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldNeverSetCookiesOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/institucional"))
                .andExpect(status().isOk())
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

        mockMvc.perform(post("/api/auth/email-verification"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/autorizados"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldAcceptPasswordResetWithoutCaptchaToken() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-authorized@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldReportCaptchaConfigurationWhenVerificationIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/captcha/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"captchaToken\":\"token\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("CAPTCHA_CONFIGURATION_ERROR"));
        }

    @Test
    @WithMockUser(roles = "INGENIERO")
    void shouldDenyIngenieroFromAuthorizationManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/autorizados"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/panel/obras/clientes"))
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
    void shouldExposeAllowedWorkStatesToEngineers() throws Exception {
        mockMvc.perform(get("/api/panel/obras/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0]").value("PLANIFICADA"))
                .andExpect(jsonPath("$[3]").value("FINALIZADA"));
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
        mockMvc.perform(options("/api/newsletter")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Captcha-Token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("X-Captcha-Token")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void shouldAcceptPublicContactRequestsAndRejectUnknownPhotos() throws Exception {
        String email = uniqueEmail();
        ContactoRequest request = new ContactoRequest(
                "Ana Gomez", "G&G Construcciones", email, "3001234567",
                "Obra Civil", "Necesito una cotización de servicios");
        request.setAceptaPolitica(true);

        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(0));
        var savedRequest = solicitudContactoRepository.findTopByCorreoOrderByFechaDesc(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(savedRequest.isAceptaPolitica());
        org.junit.jupiter.api.Assertions.assertEquals("test-policy-v1", savedRequest.getVersionPolitica());

        mockMvc.perform(get("/api/contacto"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/obras/seguimiento/fotos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"codigo-invalido\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/panel/obras/1/fotos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDropLegacyPhotoTablesDuringMigrationAndKeepContactRequests() {
        Integer remainingPhotoTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                        + "AND TABLE_NAME IN ('FOTO_OBRA', 'FOTO_OBRAS')",
                Integer.class);

        org.junit.jupiter.api.Assertions.assertEquals(0, remainingPhotoTables);
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'SOLICITUDES_CONTACTO'",
                        Integer.class));
    }

    @Test
    void shouldReturnGenericResponsesForUnsupportedRequests() throws Exception {
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
    void shouldNotExposeReactStaticCatalogEndpoints() throws Exception {
        for (String endpoint : new String[] {
                "/api/servicios",
                "/api/proyectos",
                "/api/riesgos/labores",
                "/api/riesgos/1",
                "/api/politicas-ambientales"
        }) {
            mockMvc.perform(get(endpoint)).andExpect(status().isNotFound());
        }
    }

    @Test
    void shouldReturnSecurelyGeneratedProjectTrackingAndNotFoundForUnknownCode() throws Exception {
        TrackingCodeService.GeneratedTrackingCode generated = trackingCodeService.generate();
        var cliente = new Usuario();
        cliente.setFirebaseUid(UUID.randomUUID().toString());
        cliente.setRol(Rol.CLIENTE);
        cliente = usuarioRepository.saveAndFlush(cliente);
        Obra obra = new Obra("Viaducto Metropolitano", "EN_PROGRESO", cliente.getId(), 74);
        obra.setCodigoHash(generated.hash());
        obraRepository.saveAndFlush(obra);

        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "codigo", generated.value())))
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.221");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeAvance").value(74))
                .andExpect(jsonPath("$.hitos").isArray())
                .andExpect(jsonPath("$.codigoSeguimiento").doesNotExist());
        Obra recovered = obraRepository.findById(obra.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(generated.value(),
                trackingCodeService.decrypt(recovered.getCodigoCifrado(), recovered.getCodigoHash()));

        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"NO-EXISTE\"}")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.222");
                            return request;
                        }))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ab\"}")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.223");
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    void storesNewWorkAndPhotoAndServesItOnlyWithAuthorizationOrTrackingCode() throws Exception {
        Usuario cliente = new Usuario();
        cliente.setFirebaseUid(UUID.randomUUID().toString());
        cliente.setRol(Rol.CLIENTE);
        cliente.setEmail("obra-photo@example.com");
        cliente = usuarioRepository.saveAndFlush(cliente);

        Usuario admin = new Usuario();
        admin.setFirebaseUid("test-obras-admin");
        admin.setRol(Rol.ADMIN);
        var principal = authentication(new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        String work = mockMvc.perform(post("/api/panel/obras").with(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Obra nueva","clienteId":%d,"categoria":"CIVIL","estado":"PLANIFICADA",
                                 "progreso":15,"descripcion":"Compromiso SST","hitos":[{"nombre":"Inicio"}]}
                                """.formatted(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hitos[0].nombre").value("Inicio"))
                .andReturn().getResponse().getContentAsString();
        var created = objectMapper.readTree(work);
        long obraId = created.path("id").asLong();
        String codigo = created.path("codigoSeguimiento").asText();
        var png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", png);
        String uploaded = mockMvc.perform(multipart("/api/panel/obras/{id}/fotos", obraId)
                        .file(new MockMultipartFile("archivo", "avance.png", "image/png", png.toByteArray()))
                        .with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long fotoId = objectMapper.readTree(uploaded).path("id").asLong();
        String panelUrl = "/api/panel/obras/" + obraId + "/fotos/" + fotoId;
        String trackingUrl = "/api/obras/seguimiento/fotos/" + fotoId;
        mockMvc.perform(get(panelUrl).with(principal)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        mockMvc.perform(post(trackingUrl).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"codigo-invalido\"}")
                        .with(request -> { request.setRemoteAddr("192.0.2.231"); return request; }))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(trackingUrl).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("codigo", codigo))))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
        mockMvc.perform(delete(panelUrl).with(principal)).andExpect(status().isNoContent());
        mockMvc.perform(get(panelUrl).with(principal)).andExpect(status().isNotFound());
    }

    @Test
    void engineerCanCreateAndManageWorkForAClientWithoutAccount() throws Exception {
        Usuario engineer = new Usuario();
        engineer.setFirebaseUid(UUID.randomUUID().toString());
        engineer.setRol(Rol.INGENIERO);
        engineer = usuarioRepository.saveAndFlush(engineer);
        var principal = authentication(new UsernamePasswordAuthenticationToken(
                engineer, null, List.of(new SimpleGrantedAuthority("ROLE_INGENIERO"))));
        String payload = """
                {"nombre":"Obra de ingeniero","cliente":"  Cliente externo  ","categoria":"CIVIL",
                 "estado":"PLANIFICADA","progreso":15}
                """;
        var result = mockMvc.perform(post("/api/panel/obras").with(principal)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente").value("Cliente externo"))
                .andExpect(jsonPath("$.clienteId").doesNotExist())
                .andExpect(jsonPath("$.codigoSeguimiento").isString())
                .andReturn();
        var created = objectMapper.readTree(result.getResponse().getContentAsString());
        long id = created.path("id").asLong();
        String original = created.path("codigoSeguimiento").asText();
        var persisted = obraRepository.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(persisted.getClienteId());
        org.junit.jupiter.api.Assertions.assertTrue(persisted.estaAsignadaA(engineer.getId()));
        org.junit.jupiter.api.Assertions.assertNotEquals(original, persisted.getCodigoCifrado());
        mockMvc.perform(get("/api/panel/obras/{id}", id).with(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cliente").value("Cliente externo"))
                .andExpect(jsonPath("$.codigoSeguimiento").value(original));
        mockMvc.perform(get("/api/panel/obras").with(principal))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$[0].codigoSeguimiento").value(original))
                .andExpect(jsonPath("$[0].codigoCifrado").doesNotExist());
        mockMvc.perform(patch("/api/panel/obras/{id}", id).with(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cliente":"  Cliente actualizado  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Cliente actualizado"))
                .andExpect(jsonPath("$.codigoSeguimiento").value(original));
        mockMvc.perform(trackingRequest(original)).andExpect(status().isOk());

        Usuario other = new Usuario();
        other.setId(Long.MAX_VALUE);
        other.setRol(Rol.INGENIERO);
        var otherPrincipal = authentication(new UsernamePasswordAuthenticationToken(
                other, null, List.of(new SimpleGrantedAuthority("ROLE_INGENIERO"))));
        mockMvc.perform(post("/api/panel/obras/{id}/codigo-seguimiento", id).with(otherPrincipal))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/panel/obras/{id}", id).with(otherPrincipal))
                .andExpect(status().isForbidden());
        var renewed = mockMvc.perform(post("/api/panel/obras/{id}/codigo-seguimiento", id).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Cliente actualizado"))
                .andReturn();
        String nextCode = objectMapper.readTree(renewed.getResponse().getContentAsString())
                .path("codigoSeguimiento").asText();
        mockMvc.perform(get("/api/panel/obras/{id}", id).with(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigoSeguimiento").value(nextCode));
        mockMvc.perform(trackingRequest(original)).andExpect(status().isNotFound());
        mockMvc.perform(trackingRequest(nextCode)).andExpect(status().isOk());
        mockMvc.perform(post("/api/panel/obras").with(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Obra sin cliente","categoria":"CIVIL","estado":"PLANIFICADA","progreso":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trackingRequest(String code)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        return post("/api/obras/seguimiento")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("codigo", code)))
                .with(request -> { request.setRemoteAddr("192.0.2.242"); return request; });
    }

    @Test
    void shouldApplyFailedTrackingCodeLimit() throws Exception {
        String request = "{\"codigo\":\"INVALID-TRACKING-CODE-234567890123\"}";
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/obras/seguimiento")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
                            .with(mockRequest -> {
                                mockRequest.setRemoteAddr("192.0.2.224");
                                return mockRequest;
                            }))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/api/obras/seguimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .with(mockRequest -> {
                            mockRequest.setRemoteAddr("192.0.2.224");
                            return mockRequest;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void shouldRequireAuthenticationForOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthorizedRoleForPanelEndpoints() throws Exception {
        mockMvc.perform(get("/api/panel/obras"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/panel/obras/clientes"))
                .andExpect(status().isUnauthorized());
    }

    private String uniqueEmail() {
        return "integration-" + UUID.randomUUID() + "@example.com";
    }
}
