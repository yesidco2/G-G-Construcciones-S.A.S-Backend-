package com.gyg.backend.service;

import com.gyg.backend.dto.panel.*;
import com.gyg.backend.dto.FotoObraDTO;
import com.gyg.backend.exception.ConflictException;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.*;
import com.gyg.backend.repository.AuditoriaEventoRepository;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;
import java.util.Objects;

@Service
public class PanelObrasService {

    private final ObraRepository obraRepository;
    private final ObraImageService imageService;
    private final FotosObraService fotosObraService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaEventoRepository auditRepository;
    private final TrackingCodeService trackingCodeService;

    public PanelObrasService(
            ObraRepository obraRepository,
            ObraImageService imageService,
            FotosObraService fotosObraService,
            UsuarioRepository usuarioRepository,
            AuditoriaEventoRepository auditRepository,
            TrackingCodeService trackingCodeService) {
        this.obraRepository = obraRepository;
        this.imageService = imageService;
        this.fotosObraService = fotosObraService;
        this.usuarioRepository = usuarioRepository;
        this.auditRepository = auditRepository;
        this.trackingCodeService = trackingCodeService;
    }

    @Transactional(readOnly = true)
    public List<ObraResponse> listar(Usuario actor) {
        requireEngineerOrAdmin(actor);
        return obraRepository.findAllByEliminadaEnIsNullOrderByIdDesc().stream()
                .filter(obra -> actor.getRol() == Rol.ADMIN || obra.estaAsignadaA(actor.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteObraResponse> listarClientes(Usuario actor) {
        requireAdmin(actor);
        return usuarioRepository.findAllByRolAndActivoTrueOrderByNombreAsc(Rol.CLIENTE).stream()
                .filter(PanelObrasService::hasSupabaseAccount)
                .map(cliente -> new ClienteObraResponse(
                        cliente.getId(),
                        clientDisplayName(cliente),
                        cliente.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ObraResponse obtener(Long obraId, Usuario actor) {
        Obra obra = loadActive(obraId);
        requireCanOperate(obra, actor);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse crear(ObraRequest request, Usuario actor, String ip) {
        requireEngineerOrAdmin(actor);
        Obra obra = new Obra();
        if (request.getClienteId() != null) {
            requireAdmin(actor);
            setClient(obra, resolveClient(request.getClienteId()));
        } else {
            obra.setCliente(requireClientName(request.getCliente()));
        }
        if (actor.getRol() == Rol.INGENIERO) {
            if (actor.getId() == null || !hasSupabaseAccount(actor)) {
                throw new AccessDeniedException("Se requiere una cuenta de ingeniero vinculada.");
            }
            obra.getAsignaciones().add(new AsignacionObra(actor.getId(), actor.getSupabaseUid(), LocalDateTime.now()));
        }
        applyRequest(obra, request);
        TrackingCodeService.GeneratedTrackingCode generatedCode = trackingCodeService.generate();
        obra.setCodigoHash(generatedCode.hash());
        obra.setCodigoCifrado(trackingCodeService.encrypt(generatedCode.value(), generatedCode.hash()));
        obra.setCodigoLegacy(false);
        Obra saved = obraRepository.saveAndFlush(obra);
        audit(actor, "OBRA_CREADA", saved.getId(), ip);
        return toResponse(saved, generatedCode.value());
    }

    @Transactional
    public ObraResponse actualizar(Long obraId, ObraRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (request.getClienteId() != null && !Objects.equals(obra.getClienteId(), request.getClienteId())) {
            requireAdmin(actor);
            setClient(obra, resolveClient(request.getClienteId()));
        }
        applyRequest(obra, request);
        audit(actor, "OBRA_ACTUALIZADA", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse actualizarParcial(
            Long obraId, ObraUpdateRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (!(request.hasNombre() || request.hasCliente() || request.hasClienteId() || request.hasCategoria()
                || request.hasEstado() || request.hasProgreso() || request.hasDescripcion()
                || request.hasFechaInicio() || request.hasFechaFinEstimada() || request.hasHitos())) {
            throw new IllegalArgumentException("Debes indicar al menos un campo para actualizar.");
        }

        if (request.hasNombre()) {
            if (request.getNombre() == null || request.getNombre().isBlank()) {
                throw new IllegalArgumentException("El nombre de la obra es obligatorio.");
            }
            obra.setNombreObra(request.getNombre().trim());
        }
        if (request.hasClienteId()) {
            if (request.getClienteId() == null) {
                throw new IllegalArgumentException("El cliente de la obra es obligatorio.");
            }
            if (actor.getRol() != Rol.ADMIN && !Objects.equals(obra.getClienteId(), request.getClienteId())) {
                throw new AccessDeniedException("Solo un administrador puede cambiar el cliente de una obra.");
            }
            if (!Objects.equals(obra.getClienteId(), request.getClienteId())) {
                setClient(obra, resolveClient(request.getClienteId()));
            }
        }
        if (request.hasCliente()) {
            obra.setCliente(requireClientName(request.getCliente()));
        }
        if (request.hasCategoria()) {
            if (request.getCategoria() == null || request.getCategoria().isBlank()) {
                throw new IllegalArgumentException("La categoría de la obra es obligatoria.");
            }
            obra.setCategoria(request.getCategoria());
        }
        if (request.hasEstado()) {
            if (request.getEstado() == null) {
                throw new IllegalArgumentException("El estado de la obra es obligatorio.");
            }
            validateTransition(obra, request.getEstado());
            obra.setEstado(request.getEstado().name());
        }
        if (request.hasProgreso()) {
            validateProgress(request.getProgreso());
            obra.setPorcentajeAvance(request.getProgreso());
        }
        if (request.hasDescripcion()) {
            obra.setDescripcion(trimToNull(request.getDescripcion()));
        }
        if (request.hasFechaInicio()) {
            obra.setFechaInicio(request.getFechaInicio());
        }
        if (request.hasFechaFinEstimada()) {
            obra.setFechaFinEstimada(request.getFechaFinEstimada());
        }
        validateDateRange(obra.getFechaInicio(), obra.getFechaFinEstimada());
        if (request.hasHitos()) {
            obra.clearHitos();
            if (request.getHitos() != null) {
                request.getHitos().forEach(input -> agregarHito(obra, input));
            }
        }

        audit(actor, "OBRA_ACTUALIZADA", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse cambiarEstado(Long obraId, EstadoObraRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        validateTransition(obra, request.getEstado());
        obra.setEstado(request.getEstado().name());
        audit(actor, "ESTADO_CAMBIADO", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse cambiarProgreso(Long obraId, ProgresoObraRequest request, Usuario actor, String ip) {
        validateProgress(request.getProgreso());
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        obra.setPorcentajeAvance(request.getProgreso());
        audit(actor, "PROGRESO_ACTUALIZADO", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse regenerarCodigoSeguimiento(Long obraId, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        TrackingCodeService.GeneratedTrackingCode generatedCode = trackingCodeService.generate();
        obra.setCodigoHash(generatedCode.hash());
        obra.setCodigoCifrado(trackingCodeService.encrypt(generatedCode.value(), generatedCode.hash()));
        obra.setCodigoLegacy(false);
        audit(actor, "CODIGO_SEGUIMIENTO_ROTADO", obra.getId(), ip);
        return toResponse(obra, generatedCode.value());
    }

    @Transactional
    public void eliminarLogicamente(Long obraId, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        obra.setEliminadaEn(LocalDateTime.now());
        audit(actor, "OBRA_ELIMINADA_LOGICAMENTE", obra.getId(), ip);
    }

    @Transactional
    public ObraResponse asignarIngeniero(Long obraId, Long usuarioId, Usuario actor, String ip) {
        requireAdmin(actor);
        Obra obra = loadForUpdate(obraId);
        Usuario ingeniero = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ingeniero solicitado."));
        if (!ingeniero.isActivo() || ingeniero.getRol() != Rol.INGENIERO || !hasSupabaseAccount(ingeniero)) {
            throw new ConflictException("Solo se pueden asignar ingenieros activos.");
        }
        if (obra.estaAsignadaA(usuarioId)) {
            throw new ConflictException("El ingeniero ya está asignado a esta obra.");
        }
        obra.getAsignaciones().add(new AsignacionObra(ingeniero.getId(), actor.getSupabaseUid(), LocalDateTime.now()));
        audit(actor, "INGENIERO_ASIGNADO", obra.getId(), ip);
        return toResponse(obra);
    }

    private void applyRequest(Obra obra, ObraRequest request) {
        validateProgress(request.getProgreso());
        validateDateRange(request.getFechaInicio(), request.getFechaFinEstimada());
        EstadoObra nextState = request.getEstado();
        if (obra.getId() != null) {
            validateTransition(obra, nextState);
        }
        obra.setNombreObra(request.getNombre().trim());
        obra.setCategoria(request.getCategoria());
        obra.setEstado(nextState.name());
        obra.setPorcentajeAvance(request.getProgreso());
        obra.setDescripcion(trimToNull(request.getDescripcion()));
        obra.setFechaInicio(request.getFechaInicio());
        obra.setFechaFinEstimada(request.getFechaFinEstimada());
        obra.clearHitos();
        if (request.getHitos() != null) {
            request.getHitos().forEach(input -> agregarHito(obra, input));
        }
    }

    private Usuario resolveClient(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("El cliente de la obra es obligatorio.");
        }
        return usuarioRepository.findById(clientId)
                .filter(Usuario::isActivo)
                .filter(usuario -> usuario.getRol() == Rol.CLIENTE)
                .filter(PanelObrasService::hasSupabaseAccount)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente solicitado."));
    }

    private static void setClient(Obra obra, Usuario client) {
        obra.setClienteId(client.getId());
        obra.setCliente(clientDisplayName(client));
    }

    private static boolean hasSupabaseAccount(Usuario usuario) {
        return usuario.getSupabaseUid() != null && !usuario.getSupabaseUid().isBlank();
    }

    private static String requireClientName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            throw new IllegalArgumentException("Indica el nombre del cliente.");
        }
        return name;
    }

    private static String clientDisplayName(Usuario cliente) {
        if (cliente.getNombre() != null && !cliente.getNombre().isBlank()) {
            return cliente.getNombre();
        }
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            return cliente.getEmail();
        }
        return "Cliente #" + cliente.getId();
    }

    private static void agregarHito(Obra obra, HitoObraRequest input) {
        obra.addHito(new HitoObra((long) obra.getHitos().size() + 1, input.getNombre().trim(),
                input.getDescripcion(), input.getFechaObjetivo(), input.isCompletado()));
    }

    private static void validateTransition(Obra obra, EstadoObra nextState) {
        EstadoObra current;
        try {
            current = EstadoObra.valueOf(obra.getEstado());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ConflictException("La obra tiene un estado que requiere revisión administrativa.");
        }
        if (current != nextState && !current.permiteTransicionA(nextState)) {
            throw new ConflictException("La transición de estado de la obra no está permitida.");
        }
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha fin estimada no puede ser anterior a la fecha de inicio.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Obra loadActive(Long obraId) {
        return obraRepository.findByIdAndEliminadaEnIsNull(obraId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la obra solicitada."));
    }

    @Transactional(readOnly = true)
    public void verificarAcceso(Long obraId, Usuario actor) {
        requireCanOperate(loadActive(obraId), actor);
    }

    private Obra loadForUpdate(Long obraId) {
        return obraRepository.findActiveByIdForUpdate(obraId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la obra solicitada."));
    }

    private void requireCanOperate(Obra obra, Usuario actor) {
        requireEngineerOrAdmin(actor);
        if (actor.getRol() == Rol.INGENIERO
                && !obra.estaAsignadaA(actor.getId())) {
            throw new AccessDeniedException("No tienes acceso a esta obra.");
        }
    }

    private void requireEngineerOrAdmin(Usuario actor) {
        if (actor == null || !actor.isActivo()
                || (actor.getRol() != Rol.ADMIN && actor.getRol() != Rol.INGENIERO)) {
            throw new AccessDeniedException("No tienes permisos para operar el panel de obras.");
        }
    }

    private void requireAdmin(Usuario actor) {
        if (actor == null || !actor.isActivo() || actor.getRol() != Rol.ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede asignar ingenieros.");
        }
    }

    private void audit(Usuario actor, String action, Long obraId, String ip) {
        auditRepository.save(new AuditoriaEvento(actor.getSupabaseUid(), action, obraId, ip));
    }

    private void validateProgress(Integer progress) {
        if (progress == null || progress < 0 || progress > 100) {
            throw new IllegalArgumentException("El progreso debe estar entre 0 y 100.");
        }
    }

    private ObraResponse toResponse(Obra obra) {
        return toResponse(obra, obra.getCodigoCifrado() == null ? null
                : trackingCodeService.decrypt(obra.getCodigoCifrado(), obra.getCodigoHash()));
    }

    private ObraResponse toResponse(Obra obra, String trackingCode) {
        List<HitoObraResponse> hitos = obra.getHitos().stream()
                .map(hito -> new HitoObraResponse(
                        hito.id(), hito.nombre(), hito.descripcion(),
                        hito.fechaObjetivo(), hito.completado()))
                .toList();
        List<FotoObraDTO> fotos = obra.getFotos().stream().map(foto -> toPhotoResponse(obra, foto)).toList();
        return new ObraResponse(
                obra.getId(), obra.getNombreObra(), obra.getCliente(), obra.getClienteId(),
                obra.getCategoria(), obra.getEstado(), obra.getPorcentajeAvance(), obra.getDescripcion(),
                obra.getFechaInicio(), obra.getFechaFinEstimada(), hitos, fotos, trackingCode);
    }

    private FotoObraDTO toPhotoResponse(Obra obra, FotoObraGuardada foto) {
        return new FotoObraDTO(foto.id(), foto.nombre(), "/api/panel/obras/" + obra.getId() + "/fotos/" + foto.id(),
                foto.fecha(), foto.descripcion());
    }

    @Transactional
    public FotoObraDTO subirFoto(Long obraId, MultipartFile archivo, String descripcion, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (descripcion != null && descripcion.length() > 1000) {
            throw new IllegalArgumentException("La descripción de la foto admite hasta 1000 caracteres.");
        }
        String base64 = Base64.getEncoder().encodeToString(imageService.sanitize(archivo));
        ContenidoFotosObra contenido = fotosObraService.contenido(obraId);
        for (FotoObraGuardada foto : obra.getFotos()) {
            if (base64.equals(contenido.getContenido().get(foto.id().toString()))) {
                return toPhotoResponse(obra, foto);
            }
        }
        if (obra.getFotos().size() >= 20) {
            throw new ConflictException("La obra admite hasta 20 fotos.");
        }
        long fotoId;
        do {
            fotoId = java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 1L << 53);
        } while (contieneFoto(obra, fotoId));
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            nombre = "foto.png";
        }
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        FotoObraGuardada foto = new FotoObraGuardada(fotoId, nombre.substring(0, Math.min(nombre.length(), 200)),
                LocalDate.now(), trimToNull(descripcion), actor.getSupabaseUid());
        obra.getFotos().add(foto);
        contenido.guardar(fotoId, base64);
        audit(actor, "FOTO_OBRA_SUBIDA", obraId, ip);
        return toPhotoResponse(obra, foto);
    }

    private boolean contieneFoto(Obra obra, long fotoId) {
        return obra.getFotos().stream().anyMatch(foto -> foto.id() == fotoId);
    }

    @Transactional(readOnly = true)
    public byte[] obtenerFoto(Long obraId, Long fotoId, Usuario actor) {
        Obra obra = loadActive(obraId);
        requireCanOperate(obra, actor);
        obra.buscarFoto(fotoId);
        return fotosObraService.obtener(obraId, fotoId);
    }

    @Transactional
    public void eliminarFoto(Long obraId, Long fotoId, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        obra.getFotos().remove(obra.buscarFoto(fotoId));
        fotosObraService.contenido(obraId).eliminar(fotoId);
        audit(actor, "FOTO_OBRA_ELIMINADA", obraId, ip);
    }
}
