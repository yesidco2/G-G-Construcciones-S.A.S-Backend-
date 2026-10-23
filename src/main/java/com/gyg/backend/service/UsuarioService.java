package com.gyg.backend.service;

import com.google.firebase.auth.FirebaseToken;
import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

    public Usuario obtenerOCrear(FirebaseToken token) {
        return obtenerOCrear(token.getUid(), token.getEmail(), token.isEmailVerified(), token.getClaims());
    }

    Usuario obtenerOCrear(String uid, String email, boolean emailVerificado, Map<String, Object> claims) {
        return usuarioRepository.findByFirebaseUid(uid)
                .map(usuario -> {
                    aplicarDatosDelToken(usuario, email, emailVerificado, claims);
                    return usuarioRepository.save(usuario);
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
                        aplicarDatosDelToken(existing, email, emailVerificado, claims);
                        return usuarioRepository.save(existing);
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private void aplicarDatosDelToken(
            Usuario usuario, String email, boolean emailVerificado, Map<String, Object> claims) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        usuario.setEmail(normalizedEmail);
        usuario.setEmailVerificado(emailVerificado);
        Object name = claims.get("name");
        usuario.setNombre(name instanceof String value ? value : null);

        Rol rol = emailVerificado && normalizedEmail != null
                ? administradorRepository.findByEmail(normalizedEmail)
                        .filter(AdministradorAutorizado::isActivo)
                        .map(AdministradorAutorizado::getRol)
                        .orElse(Rol.USUARIO)
                : Rol.USUARIO;
        usuario.setRol(rol);
    }
}
