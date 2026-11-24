package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.model.Obra;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.ObraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.sst.pin-secret=induction-integration-test-secret-only")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class InduccionIntegrationTest {

    private static final String EXAMPLE = "1eb48aaa-c389-4c3c-8379-a0e092fc830e";
    private static final String TOKEN = "X-Induccion-Token";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ObraRepository obras;

    @Test
    void registraEntregaCorreccionPermisosYRevocacion() throws Exception {
        String panel = obra();
        mvc.perform(get(panel + "/asignaciones")).andExpect(status().isUnauthorized());
        var creado = create(panel, EXAMPLE, 1, List.of("Ana Gómez", "Ana Gómez", "Juan Pérez"));
        String id = creado.get("id").asText();
        String publicUrl = "/api/inducciones/" + id;
        String trabajadores = panel + "/asignaciones/" + id + "/trabajadores";
        var ana = creado.get("pins").get(0);
        var segundaAna = creado.get("pins").get(1);
        String worker = ana.get("id").asText();
        String other = segundaAna.get("id").asText();
        String pin = ana.get("pin").asText();
        String otherPin = segundaAna.get("pin").asText();
        assertNotEquals(pin, otherPin);
        mvc.perform(get(publicUrl + "/nombres").param("buscar", "gomez"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.personas[0].pin").doesNotExist())
                .andExpect(jsonPath("$.personas[0].estado").doesNotExist())
                .andExpect(jsonPath("$.personas[0].numero").value(1))
                .andExpect(jsonPath("$.personas[1].numero").value(2));
        acceso(publicUrl, worker, otherPin).andExpect(status().isUnauthorized());
        String tokenViejo = read(acceso(publicUrl, worker, pin).andExpect(status().isOk())).get("token").asText();
        String nuevo = read(mvc.perform(post(trabajadores + "/" + worker + "/pin").with(actor(Rol.ADMIN)))
                .andExpect(status().isOk())).get("pin").asText();
        acceso(publicUrl, worker, pin).andExpect(status().isUnauthorized());
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, tokenViejo)).andExpect(status().isForbidden());
        String token = read(acceso(publicUrl, worker, nuevo).andExpect(status().isOk())).get("token").asText();
        String otherToken = read(acceso(publicUrl, other, otherPin).andExpect(status().isOk())).get("token").asText();
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, token))
                .andExpect(jsonPath("$.preguntas[0].correcta").doesNotExist());
        byte[] png = image();
        String first = UUID.randomUUID().toString();
        enviar(publicUrl, token, first, png).andExpect(status().isConflict());

        responder(publicUrl, token, 0, 1).andExpect(jsonPath("$.correcta").value(false));
        responder(publicUrl, token, 1, 1).andExpect(jsonPath("$.correcta").value(true));
        responder(publicUrl, token, 0, 0).andExpect(jsonPath("$.correcta").value(true));
        responder(publicUrl, token, 1, 0).andExpect(jsonPath("$.opcion").value(1));
        responder(publicUrl, token, 2, 0).andExpect(jsonPath("$.correcta").value(true));
        mvc.perform(multipart(publicUrl + "/entrega").param("referencia", first).header(TOKEN, token))
                .andExpect(status().isBadRequest());
        enviar(publicUrl, token, first, new byte[] {1, 2, 3}).andExpect(status().isBadRequest());
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, token))
                .andExpect(jsonPath("$.respuestas.length()").value(3));
        enviar(publicUrl, token, first, png).andExpect(status().isOk());
        enviar(publicUrl, token, first, png).andExpect(status().isOk());
        mvc.perform(get(publicUrl + "/foto").header(TOKEN, otherToken)).andExpect(status().isNotFound());
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, otherToken))
                .andExpect(jsonPath("$.respuestas.length()").value(0));
        mvc.perform(get(publicUrl + "/foto").header(TOKEN, token)).andExpect(content().contentType(MediaType.IMAGE_PNG));
        String detalle = trabajadores + "/" + worker;
        mvc.perform(get(detalle).with(actor(Rol.INGENIERO))).andExpect(status().isForbidden());
        postJson(detalle + "/foto", Map.of("entrega", first), actor(Rol.INGENIERO)).andExpect(status().isForbidden());
        postJson(detalle + "/foto", Map.of("entrega", first), actor(Rol.ADMIN))
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        mvc.perform(get(trabajadores).with(actor(Rol.ADMIN))).andExpect(jsonPath("$[0].estado").value("POR_REVISAR"));
        postJson(detalle + "/revision", Map.of("entrega", first, "validar", false, "observacion", "Mejora el encuadre."),
                actor(Rol.ADMIN)).andExpect(status().isOk());
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, token))
                .andExpect(jsonPath("$.estado").value("CORRECCION"))
                .andExpect(jsonPath("$.entrega.observacion").value("Mejora el encuadre."));
        String second = UUID.randomUUID().toString();
        enviar(publicUrl, token, second, png).andExpect(status().isOk());
        postJson(detalle + "/revision", Map.of("entrega", first, "validar", true), actor(Rol.ADMIN))
                .andExpect(status().isConflict());
        postJson(detalle + "/revision", Map.of("entrega", second, "validar", true), actor(Rol.ADMIN))
                .andExpect(status().isOk());
        mvc.perform(get(detalle).with(actor(Rol.ADMIN)))
                .andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.historial[0].revisor_uid").value("test-ADMIN"))
                .andExpect(jsonPath("$.estado").value("VALIDADA"));
        mvc.perform(post(panel + "/asignaciones/" + id + "/cerrar").with(actor(Rol.ADMIN))).andExpect(status().isOk());
        mvc.perform(get(publicUrl + "/nombres")).andExpect(status().isNotFound());
        enviar(publicUrl, token, second, png).andExpect(status().isNotFound());
        responder(publicUrl, token, 2, 0).andExpect(status().isNotFound());
    }

    @Test
    void bloqueaIntentosYConservaVersionAsignada() throws Exception {
        String panel = obra();
        var secciones = List.of(Map.of("titulo", "Primera sección", "texto", "Contenido original"));
        var preguntas = List.of(Map.of("texto", "Pregunta", "opciones", List.of("Opción A", "Opción B"),
                "correcta", 0, "seccion", 0));
        var material = read(postJson(panel + "/catalogo", Map.of("titulo", "Inducción nueva",
                "contenido", Map.of("secciones", secciones, "preguntas", preguntas)), actor(Rol.ADMIN))
                .andExpect(status().isOk())).get("id").asText();
        var creado = create(panel, material, 1, List.of("Operario"));
        postJson(panel + "/catalogo", Map.of("material", material, "titulo", "Inducción nueva",
                "contenido", Map.of("secciones", List.of(Map.of("titulo", "Versión nueva", "texto", "Texto nuevo")),
                        "preguntas", preguntas)), actor(Rol.ADMIN)).andExpect(jsonPath("$.version").value(2));
        String publicUrl = "/api/inducciones/" + creado.get("id").asText();
        var person = creado.get("pins").get(0);
        String worker = person.get("id").asText();
        String pin = person.get("pin").asText();
        String wrong = pin.equals("000000") ? "999999" : "000000";
        for (int i = 0; i < 4; i++) acceso(publicUrl, worker, wrong).andExpect(status().isUnauthorized());
        acceso(publicUrl, worker, wrong).andExpect(status().isTooManyRequests());
        acceso(publicUrl, worker, pin).andExpect(status().isTooManyRequests());
        String path = panel + "/asignaciones/" + creado.get("id").asText() + "/trabajadores/" + worker + "/pin";
        String renewed = read(mvc.perform(post(path).with(actor(Rol.ADMIN)))
                .andExpect(status().isOk())).get("pin").asText();
        String token = read(acceso(publicUrl, worker, renewed).andExpect(status().isOk())).get("token").asText();
        mvc.perform(get(publicUrl + "/entrega").header(TOKEN, token))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.secciones[0].texto").value("Contenido original"));
    }

    private JsonNode create(String panel, String material, int version, List<String> names) throws Exception {
        return read(postJson(panel + "/asignaciones", Map.of("material", material, "version", version, "nombres", names),
                actor(Rol.ADMIN)).andExpect(status().isOk()));
    }

    private String obra() {
        var obra = new Obra("Obra SST de pruebas", "EN_PROGRESO", 1L);
        obra.setCodigoHash(UUID.randomUUID().toString());
        return "/api/panel/obras/" + obras.saveAndFlush(obra).getId() + "/inducciones";
    }

    private RequestPostProcessor actor(Rol role) {
        var usuario = new Usuario();
        usuario.setRol(role);
        usuario.setSupabaseUid("test-" + role);
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private ResultActions postJson(String path, Object payload, RequestPostProcessor actor) throws Exception {
        return mvc.perform(post(path).with(actor).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(payload)));
    }

    private ResultActions acceso(String path, String worker, String pin) throws Exception {
        return mvc.perform(post(path + "/acceso").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("trabajador", worker, "pin", pin))));
    }

    private ResultActions responder(String path, String token, int question, int option) throws Exception {
        return mvc.perform(post(path + "/respuestas").header(TOKEN, token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("pregunta", question, "opcion", option))));
    }

    private ResultActions enviar(String path, String token, String id, byte[] bytes) throws Exception {
        return mvc.perform(multipart(path + "/entrega")
                .file(new MockMultipartFile("archivo", "epp.png", "image/png", bytes))
                .param("referencia", id).header(TOKEN, token));
    }

    private JsonNode read(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsByteArray());
    }

    private byte[] image() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
