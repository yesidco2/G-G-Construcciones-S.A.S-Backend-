package com.gyg.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_uid", nullable = false)
    private String actorUid;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "obra_id")
    private Long obraId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "ip", length = 64)
    private String ip;

    public AuditoriaEvento() {
    }

    public AuditoriaEvento(String actorUid, String accion, Long obraId, String ip) {
        this.actorUid = actorUid;
        this.accion = accion;
        this.obraId = obraId;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }
}
