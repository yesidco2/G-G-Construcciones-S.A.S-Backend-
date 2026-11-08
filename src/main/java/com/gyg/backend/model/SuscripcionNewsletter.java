package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suscripciones_newsletter")
public class SuscripcionNewsletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo", nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "acepta_politica", nullable = false)
    private boolean aceptaPolitica;

    @Column(name = "version_politica", nullable = false, length = 50)
    private String versionPolitica;

    @Column(name = "fecha_consentimiento", nullable = false)
    private LocalDateTime fechaConsentimiento;

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public boolean isAceptaPolitica() {
        return aceptaPolitica;
    }

    public void setAceptaPolitica(boolean aceptaPolitica) {
        this.aceptaPolitica = aceptaPolitica;
    }

    public String getVersionPolitica() {
        return versionPolitica;
    }

    public void setVersionPolitica(String versionPolitica) {
        this.versionPolitica = versionPolitica;
    }

    public LocalDateTime getFechaConsentimiento() {
        return fechaConsentimiento;
    }

    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) {
        this.fechaConsentimiento = fechaConsentimiento;
    }
}
