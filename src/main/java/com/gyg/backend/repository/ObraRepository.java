package com.gyg.backend.repository;

import com.gyg.backend.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ObraRepository extends JpaRepository<Obra, Long> {

    @Query("SELECT DISTINCT o FROM Obra o LEFT JOIN FETCH o.fotos "
            + "WHERE o.codigoHash = :codigoHash AND o.eliminadaEn IS NULL")
    Optional<Obra> findByCodigoHashWithFotosAndHitos(@Param("codigoHash") String codigoHash);

    List<Obra> findAllByEliminadaEnIsNullOrderByIdDesc();

    Optional<Obra> findByIdAndEliminadaEnIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Obra o WHERE o.id = :id AND o.eliminadaEn IS NULL")
    Optional<Obra> findActiveByIdForUpdate(@Param("id") Long id);
}
