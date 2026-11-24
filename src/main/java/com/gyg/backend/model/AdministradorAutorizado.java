package com.gyg.backend.model;

import java.time.LocalDateTime;

public class AdministradorAutorizado {

    private Long id;

    private String email;

    private Rol rol = Rol.INGENIERO;

    private boolean activo = true;

    private String creadoPor;

    private LocalDateTime creadoEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) { this.id = id; }

    public void setCreadoEn(LocalDateTime value) { this.creadoEn = value; }

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
