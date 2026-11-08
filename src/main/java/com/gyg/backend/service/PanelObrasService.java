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
    public ObraResponse obtener(Long obraId, Usuario actor) {
        Obra obra = loadActive(obraId);
        requireCanOperate(obra, actor);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse crear(ObraRequest request, Usuario actor, String ip) {
        requireEngineerOrAdmin(actor);
        Obra obra = new Obra();
        applyRequest(obra, request);
        TrackingCodeService.GeneratedTrackingCode generatedCode = trackingCodeService.generate();
        obra.setCodigoHash(generatedCode.hash());
        Obra saved = obraRepository.saveAndFlush(obra);
        if (actor.getRol() == Rol.INGENIERO) {
            assignmentRepository.save(new ObraIngeniero(saved, actor, actor.getFirebaseUid()));
        }
        audit(actor, "OBRA_CREADA", saved.getId(), ip);
        return toResponse(saved, generatedCode.value());
    }

    @Transactional
    public ObraResponse actualizar(Long obraId, ObraRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        applyRequest(obra, request);
        audit(actor, "OBRA_ACTUALIZADA", obra.getId(), ip);
        return toResponse(obra);
    }

    @Transactional
    public ObraResponse cambiarEstado(Long obraId, EstadoObraRequest request, Usuario actor, String ip) {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
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
    public ObraResponse subirFoto(Long obraId, MultipartFile file, Usuario actor, String ip) throws IOException {
        Obra obra = loadForUpdate(obraId);
        requireCanOperate(obra, actor);
        if (file == null || file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB.");
        }
        if (fotoRepository.countByObra_Id(obraId) >= MAX_PHOTOS_PER_OBRA) {
            throw new ConflictException("La obra alcanzó el máximo de 20 fotografías.");
        }

        ObraPhotoStorageService.StoredPhoto stored = photoStorage.store(file);
        try {
            FotoObra foto = new FotoObra("pending", LocalDate.now(), obra);
            foto.setStorageKey(stored.key());
            foto.setSubidoPor(actor.getFirebaseUid());
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

    private void applyRequest(Obra obra, ObraRequest request) {
        validateProgress(request.getProgreso());
        obra.setNombreObra(request.getNombre().trim());
        obra.setCliente(request.getCliente().trim());
        obra.setClienteId(request.getClienteId());
        obra.setCategoria(request.getCategoria());
        obra.setEstado(request.getEstado().name());
        obra.setPorcentajeAvance(request.getProgreso());
        obra.clearHitos();
        if (request.getHitos() != null) {
            request.getHitos().forEach(input -> {
                HitoObra hito = new HitoObra();
                hito.setNombre(input.getNombre().trim());
                hito.setDescripcion(input.getDescripcion());
                hito.setFechaObjetivo(input.getFechaObjetivo());
                hito.setCompletado(input.isCompletado());
                obra.addHito(hito);
            });
        }
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
                        foto.getId(), foto.getFecha(), foto.getSubidoPor(),
                        foto.getStorageKey() == null
                                ? foto.getUrl()
                                : "/api/panel/obras/" + obra.getId() + "/fotos/" + foto.getId()))
                .toList();
        return new ObraResponse(
                obra.getId(), obra.getNombreObra(), obra.getCliente(), obra.getClienteId(),
                obra.getCategoria(), obra.getEstado(), obra.getPorcentajeAvance(), hitos, fotos, trackingCode);
    }

    public record StoredImage(byte[] content, String contentType) {
    }
}
