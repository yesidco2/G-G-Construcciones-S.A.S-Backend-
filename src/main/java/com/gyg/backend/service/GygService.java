package com.gyg.backend.service;

import com.gyg.backend.dto.*;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.exception.TooManyRequestsException;
import com.gyg.backend.model.*;
import com.gyg.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GygService {

    private static final long CONTACT_COOLDOWN_SECONDS = 30 * 60;
    private final Map<String, LocalDateTime> lastContactByEmail = new ConcurrentHashMap<>();

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private LaborRepository laborRepository;

    @Autowired
    private PoliticaAmbientalRepository politicaAmbientalRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ChatEngine chatEngine;

    // 1. GET /api/institucional
    public InstitucionalDTO getInstitucional() {
        return new InstitucionalDTO(
                "Proveer soluciones integrales de ingenieria civil y consultoria en SST con altos estandares de calidad, seguridad y sostenibilidad, aportando al desarrollo del pais y al bienestar de los trabajadores.",
                "Ser en el 2030 la empresa lider en integracion de servicios de ingenieria y consultoria de seguridad laboral, reconocida por su innovacion, confiabilidad y compromiso con la vida.",
                Arrays.asList("Seguridad y Salud en el Trabajo", "Integridad", "Calidad Excepcional", "Sostenibilidad", "Compromiso")
        );
    }

    // 2. GET /api/servicios
    public List<ServicioDTO> getServicios() {
        return servicioRepository.findAll().stream()
                .map(s -> new ServicioDTO(s.getId(), s.getTitulo(), s.getDescripcion(), s.getBloque()))
                .collect(Collectors.toList());
    }

    // 3. GET /api/datos-legales
    public DatosLegalesDTO getDatosLegales() {
        return new DatosLegalesDTO(
                "901.458.322-1",
                "Calle 100 # 15-30, Oficina 502, Bogota, Colombia"
        );
    }

    // 4. POST /api/contacto
    @Transactional
    public ContactoResponse crearContacto(ContactoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de contacto no puede ser nula");
        }

        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        String correo = request.getCorreo() == null ? "" : request.getCorreo().trim();
        String mensaje = request.getMensaje() == null ? "" : request.getMensaje().trim();

        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre es requerido");
        }
        if (correo.isEmpty()) {
            throw new IllegalArgumentException("El correo es requerido");
        }
        if (mensaje.isEmpty()) {
            throw new IllegalArgumentException("El mensaje es requerido");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedEmail = correo.toLowerCase(Locale.ROOT);
        synchronized (lastContactByEmail) {
            LocalDateTime lastContact = findLastContactByEmail(normalizedEmail);
            if (lastContact == null) {
                lastContact = lastContactByEmail.get(normalizedEmail);
            }
            if (lastContact != null && lastContact.plusSeconds(CONTACT_COOLDOWN_SECONDS).isAfter(now)) {
                throw new TooManyRequestsException(
                        "Ya recibimos una solicitud con este correo. Intenta nuevamente en 30 minutos.");
            }
            lastContactByEmail.put(normalizedEmail, now);
        }

        String tipoServicio = request.getTipoServicio() == null || request.getTipoServicio().trim().isEmpty()
                ? "GENERAL"
                : request.getTipoServicio().trim().toUpperCase(Locale.ROOT);
        String nivelRiesgoSst = mapTipoServicioToNivelRiesgo(tipoServicio);

        LocalDateTime timestamp = now;
        String recomendaciones = "nombre=" + nombre + "; empresa=" + (request.getEmpresa() == null ? "" : request.getEmpresa())
                + "; telefono=" + (request.getTelefono() == null ? "" : request.getTelefono())
            + "; correo=" + normalizedEmail
                + "; tipoServicio=" + tipoServicio + "; mensaje=" + mensaje;

        jdbcTemplate.update(
                "INSERT INTO cotizaciones (usuario_id, metros_cuadrados, nivel_riesgo_sst, subtotal, iva, total, recomendaciones_ia, fecha_cotizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                java.math.BigDecimal.ZERO,
                nivelRiesgoSst,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                recomendaciones,
                java.sql.Timestamp.valueOf(timestamp)
        );

        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM cotizaciones", Long.class);
        return new ContactoResponse(id, nombre, correo, mensaje, timestamp);
    }

    private LocalDateTime findLastContactByEmail(String normalizedEmail) {
        String pattern = "%correo=" + normalizedEmail + ";%";
        return jdbcTemplate.query(
                "SELECT MAX(fecha_cotizacion) FROM cotizaciones WHERE LOWER(recomendaciones_ia) LIKE LOWER(?)",
                resultSet -> {
                    if (!resultSet.next() || resultSet.getTimestamp(1) == null) {
                        return null;
                    }
                    return resultSet.getTimestamp(1).toLocalDateTime();
                },
                pattern
        );
    }

    // 5. POST /api/chat
    @Transactional
    public ChatResponse responderChat(ChatRequest request) {
        if (request == null || request.getPregunta() == null || request.getPregunta().trim().isEmpty()) {
            throw new IllegalArgumentException("La pregunta es requerida");
        }

        String pregunta = request.getPregunta().trim();
        String respuesta = chatEngine.responder(pregunta);
        LocalDateTime timestamp = LocalDateTime.now();

        jdbcTemplate.update(
                "INSERT INTO logs_chat_ia (session_id, pregunta_usuario, respuesta_ia, fecha_interaccion) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                pregunta,
                respuesta,
                java.sql.Timestamp.valueOf(timestamp)
        );

        return new ChatResponse(pregunta, respuesta, timestamp);
    }

    // 6. GET /api/proyectos
    public List<ProyectoDTO> getProyectos(String categoria) {
        String categoriaFiltro = categoria == null ? null : categoria.trim().toUpperCase(Locale.ROOT);

        return obraRepository.findAll().stream()
                .filter(obra -> categoriaFiltro == null || categoriaFiltro.equalsIgnoreCase(inferirCategoria(obra.getNombreObra())))
                .map(obra -> new ProyectoDTO(
                        obra.getId(),
                        obra.getNombreObra(),
                        inferirCategoria(obra.getNombreObra()),
                        construirDescripcion(obra.getNombreObra()),
                        construirMetricaDestacada(obra.getNombreObra()),
                        construirImagen(obra.getNombreObra())
                ))
                .collect(Collectors.toList());
    }

    // 7. GET /api/obras/seguimiento/{codigo}
    public SeguimientoObraDTO getSeguimientoProyecto(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El codigo de acceso no puede estar vacio");
        }

        Obra obra = obraRepository.findByCodigoSeguimientoWithFotos(codigo.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Codigo de seguimiento invalido o no registrado"));

        List<FotoObraDTO> fotosObra = obra.getFotos().stream()
                .map(f -> new FotoObraDTO(f.getUrl(), f.getFecha()))
                .collect(Collectors.toList());

        return new SeguimientoObraDTO(obra.getNombreObra(), obra.getPorcentajeAvance(), fotosObra);
    }

    // 8. GET /api/riesgos/labores
    public List<LaborDTO> getLabores() {
        return laborRepository.findAll().stream()
                .map(l -> new LaborDTO(l.getId(), l.getNombre()))
                .collect(Collectors.toList());
    }

    // 9. GET /api/riesgos/{laborId}
    public LaborDetalleDTO getLaborDetalle(Long laborId) {
        Labor labor = laborRepository.findById(laborId)
                .orElseThrow(() -> new ResourceNotFoundException("Labor no encontrada con ID: " + laborId));

        List<RiesgoDTO> riesgosDTO = labor.getRiesgos().stream()
                .map(r -> new RiesgoDTO(r.getId(), r.getDescripcion(), r.getNivelSeveridad()))
                .collect(Collectors.toList());

        List<EppDTO> eppsDTO = labor.getEpps().stream()
                .map(e -> new EppDTO(e.getId(), e.getNombre(), e.getTipo()))
                .collect(Collectors.toList());

        return new LaborDetalleDTO(
                labor.getId(),
                labor.getNombre(),
                riesgosDTO,
                eppsDTO
        );
    }

    // 10. GET /api/politicas-ambientales
    public List<PoliticaAmbientalDTO> getPoliticasAmbientales() {
        return politicaAmbientalRepository.findAll().stream()
                .map(p -> new PoliticaAmbientalDTO(p.getId(), p.getTitulo(), p.getDescripcion()))
                .collect(Collectors.toList());
    }

    private String mapTipoServicioToNivelRiesgo(String tipoServicio) {
        if (tipoServicio == null) {
            return "BAJO";
        }
        String normalized = tipoServicio.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("SST") || normalized.contains("SEGURIDAD") || normalized.contains("SALUD")) {
            return "MEDIO";
        }
        if (normalized.contains("CIVIL") || normalized.contains("ESTRUCTURA") || normalized.contains("OBRA")) {
            return "ALTO";
        }
        return "BAJO";
    }

    private String inferirCategoria(String nombreObra) {
        if (nombreObra == null) {
            return "CIVIL";
        }
        String nombre = nombreObra.toUpperCase(Locale.ROOT);
        if (nombre.contains("SST") || nombre.contains("SEGURIDAD") || nombre.contains("SALUD") || nombre.contains("HSEQ")) {
            return "SST";
        }
        return "CIVIL";
    }

    private String construirDescripcion(String nombreObra) {
        if (nombreObra == null) {
            return "Proyecto de infraestructura y consultoria con enfoque de seguridad, calidad y sostenibilidad.";
        }
        String categoria = inferirCategoria(nombreObra);
        if ("SST".equals(categoria)) {
            return "Programa de acompañamiento y fortalecimiento de seguridad y salud en el trabajo para la operación del proyecto.";
        }
        return "Proyecto de infraestructura y obra civil con control de calidad, planificación y ejecución técnica.";
    }

    private String construirMetricaDestacada(String nombreObra) {
        if ("SST".equals(inferirCategoria(nombreObra))) {
            return "Cumplimiento y prevención";
        }
        return "Ejecución y calidad";
    }

    private String construirImagen(String nombreObra) {
        if ("SST".equals(inferirCategoria(nombreObra))) {
            return "https://images.unsplash.com/photo-1581092921461-eab62e97a2d6?auto=format&fit=crop&w=900&q=80";
        }
        return "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=900&q=80";
    }

}
