package com.gyg.backend.repository;

import com.gyg.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import com.gyg.backend.model.Rol;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByAutorizacionEmail(String email);

    List<Usuario> findAllByAutorizacionEmailIsNotNullOrderByAutorizacionEmailAsc();

    long countByAutorizacionActivaTrueAndRolAutorizado(Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.autorizacionEmail = :email")
    Optional<Usuario> findAuthorizationForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.rolAutorizado = :rol AND u.autorizacionActiva = true ORDER BY u.id")
    List<Usuario> findActiveAuthorizationsForUpdate(@Param("rol") Rol rol);
    Optional<Usuario> findBySupabaseUid(String supabaseUid);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM Usuario u WHERE u.supabaseUid IS NOT NULL ORDER BY u.email")
    List<Usuario> findAllByOrderByEmailAsc();

    List<Usuario> findAllByRolAndActivoTrueOrderByNombreAsc(Rol rol);

    long countByRolAndActivoTrue(Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol AND u.activo = true ORDER BY u.id")
    List<Usuario> findActiveByRolForUpdate(@Param("rol") Rol rol);
}
