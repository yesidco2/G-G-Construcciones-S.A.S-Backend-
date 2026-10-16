package com.gyg.backend.repository;

import com.gyg.backend.model.SolicitudContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    Optional<SolicitudContacto> findTopByCorreoOrderByFechaDesc(String correo);
}
