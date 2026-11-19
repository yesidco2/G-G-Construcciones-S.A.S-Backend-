package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "administradores_autorizados")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    @Column(name = "email")
    private String email;

    @Column(name = "nombre")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol = Rol.USUARIO;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Transient
    private boolean emailVerificado;

    @Column(name = "autorizacion_email", unique = true)
    private String autorizacionEmail;

    @Enumerated(EnumType.STRING)
    private Rol rolAutorizado;

    private boolean autorizacionActiva;

    private String autorizadoPor;

    private LocalDateTime autorizadoEn;

    public String getAutorizacionEmail() { return autorizacionEmail; }
    public void setAutorizacionEmail(String value) { autorizacionEmail = value; }
    public Rol getRolAutorizado() { return rolAutorizado; }
    public void setRolAutorizado(Rol value) { rolAutorizado = value; }
    public boolean isAutorizacionActiva() { return autorizacionActiva; }
    public void setAutorizacionActiva(boolean value) { autorizacionActiva = value; }
    public String getAutorizadoPor() { return autorizadoPor; }
    public void setAutorizadoPor(String value) { autorizadoPor = value; }
    public LocalDateTime getAutorizadoEn() { return autorizadoEn; }
    public void setAutorizadoEn(LocalDateTime value) { autorizadoEn = value; }

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }
}
