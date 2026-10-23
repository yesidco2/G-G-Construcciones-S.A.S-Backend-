package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "administradores_autorizados")
public class AdministradorAutorizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol = Rol.INGENIERO;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_por")
    private String creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    @PreUpdate
    void prepararPersistencia() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (rol == null) {
            rol = Rol.INGENIERO;
        } else if (rol == Rol.USUARIO) {
            throw new IllegalStateException("Una autorización debe tener rol INGENIERO o ADMIN.");
        }
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
