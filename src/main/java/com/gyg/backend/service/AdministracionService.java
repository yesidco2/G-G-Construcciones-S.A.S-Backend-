package com.gyg.backend.service;

import com.gyg.backend.dto.ActualizarUsuarioRequest;
import com.gyg.backend.dto.AdministradorAutorizadoDTO;
import com.gyg.backend.dto.AutorizacionRequest;
import com.gyg.backend.dto.UsuarioAdminDTO;
import com.gyg.backend.exception.ConflictException;
import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdministracionService {

    private final AdministradorAutorizadoRepository autorizadoRepository;
    private final UsuarioRepository usuarioRepository;

    public AdministracionService(
            AdministradorAutorizadoRepository autorizadoRepository,
            UsuarioRepository usuarioRepository) {
        this.autorizadoRepository = autorizadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AdministradorAutorizadoDTO> listarAutorizados() {
        return autorizadoRepository.findAllByOrderByEmailAsc().stream()
                .map(AdministradorAutorizadoDTO::from)
                .toList();
    }

    @Transactional
    public AdministradorAutorizadoDTO crearAutorizacion(AutorizacionRequest request, Usuario actor) {
        String email = normalizarEmail(request.email());
        if (autorizadoRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("El correo ya tiene una autorización registrada.");
        }

        AdministradorAutorizado autorizado = new AdministradorAutorizado();
        autorizado.setEmail(email);
        autorizado.setRol(Rol.valueOf(request.rol()));
        autorizado.setActivo(true);
        autorizado.setCreadoPor(actor.getEmail());
        try {
            return AdministradorAutorizadoDTO.from(autorizadoRepository.saveAndFlush(autorizado));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("El correo ya tiene una autorización registrada.");
        }
    }

    @Transactional
    public void eliminarAutorizacion(Long id, Usuario actor) {
        AdministradorAutorizado autorizado = autorizadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la autorización solicitada."));
        if (esMismoCorreo(autorizado.getEmail(), actor.getEmail())) {
            throw new ConflictException("No puedes eliminar tu propia autorización de administrador.");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(autorizado.getEmail()).orElse(null);
        long adminUsers = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN);
        long adminAuthorizations = autorizadoRepository.countByActivoTrueAndRol(Rol.ADMIN);
        long remainingUsers = adminUsers - (usuario != null && usuario.isActivo()
                && usuario.getRol() == Rol.ADMIN ? 1 : 0);
        long remainingAuthorizations = adminAuthorizations
                - (autorizado.isActivo() && autorizado.getRol() == Rol.ADMIN ? 1 : 0);
        if (usuario != null && usuario.isActivo() && usuario.getRol() == Rol.ADMIN
                && remainingUsers == 0) {
            throw new ConflictException(
                    "No puedes eliminar la autorización del último administrador activo.");
        }
        asegurarAdminRestante(remainingUsers, remainingAuthorizations);

        autorizadoRepository.delete(autorizado);
        if (usuario != null) {
            usuario.setRol(Rol.USUARIO);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarUsuarios() {
        return usuarioRepository.findAllByOrderByEmailAsc().stream()
                .map(UsuarioAdminDTO::from)
                .toList();
    }

    @Transactional
    public UsuarioAdminDTO actualizarUsuario(Long id, ActualizarUsuarioRequest request, Usuario actor) {
        if (request.rol() == null && request.activo() == null) {
            throw new IllegalArgumentException("Debes indicar al menos el rol o el estado activo.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario solicitado."));
        Rol rolActual = usuario.getRol();
        boolean activoActual = usuario.isActivo();
        Rol nuevoRol = request.rol() == null ? rolActual : Rol.valueOf(request.rol());
        boolean nuevoEstado = request.activo() == null ? activoActual : request.activo();

        if (esMismoCorreo(usuario.getEmail(), actor.getEmail())
                && (nuevoRol != Rol.ADMIN || !nuevoEstado)) {
            throw new ConflictException("No puedes bajar tu propio rol ni desactivar tu cuenta.");
        }

        AdministradorAutorizado autorizacionActual = usuario.getEmail() == null
                ? null
                : autorizadoRepository.findByEmail(normalizarEmail(usuario.getEmail())).orElse(null);
        boolean eliminaAutorizacionAdmin = nuevoRol != Rol.ADMIN
                && autorizacionActual != null
                && autorizacionActual.isActivo()
                && autorizacionActual.getRol() == Rol.ADMIN;

        long activeAdminUsers = usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN);
        boolean eraAdminActivo = rolActual == Rol.ADMIN && activoActual;
        boolean seguiraAdminActivo = nuevoRol == Rol.ADMIN && nuevoEstado;
        long remainingAdminUsers = activeAdminUsers - (eraAdminActivo && !seguiraAdminActivo ? 1 : 0);
        if (eraAdminActivo && !seguiraAdminActivo && remainingAdminUsers == 0) {
            throw new ConflictException(
                    "No puedes cambiar el estado o rol del último administrador activo.");
        }

        long activeAdminAuthorizations = autorizadoRepository.countByActivoTrueAndRol(Rol.ADMIN);
        long remainingAdminAuthorizations = activeAdminAuthorizations
                - (eliminaAutorizacionAdmin ? 1 : 0);
        asegurarAdminRestante(remainingAdminUsers, remainingAdminAuthorizations);

        if (request.rol() != null) {
            actualizarAutorizacionPorCambioDeRol(usuario, nuevoRol, actor, autorizacionActual);
            usuario.setRol(nuevoRol);
        }
        usuario.setActivo(nuevoEstado);
        return UsuarioAdminDTO.from(usuarioRepository.save(usuario));
    }

    private void actualizarAutorizacionPorCambioDeRol(
            Usuario usuario,
            Rol nuevoRol,
            Usuario actor,
            AdministradorAutorizado autorizacion) {
        if (nuevoRol == Rol.USUARIO) {
            if (autorizacion != null) {
                autorizadoRepository.delete(autorizacion);
            }
            return;
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new ConflictException("No se puede asignar un rol administrativo a un usuario sin correo.");
        }
        AdministradorAutorizado autorizada = autorizacion == null
                ? new AdministradorAutorizado()
                : autorizacion;
        autorizada.setEmail(normalizarEmail(usuario.getEmail()));
        autorizada.setRol(nuevoRol);
        autorizada.setActivo(true);
        if (autorizacion == null) {
            autorizada.setCreadoPor(actor.getEmail());
        }
        autorizadoRepository.save(autorizada);
    }

    private static void asegurarAdminRestante(long usuariosAdmin, long autorizacionesAdmin) {
        if (usuariosAdmin <= 0 && autorizacionesAdmin <= 0) {
            throw new ConflictException("La operación no está permitida porque dejaría el sistema sin administradores activos.");
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean esMismoCorreo(String left, String right) {
        return left != null && right != null
                && left.trim().equalsIgnoreCase(right.trim());
    }
}
