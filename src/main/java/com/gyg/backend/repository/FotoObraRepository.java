package com.gyg.backend.repository;

import com.gyg.backend.model.FotoObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FotoObraRepository extends JpaRepository<FotoObra, Long> {

    long countByObra_Id(Long obraId);

    Optional<FotoObra> findByIdAndObra_Id(Long id, Long obraId);
}
