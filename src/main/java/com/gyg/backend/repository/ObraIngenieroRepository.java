package com.gyg.backend.repository;

import com.gyg.backend.model.ObraIngeniero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObraIngenieroRepository extends JpaRepository<ObraIngeniero, Long> {

    boolean existsByObra_IdAndUsuario_FirebaseUid(Long obraId, String firebaseUid);

    boolean existsByObra_IdAndUsuario_Id(Long obraId, Long usuarioId);

    List<ObraIngeniero> findAllByObra_Id(Long obraId);

    List<ObraIngeniero> findAllByUsuario_FirebaseUidAndObra_EliminadaEnIsNullOrderByObra_IdDesc(
            String firebaseUid);
}
