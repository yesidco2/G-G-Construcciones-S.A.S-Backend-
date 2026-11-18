package com.gyg.backend.service;

import com.gyg.backend.dto.panel.*;
import com.gyg.backend.exception.ConflictException;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.*;
import com.gyg.backend.repository.AuditoriaEventoRepository;
import com.gyg.backend.repository.FotoObraRepository;
import com.gyg.backend.repository.ObraIngenieroRepository;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PanelObrasService {

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PHOTOS_PER_OBRA = 20;

    private final ObraRepository obraRepository;
    private final ObraIngenieroRepository assignmentRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoObraRepository fotoRepository;
    private final AuditoriaEventoRepository auditRepository;
    private final ObraPhotoStorageService photoStorage;
    private final TrackingCodeService trackingCodeService;

    public PanelObrasService(
            ObraRepository obraRepository,
            ObraIngenieroRepository assignmentRepository,
            UsuarioRepository usuarioRepository,
            FotoObraRepository fotoRepository,
            AuditoriaEventoRepository auditRepository,
            ObraPhotoStorageService photoStorage,
            TrackingCodeService trackingCodeService) {
        this.obraRepository = obraRepository;
        this.assignmentRepository = assignmentRepository;
        this.usuarioRepository = usuarioRepository;
        this.fotoRepository = fotoRepository;
        this.auditRepository = auditRepository;
        this.photoStorage = photoStorage;
        this.trackingCodeService = trackingCodeService;
    }

    @Transactional(readOnly = true)
    public List<ObraResponse> listar(Usuario actor) {
        requireEngineerOrAdmin(actor);
        List<Obra> obras = actor.getRol() == Rol.ADMIN
                ? obraRepository.findAllByEliminadaEnIsNullOrderByIdDesc()
                : assignmentRepository
                        .findAllByUsuario_FirebaseUidAndObra_EliminadaEnIsNullOrderByObra_IdDesc(
                                actor.getFirebaseUid())
                        .stream()
                        .map(ObraIngeniero::getObra)
                        .toList();
        return obras.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteObraResponse> listarClientes(Usuario actor) {
        requireAdmin(actor);
        return usuarioRepository.findAllByRolAndActivoTrueOrderByNombreAsc(Rol.CLIENTE).stream()
                .map(cliente -> new ClienteObraResponse(
                        cliente.getId(),
                        cliente.getNombre() == null || cliente.getNombre().isBlank()
                                ? cliente.getEmail() : cliente.getNombre(),
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
        requireAdmin(actor);
        Obra obra = new Obra();
        applyRequest(obra, request, resolveClient(request.getClienteId()));
        TrackingCodeService.GeneratedTrackingCode generatedCode = trackingCodeService.generate();
        obra.setCodigoHash(generatedCode.hash());
        obra.setCodigoLegacy(false);
        Obra saved = obraRepository.saveAndFlush(obra);
        audit(actor, "OBRA_CREADA", saved.getId(), ip);
        return toResponse(saved, generatedCode.value());
    }

    @Transactional
    public ObraResponse actualizar(Long obraId, ObraRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (actor.getRol() != Rol.ADMIN && !obra.getClienteId().equals(request.getClienteId())) {
            throw new AccessDeniedException("Solo un administrador puede cambiar el cliente de una obra.");
        }
        Usuario client = obra.getClienteId().equals(request.getClienteId())
                ? null : resolveClient(request.getClienteId());
        applyRequest(obra, request, client);
        audit(actor, "OBRA_ACTUALIZADA", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse actualizarParcial(
            Long obraId, ObraUpdateRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (!(request.hasNombre() || request.hasClienteId() || request.hasCategoria()
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
            if (actor.getRol() != Rol.ADMIN && !obra.getClienteId().equals(request.getClienteId())) {
                throw new AccessDeniedException("Solo un administrador puede cambiar el cliente de una obra.");
            }
            if (!obra.getClienteId().equals(request.getClienteId())) {
                setClient(obra, resolveClient(request.getClienteId()));
            }
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
                request.getHitos().forEach(input -> obra.addHito(toHito(input)));
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
        requireAdmin(actor);
        Obra obra = loadForUpdate(obraId);
        TrackingCodeService.GeneratedTrackingCode generatedCode = trackingCodeService.generate();
        obra.setCodigoHash(generatedCode.hash());
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
        if (!ingeniero.isActivo() || ingeniero.getRol() != Rol.INGENIERO) {
            throw new ConflictException("Solo se pueden asignar ingenieros activos.");
        }
        if (assignmentRepository.existsByObra_IdAndUsuario_Id(obraId, usuarioId)) {
            throw new ConflictException("El ingeniero ya está asignado a esta obra.");
        }
        assignmentRepository.save(new ObraIngeniero(obra, ingeniero, actor.getFirebaseUid()));
        audit(actor, "INGENIERO_ASIGNADO", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse subirFoto(
            Long obraId, MultipartFile file, String description, Usuario actor, String ip) throws IOException {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (file == null || file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB.");
        }
        String normalizedDescription = trimToNull(description);
        if (normalizedDescription != null && normalizedDescription.length() > 1000) {
            throw new IllegalArgumentException("La descripción de la fotografía no puede superar 1000 caracteres.");
        }
        if (fotoRepository.countByObra_Id(obraId) >= MAX_PHOTOS_PER_OBRA) {
            throw new ConflictException("La obra alcanzó el máximo de 20 fotografías.");
        }

        ObraPhotoStorageService.StoredPhoto stored = photoStorage.store(file);
        try {
            FotoObra foto = new FotoObra("pending", LocalDate.now(), obra);
            foto.setStorageKey(stored.key());
            foto.setSubidoPor(actor.getFirebaseUid());
            foto.setDescripcion(normalizedDescription);
            FotoObra saved = fotoRepository.saveAndFlush(foto);
            saved.setUrl("/api/panel/obras/" + obraId + "/fotos/" + saved.getId());
            audit(actor, "FOTO_SUBIDA", obraId, ip);
            return obtener(obraId, actor);
        } catch (RuntimeException exception) {
            photoStorage.delete(stored.key());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public StoredImage obtenerFoto(Long obraId, Long fotoId, Usuario actor) throws IOException {
        Obra obra = loadActive(obraId);
        requireCanOperate(obra, actor);
        FotoObra foto = fotoRepository.findByIdAndObra_Id(fotoId, obraId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la fotografía solicitada."));
        if (foto.getStorageKey() == null) {
            throw new ResourceNotFoundException("No existe la fotografía solicitada.");
        }
        return new StoredImage(photoStorage.read(foto.getStorageKey()), "image/png");
    }

    @Transactional
    public void eliminarFoto(Long obraId, Long fotoId, Usuario actor, String ip) throws IOException {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        FotoObra foto = fotoRepository.findByIdAndObra_Id(fotoId, obraId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la fotografía solicitada."));
        if (foto.getStorageKey() != null) {
            photoStorage.delete(foto.getStorageKey());
        }
        fotoRepository.delete(foto);
        audit(actor, "FOTO_ELIMINADA", obraId, ip);
    }

    private void applyRequest(Obra obra, ObraRequest request, Usuario client) {
        validateProgress(request.getProgreso());
        validateDateRange(request.getFechaInicio(), request.getFechaFinEstimada());
        EstadoObra nextState = request.getEstado();
        if (obra.getId() != null) {
            validateTransition(obra, nextState);
        }
        obra.setNombreObra(request.getNombre().trim());
        if (client != null) {
            setClient(obra, client);
        } else if (obra.getId() == null) {
            throw new IllegalArgumentException("El cliente de la obra es obligatorio.");
        }
        obra.setCategoria(request.getCategoria());
        obra.setEstado(nextState.name());
        obra.setPorcentajeAvance(request.getProgreso());
        obra.setDescripcion(trimToNull(request.getDescripcion()));
        obra.setFechaInicio(request.getFechaInicio());
        obra.setFechaFinEstimada(request.getFechaFinEstimada());
        obra.clearHitos();
        if (request.getHitos() != null) {
            request.getHitos().forEach(input -> obra.addHito(toHito(input)));
        }
    }

    private Usuario resolveClient(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("El cliente de la obra es obligatorio.");
        }
        return usuarioRepository.findById(clientId)
                .filter(Usuario::isActivo)
                .filter(usuario -> usuario.getRol() == Rol.CLIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente solicitado."));
    }

    private static void setClient(Obra obra, Usuario client) {
        obra.setClienteId(client.getId());
        obra.setCliente(client.getNombre() == null || client.getNombre().isBlank()
                ? client.getEmail() : client.getNombre());
    }

    private static HitoObra toHito(HitoObraRequest input) {
        HitoObra hito = new HitoObra();
        hito.setNombre(input.getNombre().trim());
        hito.setDescripcion(input.getDescripcion());
        hito.setFechaObjetivo(input.getFechaObjetivo());
        hito.setCompletado(input.isCompletado());
        return hito;
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

    private Obra loadForUpdate(Long obraId) {
        return obraRepository.findActiveByIdForUpdate(obraId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la obra solicitada."));
    }

    private void requireCanOperate(Obra obra, Usuario actor) {
        requireEngineerOrAdmin(actor);
        if (actor.getRol() == Rol.INGENIERO
                && !assignmentRepository.existsByObra_IdAndUsuario_FirebaseUid(
                        obra.getId(), actor.getFirebaseUid())) {
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
        auditRepository.save(new AuditoriaEvento(actor.getFirebaseUid(), action, obraId, ip));
    }

    private void validateProgress(Integer progress) {
        if (progress == null || progress < 0 || progress > 100) {
            throw new IllegalArgumentException("El progreso debe estar entre 0 y 100.");
        }
    }

    private ObraResponse toResponse(Obra obra) {
        return toResponse(obra, null);
    }

    private ObraResponse toResponse(Obra obra, String trackingCode) {
        List<HitoObraResponse> hitos = obra.getHitos().stream()
                .map(hito -> new HitoObraResponse(
                        hito.getId(), hito.getNombre(), hito.getDescripcion(),
                        hito.getFechaObjetivo(), hito.isCompletado()))
                .toList();
        List<FotoObraResponse> fotos = obra.getFotos().stream()
                .map(foto -> new FotoObraResponse(
                        foto.getId(),
                        foto.getStorageKey() == null
                                ? foto.getUrl()
                                : "/api/panel/obras/" + obra.getId() + "/fotos/" + foto.getId(),
                        foto.getFecha(), foto.getDescripcion(), foto.getSubidoPor()))
                .toList();
        return new ObraResponse(
                obra.getId(), obra.getNombreObra(), obra.getCliente(), obra.getClienteId(),
                obra.getCategoria(), obra.getEstado(), obra.getPorcentajeAvance(), obra.getDescripcion(),
                obra.getFechaInicio(), obra.getFechaFinEstimada(), hitos, fotos, trackingCode);
    }

    public record StoredImage(byte[] content, String contentType) {
    }
}
