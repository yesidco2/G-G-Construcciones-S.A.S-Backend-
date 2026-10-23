package com.gyg.backend.repository;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministradorAutorizadoRepository extends JpaRepository<AdministradorAutorizado, Long> {
    Optional<AdministradorAutorizado> findByEmail(String email);

    List<AdministradorAutorizado> findAllByOrderByEmailAsc();

    long countByActivoTrueAndRol(Rol rol);
}
