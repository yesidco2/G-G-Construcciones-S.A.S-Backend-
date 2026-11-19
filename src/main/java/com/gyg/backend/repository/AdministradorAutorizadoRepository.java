package com.gyg.backend.repository;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Compatibility facade for authorizations stored on the account row. */
@Repository
@Transactional
public class AdministradorAutorizadoRepository {
    private final UsuarioRepository usuarios;

    public AdministradorAutorizadoRepository(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<AdministradorAutorizado> findByEmail(String email) {
        return usuarios.findByAutorizacionEmail(email).map(this::toAuthorization);
    }

    public List<AdministradorAutorizado> findAllByOrderByEmailAsc() {
        return usuarios.findAllByAutorizacionEmailIsNotNullOrderByAutorizacionEmailAsc()
                .stream().map(this::toAuthorization).toList();
    }

    public long countByActivoTrueAndRol(Rol rol) {
        return usuarios.countByAutorizacionActivaTrueAndRolAutorizado(rol);
    }

    public List<AdministradorAutorizado> findActiveByRolForUpdate(Rol rol) {
        return usuarios.findActiveAuthorizationsForUpdate(rol)
                .stream().map(this::toAuthorization).toList();
    }

    public Optional<AdministradorAutorizado> findByIdForUpdate(Long id) {
        return usuarios.findByIdForUpdate(id)
                .filter(user -> user.getAutorizacionEmail() != null)
                .map(this::toAuthorization);
    }

    public AdministradorAutorizado save(AdministradorAutorizado authorization) {
        if (authorization.getRol() != Rol.ADMIN && authorization.getRol() != Rol.INGENIERO) {
            throw new IllegalArgumentException("Solo se pueden autorizar ADMIN e INGENIERO.");
        }
        String email = authorization.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        Usuario user = authorization.getId() == null
                ? usuarios.findByEmailIgnoreCase(email).orElseGet(Usuario::new)
                : usuarios.findByIdForUpdate(authorization.getId()).orElseThrow();
        if (user.getId() == null) {
            user.setEmail(email);
        }
        user.setAutorizacionEmail(email);
        user.setRolAutorizado(authorization.getRol());
        user.setAutorizacionActiva(authorization.isActivo());
        user.setAutorizadoPor(authorization.getCreadoPor());
        user.setAutorizadoEn(authorization.getCreadoEn() == null
                ? LocalDateTime.now() : authorization.getCreadoEn());
        return toAuthorization(usuarios.save(user));
    }

    public AdministradorAutorizado saveAndFlush(AdministradorAutorizado authorization) {
        AdministradorAutorizado saved = save(authorization);
        usuarios.flush();
        return saved;
    }

    public void delete(AdministradorAutorizado authorization) {
        Usuario user = usuarios.findByIdForUpdate(authorization.getId()).orElseThrow();
        user.setAutorizacionEmail(null);
        user.setRolAutorizado(null);
        user.setAutorizacionActiva(false);
        usuarios.save(user);
    }

    private AdministradorAutorizado toAuthorization(Usuario user) {
        AdministradorAutorizado result = new AdministradorAutorizado();
        result.setId(user.getId());
        result.setEmail(user.getAutorizacionEmail());
        result.setRol(user.getRolAutorizado());
        result.setActivo(user.isAutorizacionActiva());
        result.setCreadoPor(user.getAutorizadoPor());
        result.setCreadoEn(user.getAutorizadoEn());
        return result;
    }
}
