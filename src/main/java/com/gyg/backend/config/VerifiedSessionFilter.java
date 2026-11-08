package com.gyg.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.service.VerifiedSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

public class VerifiedSessionFilter extends OncePerRequestFilter {

    private final VerifiedSessionService verifiedSessionService;
    private final ObjectMapper objectMapper;
    private final boolean captchaEnabled;

    public VerifiedSessionFilter(
            VerifiedSessionService verifiedSessionService,
            ObjectMapper objectMapper,
            boolean captchaEnabled) {
        this.verifiedSessionService = verifiedSessionService;
        this.objectMapper = objectMapper;
        this.captchaEnabled = captchaEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !captchaEnabled
                || !(path.startsWith("/api/panel/") || path.equals("/api/panel")
                || path.startsWith("/api/admin/") || path.equals("/api/admin"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!verifiedSessionService.isValid(VerifiedSessionService.firebaseToken(request))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "status", HttpServletResponse.SC_FORBIDDEN,
                    "error", "Acceso denegado",
                    "code", "CAPTCHA_REQUIRED",
                    "message", "Completa la verificación CAPTCHA para continuar."));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
