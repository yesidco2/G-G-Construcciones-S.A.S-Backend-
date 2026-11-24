package com.gyg.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.exception.ConflictException;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.*;

@Service
@Transactional(rollbackFor = IOException.class)
public class InduccionService {
    private static final String PENDIENTE = "PENDIENTE";
    private static final String CORRECCION = "CORRECCION";
    private static final String POR_REVISAR = "POR_REVISAR";
    private static final String VALIDADA = "VALIDADA";
    private static final long MAX_EVIDENCE_PIXELS = 16_000_000L;
    private static final int ATTEMPT_LIMIT = 5;
    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final PanelObrasService panel;
    private final ObraImageService photos;
    private final byte[] pinSecret;

    public InduccionService(JdbcTemplate db, ObjectMapper json, PanelObrasService panel,
            ObraImageService photos, @Value("${app.sst.pin-secret:${APP_SST_PIN_SECRET:}}") String pinSecret) {
        this.db = db;
        this.json = json;
        this.panel = panel;
        this.photos = photos;
        this.pinSecret = pinSecret.getBytes(StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> catalogo(long obra, Usuario actor) {
        panel.verificarAcceso(obra, actor);
        return rows("""
                SELECT m.id, m.titulo, v.version, v.ejemplo
                FROM apoyo.sst_material m JOIN apoyo.sst_version v ON m.id = v.material_id
                WHERE m.obra_id IS NULL OR m.obra_id = ?
                ORDER BY m.titulo, v.version DESC
                """, obra);
    }

    public Map<String, Object> publicar(long obra, Usuario actor, UUID material, String titulo, Contenido contenido)
            throws IOException {
        panel.verificarAcceso(obra, actor);
        if (titulo == null || titulo.isBlank() || titulo.length() > 150 || contenido == null) {
            throw new IllegalArgumentException("Indica el título y el contenido de la inducción.");
        }
        contenido.validar();
        int version = 1;
        if (material == null) {
            material = UUID.randomUUID();
            db.update("INSERT INTO apoyo.sst_material (id, obra_id, titulo) VALUES (?, ?, ?)", material, obra, titulo.strip());
        } else {
            var existing = find("SELECT titulo FROM apoyo.sst_material WHERE id = ? AND obra_id = ? FOR UPDATE", material, obra);
            if (!existing.get("titulo").equals(titulo.strip())) {
                throw new IllegalArgumentException("Conserva el título al publicar una versión del mismo material.");
            }
            version = db.queryForObject("SELECT max(version) + 1 FROM apoyo.sst_version WHERE material_id = ?", Integer.class, material);
        }
        db.update("INSERT INTO apoyo.sst_version VALUES (?, ?, false, ?)",
                material, version, json.writeValueAsString(contenido));
        return Map.of("id", material, "version", version);
    }

    public List<Map<String, Object>> asignaciones(long obra, Usuario actor) {
        panel.verificarAcceso(obra, actor);
        return rows("""
                SELECT a.id, m.titulo, a.version, a.creada_en, a.cerrada_en, v.ejemplo
                FROM apoyo.sst_asignacion a JOIN apoyo.sst_material m ON a.material_id = m.id
                JOIN apoyo.sst_version v ON v.material_id = a.material_id AND v.version = a.version
                WHERE a.obra_id = ? ORDER BY a.creada_en DESC
                """, obra);
    }

    public Map<String, Object> crear(long obra, Usuario actor, UUID material, int version, List<String> nombres) {
        panel.verificarAcceso(obra, actor);
        requireSecret();
        if (nombres == null || nombres.isEmpty() || nombres.size() > 500
                || nombres.stream().anyMatch(n -> n == null || n.isBlank() || n.length() > 150)) {
            throw new IllegalArgumentException("Registra entre 1 y 500 nombres de hasta 150 caracteres.");
        }
        find("""
                SELECT 1 FROM apoyo.sst_material m JOIN apoyo.sst_version v ON m.id = v.material_id
                WHERE m.id = ? AND v.version = ? AND (m.obra_id IS NULL OR m.obra_id = ?)
                """, material, version, obra);
        UUID assignment = UUID.randomUUID();
        db.update("INSERT INTO apoyo.sst_asignacion (id, obra_id, material_id, version) VALUES (?, ?, ?, ?)",
                assignment, obra, material, version);
        List<Map<String, Object>> pins = new ArrayList<>();
        int numero = 0;
        for (String entry : nombres) {
            String nombre = entry.strip();
            UUID worker = UUID.randomUUID();
            Pin pin = generatePin(assignment);
            db.update("""
                    INSERT INTO apoyo.sst_trabajador (id, asignacion_id, nombre, numero, pin_hash)
                    VALUES (?, ?, ?, ?, ?)
                    """, worker, assignment, nombre, ++numero, pin.hash());
            pins.add(Map.of("id", worker, "nombre", nombre, "numero", numero, "pin", pin.value()));
        }
        return Map.of("id", assignment, "pins", pins);
    }

    public void cerrar(long obra, Usuario actor, UUID assignment) {
        panelAssignment(obra, actor, assignment);
        db.update("UPDATE apoyo.sst_asignacion SET cerrada_en = coalesce(cerrada_en, now()) WHERE id = ?", assignment);
        db.update("UPDATE apoyo.sst_trabajador SET sesion_hash = NULL, sesion_hasta = NULL WHERE asignacion_id = ?", assignment);
    }

    public Map<String, String> regenerar(long obra, Usuario actor, UUID assignment, UUID worker) {
        requireOpen(panelAssignment(obra, actor, assignment));
        worker(assignment, worker);
        Pin pin = generatePin(assignment);
        db.update("""
                UPDATE apoyo.sst_trabajador SET pin_hash = ?, intentos = 0, ventana_hasta = NULL,
                    sesion_hash = NULL, sesion_hasta = NULL WHERE id = ?
                """, pin.hash(), worker);
        return Map.of("pin", pin.value());
    }

    public List<Map<String, Object>> cuadrilla(long obra, Usuario actor, UUID assignment) {
        panelAssignment(obra, actor, assignment);
        return rows("""
                SELECT id, nombre, numero, estado FROM apoyo.sst_trabajador WHERE asignacion_id = ? ORDER BY numero
                """, assignment);
    }

    public Map<String, Object> detalle(long obra, Usuario actor, UUID assignment, UUID worker) throws IOException {
        var asignacion = panelAssignment(obra, actor, assignment);
        var detalle = entrega(worker(assignment, worker), true);
        detalle.put("material", contenido(asignacion));
        detalle.put("historial", rows("""
                SELECT id, enviada_en, revisada_en, revisor_uid, decision, observacion
                FROM apoyo.sst_entrega WHERE trabajador_id = ? ORDER BY enviada_en DESC
                """, worker));
        return detalle;
    }

    public void revisar(long obra, Usuario actor, UUID assignment, UUID worker,
            UUID submission, boolean validar, String observacion) {
        panelAssignment(obra, actor, assignment);
        var row = worker(assignment, worker);
        String nota = observacion == null ? "" : observacion.strip();
        if (nota.length() > 1000 || (!validar && nota.isBlank())) {
            throw new IllegalArgumentException("Indica una observación de hasta 1000 caracteres al devolver.");
        }
        if (!POR_REVISAR.equals(row.get("estado"))
                || !Objects.equals(latest(worker).get("id"), submission)) {
            throw new ConflictException("La entrega cambió. Abre la versión actual.");
        }
        String decision = validar ? VALIDADA : CORRECCION;
        db.update("""
                UPDATE apoyo.sst_entrega SET revisada_en = now(), revisor_uid = ?, decision = ?, observacion = ?
                WHERE id = ? AND trabajador_id = ?
                """, actor.getSupabaseUid(), decision, nota.isBlank() ? null : nota, submission, worker);
        db.update("UPDATE apoyo.sst_trabajador SET estado = ? WHERE id = ?", decision, worker);
    }

    public byte[] foto(long obra, Usuario actor, UUID assignment, UUID worker, UUID submission) {
        panelAssignment(obra, actor, assignment);
        worker(assignment, worker);
        return (byte[]) find("SELECT foto FROM apoyo.sst_entrega WHERE id = ? AND trabajador_id = ?", submission, worker)
                .get("foto");
    }

    public Map<String, Object> nombres(UUID assignment, String search, int page) {
        publicAssignment(assignment);
        if (search.length() > 150 || page < 0) throw new IllegalArgumentException("Búsqueda inválida.");
        String query = normalize(search);
        var roster = rows("""
                SELECT id, nombre, numero FROM apoyo.sst_trabajador WHERE asignacion_id = ? ORDER BY nombre, numero
                """, assignment).stream()
                .filter(row -> normalize((String) row.get("nombre")).contains(query))
                .toList();
        return Map.of("total", roster.size(), "personas", roster.stream().skip(20L * page).limit(20).toList());
    }

    public Login autenticar(UUID assignment, UUID worker, String pin) {
        publicAssignment(assignment);
        requireSecret();
        var rows = rows("""
                SELECT pin_hash, CASE WHEN ventana_hasta > now() THEN intentos ELSE 0 END AS fallos
                FROM apoyo.sst_trabajador WHERE asignacion_id = ? AND id = ?
                """, assignment, worker);
        if (rows.isEmpty()) return new Login(null, false);
        var row = rows.get(0);
        int attempts = (int) row.get("fallos");
        if (attempts >= ATTEMPT_LIMIT) return new Login(null, true);
        if (pin == null || !pin.matches("[0-9]{6}")
                || !MessageDigest.isEqual(((String) row.get("pin_hash")).getBytes(StandardCharsets.US_ASCII),
                        hmac(assignment, pin).getBytes(StandardCharsets.US_ASCII))) {
            db.update("""
                    UPDATE apoyo.sst_trabajador SET intentos = ?,
                        ventana_hasta = CASE WHEN ventana_hasta > now() THEN ventana_hasta
                            ELSE now() + interval '15' minute END
                    WHERE id = ?
                    """, attempts + 1, worker);
            return new Login(null, attempts + 1 >= ATTEMPT_LIMIT);
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("""
                UPDATE apoyo.sst_trabajador SET sesion_hash = ?, sesion_hasta = now() + interval '4' hour,
                    intentos = 0, ventana_hasta = NULL WHERE id = ?
                """, digest(token), worker);
        return new Login(token, false);
    }

    public Map<String, Object> miEntrega(UUID assignment, String token) throws IOException {
        var asignacion = publicAssignment(assignment);
        var detalle = entrega(session(assignment, token), false);
        var contenido = contenido(asignacion);
        detalle.put("secciones", contenido.secciones());
        detalle.put("preguntas", contenido.preguntas().stream()
                .map(p -> Map.of("texto", p.texto(), "opciones", p.opciones(), "seccion", p.seccion())).toList());
        detalle.put("titulo", asignacion.get("titulo"));
        detalle.put("version", asignacion.get("version"));
        detalle.put("ejemplo", asignacion.get("ejemplo"));
        return detalle;
    }

    public Map<String, Object> responder(UUID assignment, String token, int pregunta, int opcion) throws IOException {
        var asignacion = publicAssignment(assignment);
        var row = session(assignment, token);
        var preguntas = contenido(asignacion).preguntas();
        if (pregunta < 0 || pregunta >= preguntas.size() || opcion < 0 || opcion >= preguntas.get(pregunta).opciones().size()) {
            throw new IllegalArgumentException("Pregunta u opción inválida.");
        }
        if (!PENDIENTE.equals(row.get("estado"))) throw new ConflictException("El cuestionario ya fue entregado.");
        var question = preguntas.get(pregunta);
        var existing = rows("""
                SELECT correcta FROM apoyo.sst_respuesta WHERE trabajador_id = ? AND pregunta = ?
                """, row.get("id"), pregunta);
        boolean correcta = opcion == question.correcta();
        if (existing.isEmpty()) {
            db.update("INSERT INTO apoyo.sst_respuesta (trabajador_id, pregunta, opcion, correcta) VALUES (?, ?, ?, ?)",
                    row.get("id"), pregunta, opcion, correcta);
        } else if (Boolean.FALSE.equals(existing.get(0).get("correcta"))) {
            db.update("""
                    UPDATE apoyo.sst_respuesta SET opcion = ?, correcta = ?, intentada_en = now()
                    WHERE trabajador_id = ? AND pregunta = ?
                    """, opcion, correcta, row.get("id"), pregunta);
        }
        return find("""
                SELECT pregunta, opcion, correcta FROM apoyo.sst_respuesta WHERE trabajador_id = ? AND pregunta = ?
                """, row.get("id"), pregunta);
    }

    public void enviar(UUID assignment, String token, UUID submission, MultipartFile file) throws IOException {
        var asignacion = publicAssignment(assignment);
        var row = session(assignment, token);
        UUID worker = (UUID) row.get("id");
        if (db.queryForObject("SELECT count(*) FROM apoyo.sst_entrega WHERE trabajador_id = ? AND id = ?",
                Long.class, worker, submission) > 0) return;
        if (!PENDIENTE.equals(row.get("estado")) && !CORRECCION.equals(row.get("estado"))) {
            throw new ConflictException("Ya existe una entrega para revisión.");
        }
        long correct = db.queryForObject("""
                SELECT count(*) FROM apoyo.sst_respuesta WHERE trabajador_id = ? AND correcta
                """, Long.class, worker);
        if (correct != contenido(asignacion).preguntas().size()) {
            throw new ConflictException("Completa el cuestionario antes de enviar la foto.");
        }
        byte[] sanitized = photos.sanitize(file, MAX_EVIDENCE_PIXELS);
        db.update("INSERT INTO apoyo.sst_entrega (id, trabajador_id, foto) VALUES (?, ?, ?)", submission, worker, sanitized);
        db.update("UPDATE apoyo.sst_trabajador SET estado = ? WHERE id = ?", POR_REVISAR, worker);
    }

    public byte[] miFoto(UUID assignment, String token) {
        publicAssignment(assignment);
        var row = session(assignment, token);
        return (byte[]) find("SELECT foto FROM apoyo.sst_entrega WHERE id = ? AND trabajador_id = ?",
                latest((UUID) row.get("id")).get("id"), row.get("id")).get("foto");
    }

    private Map<String, Object> entrega(Map<String, Object> worker, boolean includeReviewer) {
        var data = new LinkedHashMap<>(worker);
        UUID id = (UUID) worker.get("id");
        data.put("respuestas", rows("""
                SELECT pregunta, opcion, correcta, intentada_en FROM apoyo.sst_respuesta WHERE trabajador_id = ? ORDER BY pregunta
                """, id));
        var submission = new LinkedHashMap<>(latest(id));
        if (!includeReviewer) submission.remove("revisor_uid");
        data.put("entrega", submission);
        return data;
    }

    private Map<String, Object> latest(UUID worker) {
        return rows("""
                SELECT id, enviada_en, revisada_en, revisor_uid, decision, observacion
                FROM apoyo.sst_entrega WHERE trabajador_id = ? ORDER BY enviada_en DESC LIMIT 1
                """, worker).stream().findFirst().orElse(Map.of());
    }

    private Map<String, Object> panelAssignment(long obra, Usuario actor, UUID assignment) {
        panel.verificarAcceso(obra, actor);
        find("SELECT id FROM apoyo.sst_asignacion WHERE id = ? AND obra_id = ? FOR UPDATE", assignment, obra);
        return find("""
                SELECT a.*, v.contenido FROM apoyo.sst_asignacion a
                JOIN apoyo.sst_version v ON v.material_id = a.material_id AND v.version = a.version
                WHERE a.id = ?
                """, assignment);
    }

    private Map<String, Object> publicAssignment(UUID assignment) {
        // Lock the assignment in the same order as panel operations so closure and mutations serialize.
        requireOpen(find("SELECT cerrada_en FROM apoyo.sst_asignacion WHERE id = ? FOR UPDATE", assignment));
        var row = find("""
                SELECT a.cerrada_en, a.version, m.titulo, v.ejemplo, v.contenido
                FROM apoyo.sst_asignacion a JOIN apoyo.obras_ingenieros o ON o.id = a.obra_id
                JOIN apoyo.sst_material m ON m.id = a.material_id
                JOIN apoyo.sst_version v ON v.material_id = a.material_id AND v.version = a.version
                WHERE a.id = ? AND o.eliminada_en IS NULL
                """, assignment);
        return row;
    }

    private void requireOpen(Map<String, Object> assignment) {
        if (assignment.get("cerrada_en") != null) throw new ResourceNotFoundException("La asignación está cerrada.");
    }

    private Map<String, Object> worker(UUID assignment, UUID worker) {
        return find("""
                SELECT id, nombre, numero, estado FROM apoyo.sst_trabajador WHERE asignacion_id = ? AND id = ?
                """, assignment, worker);
    }

    private Map<String, Object> session(UUID assignment, String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidSession();
        return rows("""
                SELECT id, nombre, numero, estado FROM apoyo.sst_trabajador
                WHERE asignacion_id = ? AND sesion_hash = ? AND sesion_hasta > now()
                """, assignment, digest(token)).stream().findFirst().orElseThrow(this::invalidSession);
    }

    private AccessDeniedException invalidSession() {
        return new AccessDeniedException("La sesión de inducción venció. Ingresa tu PIN de nuevo.");
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return db.queryForList(sql, args).stream().<Map<String, Object>>map(row -> {
            var normalized = new LinkedHashMap<String, Object>();
            row.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value));
            return normalized;
        }).toList();
    }

    private Map<String, Object> find(String sql, Object... args) {
        return rows(sql, args).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el recurso de inducción."));
    }

    private Contenido contenido(Map<String, Object> assignment) throws IOException {
        return json.readValue((String) assignment.get("contenido"), Contenido.class);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private Pin generatePin(UUID assignment) {
        requireSecret();
        for (int i = 0; i < 100; i++) {
            String value = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            String hash = hmac(assignment, value);
            if (db.queryForObject("SELECT count(*) FROM apoyo.sst_pin_emitido WHERE asignacion_id = ? AND pin_hash = ?",
                    Long.class, assignment, hash) == 0) {
                db.update("INSERT INTO apoyo.sst_pin_emitido VALUES (?, ?)", assignment, hash);
                return new Pin(value, hash);
            }
        }
        throw new ConflictException("No fue posible reservar otro PIN para esta asignación.");
    }

    private void requireSecret() {
        if (pinSecret.length < 32) throw new IllegalStateException("Configura APP_SST_PIN_SECRET antes de usar inducciones.");
    }

    private String hmac(UUID assignment, String pin) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pinSecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((assignment + ":" + pin).getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Pin(String value, String hash) {}
    public record Login(String token, boolean bloqueado) {}
    public record Seccion(String titulo, String texto) {}
    public record Pregunta(String texto, List<String> opciones, Integer correcta, Integer seccion) {}
    public record Contenido(List<Seccion> secciones, List<Pregunta> preguntas) {
        void validar() {
            if (secciones == null || secciones.isEmpty() || secciones.size() > 12
                    || preguntas == null || preguntas.isEmpty() || preguntas.size() > 20
                    || secciones.stream().anyMatch(s -> s == null || invalid(s.titulo(), 150) || invalid(s.texto(), 1500))
                    || preguntas.stream().anyMatch(p -> p == null || invalid(p.texto(), 300) || p.opciones() == null
                            || p.opciones().size() < 2 || p.opciones().size() > 6
                            || p.opciones().stream().anyMatch(o -> invalid(o, 200))
                            || p.correcta() == null || p.correcta() < 0 || p.correcta() >= p.opciones().size()
                            || p.seccion() == null || p.seccion() < 0 || p.seccion() >= secciones.size())) {
                throw new IllegalArgumentException("Revisa las secciones, opciones y referencias del cuestionario.");
            }
        }

        private static boolean invalid(String value, int max) {
            return value == null || value.isBlank() || value.length() > max;
        }
    }
}
