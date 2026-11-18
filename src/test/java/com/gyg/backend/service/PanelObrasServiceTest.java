package com.gyg.backend.service;

import com.gyg.backend.dto.panel.ProgresoObraRequest;
import com.gyg.backend.dto.panel.ObraRequest;
import com.gyg.backend.dto.panel.ObraUpdateRequest;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.FotoObra;
import com.gyg.backend.model.Obra;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AuditoriaEventoRepository;
import com.gyg.backend.repository.FotoObraRepository;
import com.gyg.backend.repository.ObraIngenieroRepository;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PanelObrasServiceTest {

    @Mock
    private ObraRepository obraRepository;
    @Mock
    private ObraIngenieroRepository assignmentRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FotoObraRepository fotoRepository;
    @Mock
    private AuditoriaEventoRepository auditRepository;
    @Mock
    private ObraPhotoStorageService photoStorage;
    @Mock
    private TrackingCodeService trackingCodeService;

    @InjectMocks
    private PanelObrasService service;

    @Test
    void shouldForbidEngineerFromOperatingOnUnassignedWork() {
        Usuario engineer = user(Rol.INGENIERO, "engineer-uid");
        Obra obra = obra(10L);
        when(obraRepository.findByIdAndEliminadaEnIsNull(10L)).thenReturn(Optional.of(obra));
        when(assignmentRepository.existsByObra_IdAndUsuario_FirebaseUid(10L, "engineer-uid"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.obtener(10L, engineer));
    }

    @Test
    void shouldForbidRegularUserFromOperatingPanel() {
        Usuario regularUser = user(Rol.USUARIO, "user-uid");
        Obra obra = obra(10L);
        when(obraRepository.findByIdAndEliminadaEnIsNull(10L)).thenReturn(Optional.of(obra));

        assertThrows(AccessDeniedException.class, () -> service.obtener(10L, regularUser));
    }

    @Test
    void shouldAllowAdminToOperateOnAnyActiveWork() {
        Usuario admin = user(Rol.ADMIN, "admin-uid");
        Obra obra = obra(10L);
        when(obraRepository.findByIdAndEliminadaEnIsNull(10L)).thenReturn(Optional.of(obra));

        assertEquals(10L, service.obtener(10L, admin).id());
    }

    @Test
    void shouldRejectProgressOutsideAllowedRange() {
        ProgresoObraRequest request = new ProgresoObraRequest();
        request.setProgreso(101);

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarProgreso(10L, request, user(Rol.ADMIN, "admin-uid"), "127.0.0.1"));
    }

    @Test
    void shouldHideSoftDeletedWorkFromReads() {
        when(obraRepository.findByIdAndEliminadaEnIsNull(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtener(10L, user(Rol.ADMIN, "admin-uid")));
    }

    @Test
    void shouldSoftDeleteWorkWithoutRemovingItsRecord() {
        Obra obra = obra(10L);
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));

        service.eliminarLogicamente(10L, user(Rol.ADMIN, "admin-uid"), "127.0.0.1");

        org.junit.jupiter.api.Assertions.assertNotNull(obra.getEliminadaEn());
        verify(auditRepository).save(any());
    }

    @Test
    void shouldCreateWorkOnlyForAnExistingActiveClientAndReturnCodeOnce() {
        Usuario admin = user(Rol.ADMIN, "admin-uid");
        Usuario client = user(Rol.CLIENTE, "client-uid");
        client.setId(2L);
        client.setNombre("Cliente Real");
        ObraRequest request = obraRequest(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(client));
        when(trackingCodeService.generate())
                .thenReturn(new TrackingCodeService.GeneratedTrackingCode("SECURE-CODE", "hash"));
        when(obraRepository.saveAndFlush(any(Obra.class))).thenAnswer(invocation -> {
            Obra saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        var response = service.crear(request, admin, "127.0.0.1");

        assertEquals(11L, response.id());
        assertEquals(2L, response.clienteId());
        assertEquals("Cliente Real", response.cliente());
        assertEquals("SECURE-CODE", response.codigoSeguimiento());
        verify(auditRepository).save(any());
    }

    @Test
    void shouldRejectMissingOrInactiveClientOnCreation() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crear(
                obraRequest(999L), user(Rol.ADMIN, "admin-uid"), "127.0.0.1"));

        Usuario regularUser = user(Rol.USUARIO, "user-uid");
        regularUser.setId(5L);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(regularUser));
        assertThrows(ResourceNotFoundException.class, () -> service.crear(
                obraRequest(5L), user(Rol.ADMIN, "admin-uid"), "127.0.0.1"));
        verify(obraRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldAllowAdministratorsToChangeAnExistingClient() {
        Obra obra = obra(10L);
        Usuario client = user(Rol.CLIENTE, "client-uid");
        client.setId(2L);
        client.setNombre("Cliente Autorizado");
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(client));
        ObraRequest request = obraRequest(2L);
        request.setEstado(com.gyg.backend.dto.panel.EstadoObra.EN_PROGRESO);

        var response = service.actualizar(
                10L, request, user(Rol.ADMIN, "admin-uid"), "127.0.0.1");

        assertEquals(2L, response.clienteId());
        assertEquals("Cliente Autorizado", response.cliente());
    }

    @Test
    void shouldNotAllowEngineersToCreateOrReassignWorkClients() {
        Usuario engineer = user(Rol.INGENIERO, "engineer-uid");
        assertThrows(AccessDeniedException.class,
                () -> service.crear(obraRequest(2L), engineer, "127.0.0.1"));

        Obra obra = obra(10L);
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));
        when(assignmentRepository.existsByObra_IdAndUsuario_FirebaseUid(10L, "engineer-uid"))
                .thenReturn(true);
        assertThrows(AccessDeniedException.class,
                () -> service.actualizar(10L, obraRequest(2L), engineer, "127.0.0.1"));
    }

    @Test
    void shouldPreserveFieldsOmittedFromPartialUpdates() {
        Obra obra = obra(10L);
        obra.setDescripcion("Descripción existente");
        obra.setFechaInicio(LocalDate.of(2026, 1, 1));
        obra.setFechaFinEstimada(LocalDate.of(2026, 12, 31));
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));

        ObraUpdateRequest request = new ObraUpdateRequest();
        request.setNombre("Nombre actualizado");

        var response = service.actualizarParcial(
                10L, request, user(Rol.ADMIN, "admin-uid"), "127.0.0.1");

        assertEquals("Nombre actualizado", response.nombre());
        assertEquals("Descripción existente", response.descripcion());
        assertEquals(LocalDate.of(2026, 1, 1), response.fechaInicio());
        assertEquals(LocalDate.of(2026, 12, 31), response.fechaFinEstimada());
    }

    @Test
    void shouldRejectInvalidStateTransitions() {
        Obra obra = obra(10L);
        obra.setEstado("FINALIZADA");
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));
        var request = new com.gyg.backend.dto.panel.EstadoObraRequest();
        request.setEstado(com.gyg.backend.dto.panel.EstadoObra.EN_PROGRESO);

        assertThrows(com.gyg.backend.exception.ConflictException.class,
                () -> service.cambiarEstado(10L, request, user(Rol.ADMIN, "admin-uid"), "127.0.0.1"));
    }

    @Test
    void shouldRotateWorkTrackingCodeAndReturnTheNewCodeOnlyOnce() {
        Obra obra = obra(10L);
        obra.setCodigoLegacy(true);
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));
        when(trackingCodeService.generate())
                .thenReturn(new TrackingCodeService.GeneratedTrackingCode("NEW-SECURE-CODE", "new-hash"));

        var response = service.regenerarCodigoSeguimiento(
                10L, user(Rol.ADMIN, "admin-uid"), "127.0.0.1");

        assertEquals("NEW-SECURE-CODE", response.codigoSeguimiento());
        assertEquals("new-hash", obra.getCodigoHash());
        org.junit.jupiter.api.Assertions.assertFalse(obra.isCodigoLegacy());
    }

    @Test
    void shouldStorePhotoDescriptionAndDeleteOnlyAuthorizedPhotos() throws Exception {
        Obra obra = obra(10L);
        when(obraRepository.findActiveByIdForUpdate(10L)).thenReturn(Optional.of(obra));
        when(obraRepository.findByIdAndEliminadaEnIsNull(10L)).thenReturn(Optional.of(obra));
        when(fotoRepository.countByObra_Id(10L)).thenReturn(0L);
        when(photoStorage.store(any())).thenReturn(
                new ObraPhotoStorageService.StoredPhoto("generated.png", "image/png"));
        when(fotoRepository.saveAndFlush(any(FotoObra.class))).thenAnswer(invocation -> {
            FotoObra photo = invocation.getArgument(0);
            photo.setId(20L);
            photo.getObra().addFoto(photo);
            return photo;
        });

        var response = service.subirFoto(10L, new org.springframework.mock.web.MockMultipartFile(
                "archivo", "obra.png", "image/png", new byte[] {1}), "Avance de cimentación",
                user(Rol.ADMIN, "admin-uid"), "127.0.0.1");
        assertEquals("Avance de cimentación", response.fotos().get(0).descripcion());

        ArgumentCaptor<FotoObra> photoCaptor = ArgumentCaptor.forClass(FotoObra.class);
        verify(fotoRepository).saveAndFlush(photoCaptor.capture());
        assertEquals("Avance de cimentación", photoCaptor.getValue().getDescripcion());

        when(fotoRepository.findByIdAndObra_Id(20L, 10L)).thenReturn(Optional.of(photoCaptor.getValue()));
        when(photoStorage.read("generated.png")).thenReturn(new byte[] {1, 2, 3});
        assertEquals(3, service.obtenerFoto(10L, 20L, user(Rol.ADMIN, "admin-uid")).content().length);

        service.eliminarFoto(10L, 20L, user(Rol.ADMIN, "admin-uid"), "127.0.0.1");
        verify(photoStorage).delete("generated.png");
        verify(fotoRepository).delete(photoCaptor.getValue());
    }

    private Usuario user(Rol role, String uid) {
        Usuario user = new Usuario();
        user.setFirebaseUid(uid);
        user.setRol(role);
        user.setActivo(true);
        return user;
    }

    private Obra obra(Long id) {
        Obra obra = new Obra("Proyecto", "EN_PROGRESO", 1L, 50);
        obra.setId(id);
        obra.setCodigoHash("test-code-hash");
        return obra;
    }

    private ObraRequest obraRequest(Long clientId) {
        ObraRequest request = new ObraRequest();
        request.setNombre("Obra de prueba");
        request.setClienteId(clientId);
        request.setCliente("Este nombre no se usa para asignar el cliente");
        request.setCategoria("CIVIL");
        request.setEstado(com.gyg.backend.dto.panel.EstadoObra.PLANIFICADA);
        request.setProgreso(0);
        return request;
    }
}
