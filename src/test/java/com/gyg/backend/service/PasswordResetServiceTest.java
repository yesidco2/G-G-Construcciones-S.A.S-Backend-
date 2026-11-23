package com.gyg.backend.service;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    @Test
    void requestsRecoveryForAuthorizedAccount() {
        var repository = mock(AdministradorAutorizadoRepository.class);
        var auth = mock(SupabaseAuth.class);
        var authorization = mock(AdministradorAutorizado.class);
        when(authorization.isActivo()).thenReturn(true);
        when(authorization.getRol()).thenReturn(Rol.INGENIERO);
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(authorization));

        new PasswordResetService(repository, auth).request(" Admin@example.com ");

        verify(auth).recover("admin@example.com");
    }

    @Test
    void skipsAddressesWithoutAuthorization() {
        var repository = mock(AdministradorAutorizadoRepository.class);
        var auth = mock(SupabaseAuth.class);

        new PasswordResetService(repository, auth).request("admin@example.com");

        verifyNoInteractions(auth);
    }
}
