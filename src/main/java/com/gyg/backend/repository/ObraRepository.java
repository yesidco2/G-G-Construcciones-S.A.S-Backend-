package com.gyg.backend.repository;

import com.gyg.backend.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObraRepository extends JpaRepository<Obra, Long> {

    @Query("SELECT o FROM Obra o LEFT JOIN FETCH o.fotos WHERE o.codigoSeguimiento = :codigo")
    Optional<Obra> findByCodigoSeguimientoWithFotos(@Param("codigo") String codigo);
}
