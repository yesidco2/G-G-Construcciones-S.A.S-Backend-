package com.gyg.backend.service;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdministradorAutorizadoRepository autorizadoRepository;

    private Usuario usuario;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, autorizadoRepository);
        usuario = new Usuario();
        usuario.setFirebaseUid("firebase-uid");
        usuario.setEmail("admin@example.com");

        when(usuarioRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assignsAuthorizedAdminOnlyWhenEmailIsVerified() {
        AdministradorAutorizado autorizado = authorizedAs(Rol.ADMIN, true);
        when(autorizadoRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(autorizado));

        Usuario result = usuarioService.obtenerOCrear(
                "firebase-uid", "Admin@Example.com", true, Map.of("name", "Admin"));

        assertEquals(Rol.ADMIN, result.getRol());
        assertEquals("admin@example.com", result.getEmail());
        assertEquals(true, result.isEmailVerificado());
    }

    @Test
    void leavesUnverifiedAuthorizedEmailAsRegularUser() {
        Usuario result = usuarioService.obtenerOCrear(
                "firebase-uid", "Admin@Example.com", false, Map.of("name", "Admin"));

        assertEquals(Rol.USUARIO, result.getRol());
        assertEquals(false, result.isEmailVerificado());
        verify(autorizadoRepository, never()).findByEmail(any());
    }

    @Test
    void leavesVerifiedButUnauthorizedEmailAsRegularUser() {
        when(autorizadoRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        Usuario result = usuarioService.obtenerOCrear(
                "firebase-uid", "Admin@Example.com", true, Map.of("name", "Admin"));

        assertEquals(Rol.USUARIO, result.getRol());
        assertEquals(true, result.isEmailVerificado());
    }

    @Test
    void appliesAuthorizedEngineerRoleFromDatabase() {
        AdministradorAutorizado autorizado = authorizedAs(Rol.INGENIERO, true);
        when(autorizadoRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(autorizado));

        Usuario result = usuarioService.obtenerOCrear(
                "firebase-uid", "Admin@Example.com", true, Map.of("name", "Admin"));

        assertEquals(Rol.INGENIERO, result.getRol());
    }

    private static AdministradorAutorizado authorizedAs(Rol rol, boolean active) {
        AdministradorAutorizado autorizado = new AdministradorAutorizado();
        autorizado.setEmail("admin@example.com");
        autorizado.setRol(rol);
        autorizado.setActivo(active);
        return autorizado;
    }
}