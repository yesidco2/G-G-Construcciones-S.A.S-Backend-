package com.gyg.backend.repository;

import com.gyg.backend.model.Labor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LaborRepository extends JpaRepository<Labor, Long> {
    Optional<Labor> findByNombre(String nombre);

    @Query("SELECT DISTINCT l FROM Labor l LEFT JOIN FETCH l.riesgos LEFT JOIN FETCH l.epps WHERE l.id = :id")
    Optional<Labor> findByIdWithRiesgosAndEpps(@Param("id") Long id);
}
