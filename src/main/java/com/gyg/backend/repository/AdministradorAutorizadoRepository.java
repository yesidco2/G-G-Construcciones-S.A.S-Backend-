package com.gyg.backend.repository;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdministradorAutorizadoRepository extends JpaRepository<AdministradorAutorizado, Long> {
    Optional<AdministradorAutorizado> findByEmail(String email);

    List<AdministradorAutorizado> findAllByOrderByEmailAsc();

    long countByActivoTrueAndRol(Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdministradorAutorizado a WHERE a.rol = :rol AND a.activo = true ORDER BY a.id")
    List<AdministradorAutorizado> findActiveByRolForUpdate(@Param("rol") Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdministradorAutorizado a WHERE a.id = :id")
    Optional<AdministradorAutorizado> findByIdForUpdate(@Param("id") Long id);
}
