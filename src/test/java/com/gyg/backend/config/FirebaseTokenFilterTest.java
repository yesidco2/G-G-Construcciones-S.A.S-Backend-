package com.gyg.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebaseTokenFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCacheOrdinaryVerifiedTokens() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        FirebaseToken token = token();
        Usuario user = activeUser();
        when(auth.verifyIdToken("valid-token", false)).thenReturn(token);
        when(usuarioService.obtenerOCrear(token)).thenReturn(user);
        FirebaseTokenFilter filter = new FirebaseTokenFilter(auth, usuarioService);
        FilterChain chain = (request, response) -> { };

        filter.doFilter(request("/api/proyectos"), new MockHttpServletResponse(), chain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
        filter.doFilter(request("/api/servicios"), new MockHttpServletResponse(), chain);

        verify(auth, times(1)).verifyIdToken("valid-token", false);
    }

    @Test
    void shouldCheckRevocationOnlyForPanelAndAdminRequests() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        FirebaseToken token = token();
        when(auth.verifyIdToken("valid-token", true)).thenReturn(token);
        when(usuarioService.obtenerOCrear(token)).thenReturn(activeUser());
        FirebaseTokenFilter filter = new FirebaseTokenFilter(auth, usuarioService);

        filter.doFilter(request("/api/panel/obras"), new MockHttpServletResponse(), (req, res) -> { });

        verify(auth).verifyIdToken(eq("valid-token"), eq(true));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }

    private FirebaseToken token() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getClaims()).thenReturn(Map.of("exp", System.currentTimeMillis() / 1000 + 3600));
        return token;
    }

    private Usuario activeUser() {
        Usuario user = new Usuario();
        user.setActivo(true);
        return user;
    }
}
