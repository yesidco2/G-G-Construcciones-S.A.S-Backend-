package com.gyg.backend.config;

import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.SupabaseAuth;
import com.gyg.backend.service.UsuarioService;
import com.gyg.backend.service.VerifiedSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class SupabaseTokenFilter extends OncePerRequestFilter {
    private final SupabaseAuth auth;
    private final UsuarioService users;

    public SupabaseTokenFilter(SupabaseAuth auth, UsuarioService users) {
        this.auth = auth;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                SupabaseAuth.Identity identity = auth.verify(header.substring(7).trim());
                Usuario user = users.obtenerOCrear(identity);
                if (user.isActivo() && (user.getRol() == Rol.ADMIN || user.getRol() == Rol.INGENIERO)) {
                    request.setAttribute(VerifiedSessionService.IDENTITY_ATTRIBUTE, identity);
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRol().name()))));
                }
            } catch (RuntimeException | IOException failure) {
                logger.debug("No se pudo autenticar la solicitud con Supabase.");
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
