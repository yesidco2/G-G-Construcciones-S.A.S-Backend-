package com.gyg.backend.service;

import com.gyg.backend.dto.panel.ProgresoObraRequest;
import com.gyg.backend.exception.ResourceNotFoundException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private Usuario user(Rol role, String uid) {
        Usuario user = new Usuario();
        user.setFirebaseUid(uid);
        user.setRol(role);
        user.setActivo(true);
        return user;
    }

    private Obra obra(Long id) {
        Obra obra = new Obra("Proyecto", "TEST-CODE-123", "EN_PROGRESO", 1L, 50);
        obra.setId(id);
        return obra;
    }
}
