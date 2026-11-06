package com.gyg.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    private final FirebaseAuth firebaseAuth;
    private final UsuarioService usuarioService;

    public FirebaseTokenFilter(FirebaseAuth firebaseAuth, UsuarioService usuarioService) {
        this.firebaseAuth = firebaseAuth;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (firebaseAuth != null && authorization != null && authorization.startsWith("Bearer ")) {
            String idToken = authorization.substring(7).trim();
            if (!idToken.isEmpty()) {
                try {
                    FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
                    Usuario usuario = usuarioService.obtenerOCrear(token);
                    if (usuario.isActivo()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        usuario,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (FirebaseAuthException ignored) {
                    SecurityContextHolder.clearContext();
                } catch (RuntimeException exception) {
                    logger.warn(
                            "No se pudo resolver el usuario del token de Firebase; la solicitud continuará como anónima",
                            exception);
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
