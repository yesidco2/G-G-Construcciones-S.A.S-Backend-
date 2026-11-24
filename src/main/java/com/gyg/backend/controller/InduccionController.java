package com.gyg.backend.controller;

import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.InduccionService;
import com.gyg.backend.service.InduccionService.Contenido;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class InduccionController {
    private static final String PANEL = "/api/panel/obras/{obra}/inducciones";
    private static final String ASSIGNMENT = PANEL + "/asignaciones/{asignacion}";
    private static final String WORKER = ASSIGNMENT + "/trabajadores/{trabajador}";
    private static final String PUBLIC = "/api/inducciones/{asignacion}";
    private static final String TOKEN = "X-Induccion-Token";
    private final InduccionService service;

    public InduccionController(InduccionService service) {
        this.service = service;
    }

    @ModelAttribute
    void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping(PANEL + "/catalogo")
    List<Map<String, Object>> catalogo(@PathVariable long obra, @AuthenticationPrincipal Usuario actor) {
        return service.catalogo(obra, actor);
    }

    @PostMapping(PANEL + "/catalogo")
    Map<String, Object> publicar(@PathVariable long obra, @AuthenticationPrincipal Usuario actor,
            @RequestBody Publicar input) throws IOException {
        return service.publicar(obra, actor, input.material(), input.titulo(), input.contenido());
    }

    @GetMapping(PANEL + "/asignaciones")
    List<Map<String, Object>> asignaciones(@PathVariable long obra, @AuthenticationPrincipal Usuario actor) {
        return service.asignaciones(obra, actor);
    }

    @PostMapping(PANEL + "/asignaciones")
    Map<String, Object> crear(@PathVariable long obra, @AuthenticationPrincipal Usuario actor,
            @RequestBody Crear input) {
        return service.crear(obra, actor, input.material(), input.version(), input.nombres());
    }

    @PostMapping(ASSIGNMENT + "/cerrar")
    void cerrar(@PathVariable long obra, @PathVariable UUID asignacion, @AuthenticationPrincipal Usuario actor) {
        service.cerrar(obra, actor, asignacion);
    }

    @GetMapping(ASSIGNMENT + "/trabajadores")
    List<Map<String, Object>> cuadrilla(@PathVariable long obra, @PathVariable UUID asignacion,
            @AuthenticationPrincipal Usuario actor) {
        return service.cuadrilla(obra, actor, asignacion);
    }

    @PostMapping(WORKER + "/pin")
    Map<String, String> regenerar(@PathVariable long obra, @PathVariable UUID asignacion,
            @PathVariable UUID trabajador, @AuthenticationPrincipal Usuario actor) {
        return service.regenerar(obra, actor, asignacion, trabajador);
    }

    @GetMapping(WORKER)
    Map<String, Object> detalle(@PathVariable long obra, @PathVariable UUID asignacion,
            @PathVariable UUID trabajador, @AuthenticationPrincipal Usuario actor) throws IOException {
        return service.detalle(obra, actor, asignacion, trabajador);
    }

    @PostMapping(WORKER + "/revision")
    void revisar(@PathVariable long obra, @PathVariable UUID asignacion, @PathVariable UUID trabajador,
            @AuthenticationPrincipal Usuario actor, @RequestBody Revision input) {
        if (input.validar() == null) throw new IllegalArgumentException("Indica una decisión para la revisión.");
        service.revisar(obra, actor, asignacion, trabajador, input.entrega(), input.validar(), input.observacion());
    }

    @PostMapping(WORKER + "/foto")
    ResponseEntity<byte[]> foto(@PathVariable long obra, @PathVariable UUID asignacion, @PathVariable UUID trabajador,
            @AuthenticationPrincipal Usuario actor, @RequestBody Foto input) {
        return privateImage(service.foto(obra, actor, asignacion, trabajador, input.entrega()));
    }

    @GetMapping(PUBLIC + "/nombres")
    Map<String, Object> nombres(@PathVariable UUID asignacion,
            @RequestParam(defaultValue = "") String buscar, @RequestParam(defaultValue = "0") int pagina) {
        return service.nombres(asignacion, buscar, pagina);
    }

    @PostMapping(PUBLIC + "/acceso")
    ResponseEntity<Map<String, String>> autenticar(@PathVariable UUID asignacion, @RequestBody Acceso input) {
        var login = service.autenticar(asignacion, input.trabajador(), input.pin());
        if (login.bloqueado()) {
            return ResponseEntity.status(429).body(Map.of("message", "Demasiados intentos. Espera hasta 15 minutos."));
        }
        if (login.token() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "El PIN no corresponde al nombre seleccionado."));
        }
        return ResponseEntity.ok(Map.of("token", login.token()));
    }

    @GetMapping(PUBLIC + "/entrega")
    Map<String, Object> miEntrega(@PathVariable UUID asignacion,
            @RequestHeader(name = TOKEN, required = false) String token) throws IOException {
        return service.miEntrega(asignacion, token);
    }

    @PostMapping(PUBLIC + "/respuestas")
    Map<String, Object> responder(@PathVariable UUID asignacion,
            @RequestHeader(name = TOKEN, required = false) String token, @RequestBody Respuesta input) throws IOException {
        if (input.pregunta() == null || input.opcion() == null) {
            throw new IllegalArgumentException("Selecciona una respuesta.");
        }
        return service.responder(asignacion, token, input.pregunta(), input.opcion());
    }

    @PostMapping(path = PUBLIC + "/entrega", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void enviar(@PathVariable UUID asignacion, @RequestHeader(name = TOKEN, required = false) String token,
            @RequestParam UUID referencia, @RequestPart(value = "archivo", required = false) MultipartFile archivo)
            throws IOException {
        service.enviar(asignacion, token, referencia, archivo);
    }

    @GetMapping(PUBLIC + "/foto")
    ResponseEntity<byte[]> miFoto(@PathVariable UUID asignacion,
            @RequestHeader(name = TOKEN, required = false) String token) {
        return privateImage(service.miFoto(asignacion, token));
    }

    private ResponseEntity<byte[]> privateImage(byte[] photo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(photo);
    }

    public record Publicar(UUID material, String titulo, Contenido contenido) {}
    public record Crear(UUID material, int version, List<String> nombres) {}
    public record Revision(UUID entrega, Boolean validar, String observacion) {}
    public record Foto(UUID entrega) {}
    public record Acceso(UUID trabajador, String pin) {}
    public record Respuesta(Integer pregunta, Integer opcion) {}
}
