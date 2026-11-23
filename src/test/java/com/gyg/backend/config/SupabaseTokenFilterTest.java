package com.gyg.backend.config;

import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.SupabaseAuth;
import com.gyg.backend.service.UsuarioService;
import com.gyg.backend.service.VerifiedSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupabaseTokenFilterTest {
    private final SupabaseAuth auth = mock(SupabaseAuth.class);
    private final UsuarioService users = mock(UsuarioService.class);
    private final SupabaseTokenFilter filter = new SupabaseTokenFilter(auth, users);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAuthorityFromTheBusinessAccount() throws Exception {
        var identity = new SupabaseAuth.Identity("uid", "admin@example.com", true, "session", Instant.MAX);
        var user = new Usuario();
        user.setRol(Rol.ADMIN);
        when(auth.verify("token")).thenReturn(identity);
        when(users.obtenerOCrear(identity)).thenReturn(user);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertSame(user, authentication.getPrincipal());
            assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
            assertSame(identity, VerifiedSessionService.identity(request));
        });
    }

    @Test
    void leavesRequestsWithInvalidTokensUnauthenticated() throws Exception {
        when(auth.verify("invalid")).thenThrow(new IllegalArgumentException());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));
        verifyNoInteractions(users);
    }
}
