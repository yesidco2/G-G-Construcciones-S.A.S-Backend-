package com.gyg.backend.service;

import com.gyg.backend.dto.ActualizarUsuarioRequest;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class AdministracionServiceTest {

    @Test
    void shouldLockAdministratorStateBeforeUpdatingUser() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        AdministradorAutorizadoRepository authorizations = mock(AdministradorAutorizadoRepository.class);
        AdministracionService service = new AdministracionService(authorizations, users);

        Usuario target = user(1L, "target@example.com", Rol.ADMIN);
        Usuario actor = user(2L, "actor@example.com", Rol.ADMIN);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(target));
        when(users.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(2L);
        when(authorizations.findByEmail("target@example.com")).thenReturn(Optional.empty());
        when(authorizations.countByActivoTrueAndRol(Rol.ADMIN)).thenReturn(1L);
        when(users.save(target)).thenReturn(target);

        service.actualizarUsuario(1L, new ActualizarUsuarioRequest(null, true), actor);

        verify(users).findActiveByRolForUpdate(Rol.ADMIN);
        verify(authorizations).findActiveByRolForUpdate(Rol.ADMIN);
        verify(users).findByIdForUpdate(1L);
    }

    @Test
    void shouldAssignClientRoleOnlyThroughAdministratorUserManagement() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        AdministradorAutorizadoRepository authorizations = mock(AdministradorAutorizadoRepository.class);
        AdministracionService service = new AdministracionService(authorizations, users);
        Usuario target = user(1L, "client@example.com", Rol.USUARIO);
        Usuario actor = user(2L, "admin@example.com", Rol.ADMIN);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(target));
        when(users.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);
        when(authorizations.findByEmail("client@example.com")).thenReturn(Optional.empty());
        when(authorizations.countByActivoTrueAndRol(Rol.ADMIN)).thenReturn(0L);
        when(users.save(target)).thenReturn(target);

        service.actualizarUsuario(1L, new ActualizarUsuarioRequest("CLIENTE", null), actor);

        org.junit.jupiter.api.Assertions.assertEquals(Rol.CLIENTE, target.getRol());
        verify(authorizations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Usuario user(Long id, String email, Rol role) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setEmail(email);
        user.setRol(role);
        user.setActivo(true);
        return user;
    }
}
