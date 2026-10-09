package com.gyg.backend.repository;

import com.gyg.backend.model.PoliticaAmbiental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliticaAmbientalRepository extends JpaRepository<PoliticaAmbiental, Long> {
}
