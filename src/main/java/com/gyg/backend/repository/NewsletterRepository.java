package com.gyg.backend.repository;

import com.gyg.backend.model.SuscripcionNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterRepository extends JpaRepository<SuscripcionNewsletter, Long> {

    Optional<SuscripcionNewsletter> findByCorreo(String correo);
}
