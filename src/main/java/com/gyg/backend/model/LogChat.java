package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs_chat")
public class LogChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(nullable = false, length = 2000)
    private String pregunta;

    @Column(nullable = false, length = 4000)
    private String respuesta;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "acepta_envio_proveedor", nullable = false)
    private boolean aceptaEnvioProveedor;

    @Column(name = "version_politica", length = 50)
    private String versionPolitica;

    @Column(name = "fecha_consentimiento")
    private LocalDateTime fechaConsentimiento;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public boolean isAceptaEnvioProveedor() {
        return aceptaEnvioProveedor;
    }

    public void setAceptaEnvioProveedor(boolean aceptaEnvioProveedor) {
        this.aceptaEnvioProveedor = aceptaEnvioProveedor;
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
