package com.gyg.backend.service;

import com.gyg.backend.dto.*;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.exception.TooManyRequestsException;
import com.gyg.backend.model.*;
import com.gyg.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GygService {

    private static final long CONTACT_COOLDOWN_MINUTES = 30;
    private static final Pattern CONTACT_LINK_PATTERN = Pattern.compile("(?i)(https?://|www\\.)");
    private static final Pattern TRACKING_CODE_PATTERN = Pattern.compile("^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{32}$");
    private static final Pattern LEGACY_TRACKING_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9-]{6,40}$");
    private static final String DUMMY_TRACKING_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private LaborRepository laborRepository;

    @Autowired
    private PoliticaAmbientalRepository politicaAmbientalRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private SolicitudContactoRepository solicitudContactoRepository;

    @Autowired
    private LogChatRepository logChatRepository;

    @Autowired
    private ChatEngine chatEngine;

    @Autowired
    private TrackingCodeService trackingCodeService;

    @Autowired
    private ContactCooldownLockService contactCooldownLockService;

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

        String nombre = limpiarTexto(request.getNombre());
        String correo = limpiarTexto(request.getCorreo());
        String mensaje = limpiarTexto(request.getMensaje());
        String empresa = limpiarTexto(request.getEmpresa());
        String telefono = limpiarTexto(request.getTelefono());

        if (nombre == null || nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre es requerido");
        }
        if (correo == null || correo.isEmpty()) {
            throw new IllegalArgumentException("El correo es requerido");
        }
        if (mensaje == null || mensaje.isEmpty()) {
            throw new IllegalArgumentException("El mensaje es requerido");
        }
        if (contarEnlaces(mensaje) > 2) {
            throw new IllegalArgumentException("El mensaje no puede contener mas de 2 enlaces");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedEmail = correo.toLowerCase(Locale.ROOT);
        contactCooldownLockService.lockByEmail(normalizedEmail);
        LocalDateTime lastContact = solicitudContactoRepository
                .findTopByCorreoOrderByFechaDesc(normalizedEmail)
                .map(SolicitudContacto::getFecha)
                .orElse(null);
        if (lastContact != null && lastContact.plusMinutes(CONTACT_COOLDOWN_MINUTES).isAfter(now)) {
            throw new TooManyRequestsException(
                    "Ya recibimos una solicitud con este correo. Intenta nuevamente en 30 minutos.");
        }

        String tipoServicio = request.getTipoServicio() == null || request.getTipoServicio().trim().isEmpty()
                ? "GENERAL"
                : request.getTipoServicio().trim().toUpperCase(Locale.ROOT);
        String nivelRiesgoSst = mapTipoServicioToNivelRiesgo(tipoServicio);

        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setNombre(nombre);
        solicitud.setEmpresa(empresa);
        solicitud.setCorreo(normalizedEmail);
        solicitud.setTelefono(telefono);
        solicitud.setTipoServicio(tipoServicio);
        solicitud.setNivelRiesgoSst(nivelRiesgoSst);
        solicitud.setMensaje(mensaje);
        solicitud.setFecha(now);

        SolicitudContacto savedSolicitud = solicitudContactoRepository.save(solicitud);
        return new ContactoResponse(savedSolicitud.getId(), nombre, correo, mensaje, now);
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

        LogChat logChat = new LogChat();
        logChat.setSessionId(UUID.randomUUID().toString());
        logChat.setPregunta(pregunta);
        logChat.setRespuesta(respuesta);
        logChat.setFecha(timestamp);
        logChatRepository.save(logChat);

        return new ChatResponse(pregunta, respuesta, timestamp);
    }

    // 6. GET /api/proyectos
    public List<ProyectoDTO> getProyectos(String categoria) {
        String categoriaFiltro = categoria == null ? null : categoria.trim().toUpperCase(Locale.ROOT);

        return obraRepository.findAllByEliminadaEnIsNullOrderByIdDesc().stream()
                .filter(obra -> categoriaFiltro == null || categoriaFiltro.equalsIgnoreCase(inferirCategoria(obra.getNombreObra())))
                .map(obra -> new ProyectoDTO(
                        obra.getId(),
                        obra.getNombreObra(),
                        obra.getCategoria() == null || obra.getCategoria().isBlank()
                                ? inferirCategoria(obra.getNombreObra()) : obra.getCategoria(),
                        construirDescripcion(obra.getNombreObra()),
                        construirMetricaDestacada(obra.getNombreObra()),
                        construirImagen(obra.getNombreObra())
                ))
                .collect(Collectors.toList());
    }

    // 7. POST /api/obras/seguimiento
    public SeguimientoObraDTO getSeguimientoProyecto(String codigo) {
        String candidate = codigo == null ? "" : codigo.substring(0, Math.min(codigo.length(), 128));
        String codeHash = trackingCodeService.hash(candidate);
        Optional<Obra> found = obraRepository.findByCodigoHashWithFotosAndHitos(codeHash);
        boolean codeMatches = trackingCodeService.matches(
                candidate, found.map(Obra::getCodigoHash).orElse(DUMMY_TRACKING_HASH));
        boolean validFormat = TRACKING_CODE_PATTERN.matcher(candidate).matches()
                || LEGACY_TRACKING_CODE_PATTERN.matcher(candidate).matches();
        if (found.isEmpty() || !validFormat || !codeMatches) {
            throw new ResourceNotFoundException("No se encontró el proyecto solicitado.");
        }
        Obra obra = found.get();

        List<FotoObraDTO> fotosObra = obra.getFotos().stream()
                .map(f -> new FotoObraDTO(f.getUrl(), f.getFecha()))
                .collect(Collectors.toList());
        List<SeguimientoHitoDTO> hitos = obra.getHitos().stream()
                .map(hito -> new SeguimientoHitoDTO(
                        hito.getNombre(), hito.getDescripcion(),
                        hito.getFechaObjetivo(), hito.isCompletado()))
                .toList();

        return new SeguimientoObraDTO(
                obra.getNombreObra(), obra.getPorcentajeAvance(), hitos, fotosObra);
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

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFC)
                .replaceAll("[\\p{Cntrl}&&[^\\n]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int contarEnlaces(String texto) {
        Matcher matcher = CONTACT_LINK_PATTERN.matcher(texto);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
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
