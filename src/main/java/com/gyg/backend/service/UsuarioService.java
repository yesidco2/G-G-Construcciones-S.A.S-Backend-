package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseToken;
import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorAutorizadoRepository administradorRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AdministradorAutorizadoRepository administradorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.administradorRepository = administradorRepository;
    }

    @Transactional
    public Usuario obtenerOCrear(FirebaseToken token) {
        return obtenerOCrear(token.getUid(), token.getEmail(), token.isEmailVerified(), token.getClaims());
    }

    @Transactional
    Usuario obtenerOCrear(String uid, String email, boolean emailVerificado, Map<String, Object> claims) {
        return usuarioRepository.findByFirebaseUid(uid)
                .map(usuario -> {
                    return aplicarDatosDelToken(usuario, email, emailVerificado, claims)
                            ? usuarioRepository.save(usuario)
                            : usuario;
                })
                .orElseGet(() -> crearORecuperarCarrera(uid, email, emailVerificado, claims));
    }

    private Usuario crearORecuperarCarrera(
            String uid, String email, boolean emailVerificado, Map<String, Object> claims) {
        Usuario usuario = new Usuario();
        usuario.setFirebaseUid(uid);
        aplicarDatosDelToken(usuario, email, emailVerificado, claims);
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            return usuarioRepository.findByFirebaseUid(uid)
                    .map(existing -> {
                        return aplicarDatosDelToken(existing, email, emailVerificado, claims)
                                ? usuarioRepository.save(existing)
                                : existing;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private boolean aplicarDatosDelToken(
            Usuario usuario, String email, boolean emailVerificado, Map<String, Object> claims) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        Object name = claims.get("name");
        String nombre = name instanceof String value ? value : null;

        Rol rol = emailVerificado && normalizedEmail != null
                ? administradorRepository.findByEmail(normalizedEmail)
                        .filter(AdministradorAutorizado::isActivo)
                        .map(AdministradorAutorizado::getRol)
                        .orElse(Rol.USUARIO)
                : Rol.USUARIO;
        boolean changed = !java.util.Objects.equals(usuario.getEmail(), normalizedEmail)
                || usuario.isEmailVerificado() != emailVerificado
                || !java.util.Objects.equals(usuario.getNombre(), nombre)
                || usuario.getRol() != rol;
        if (changed) {
            usuario.setEmail(normalizedEmail);
            usuario.setEmailVerificado(emailVerificado);
            usuario.setNombre(nombre);
            usuario.setRol(rol);
        }
        return changed;
    }
}
