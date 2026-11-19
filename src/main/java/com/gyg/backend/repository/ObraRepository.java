package com.gyg.backend.repository;

import com.gyg.backend.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ObraRepository extends JpaRepository<Obra, Long> {

    @Query("SELECT o FROM Obra o WHERE o.codigoHash = :codigoHash AND o.eliminadaEn IS NULL")
    Optional<Obra> findByCodigoHashWithHitos(@Param("codigoHash") String codigoHash);

    @Modifying
    @Query("""
            UPDATE Obra o SET o.codigoCifrado = :encrypted
            WHERE o.id = :id AND o.codigoHash = :hash AND o.codigoCifrado IS NULL AND o.eliminadaEn IS NULL
            """)
    int storeRecoveredTrackingCode(@Param("id") Long id, @Param("hash") String hash,
            @Param("encrypted") String encrypted);

    List<Obra> findAllByEliminadaEnIsNullOrderByIdDesc();

    Optional<Obra> findByIdAndEliminadaEnIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Obra o WHERE o.id = :id AND o.eliminadaEn IS NULL")
    Optional<Obra> findActiveByIdForUpdate(@Param("id") Long id);
}
