package com.gyg.backend.repository;

import com.gyg.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.gyg.backend.model.Rol;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByEmailAsc();

    long countByRolAndActivoTrue(Rol rol);
}
