package com.gyg.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.service.UsuarioService;
import com.gyg.backend.service.VerifiedSessionService;
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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    private final FirebaseAuth firebaseAuth;
    private final UsuarioService usuarioService;
    private final Cache<String, FirebaseToken> verifiedTokens = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

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
                    boolean privilegedRequest = requiresRevocationCheck(request);
                    String tokenKey = tokenFingerprint(idToken);
                    FirebaseToken token = privilegedRequest
                            ? firebaseAuth.verifyIdToken(idToken, true)
                            : cachedOrVerify(idToken, tokenKey);
                    Usuario usuario = usuarioService.obtenerOCrear(token);
                    if (usuario.isActivo()) {
                        request.setAttribute(
                                VerifiedSessionService.FirebaseTokenFilterAttribute.NAME, token);
                        if (privilegedRequest) {
                            verifiedTokens.put(tokenKey, token);
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        usuario,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (FirebaseAuthException exception) {
                    logger.warn("Falló la verificación del token de Firebase");
                    SecurityContextHolder.clearContext();
                } catch (RuntimeException exception) {
                    logger.warn(
                            "No se pudo verificar el token o resolver el usuario; la solicitud continuará como anónima");
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private FirebaseToken cachedOrVerify(String idToken, String tokenKey) throws FirebaseAuthException {
        FirebaseToken cached = verifiedTokens.getIfPresent(tokenKey);
        if (cached != null && !isExpired(cached)) {
            return cached;
        }
        verifiedTokens.invalidate(tokenKey);
        FirebaseToken verified = firebaseAuth.verifyIdToken(idToken, false);
        verifiedTokens.put(tokenKey, verified);
        return verified;
    }

    private static boolean requiresRevocationCheck(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/panel") || path.startsWith("/api/panel/")
                || path.equals("/api/admin") || path.startsWith("/api/admin/");
    }

    private static boolean isExpired(FirebaseToken token) {
        Object expiresAt = token.getClaims().get("exp");
        return expiresAt instanceof Number value
                && value.longValue() * 1000L <= System.currentTimeMillis();
    }

    private static String tokenFingerprint(String token) {
        try {
            byte[] fingerprint = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(fingerprint);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular la huella del token.", exception);
        }
    }
}
