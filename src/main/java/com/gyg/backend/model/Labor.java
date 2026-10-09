package com.gyg.backend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "labores")
public class Labor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @OneToMany(mappedBy = "labor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Riesgo> riesgos = new ArrayList<>();

    @OneToMany(mappedBy = "labor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EPP> epps = new ArrayList<>();

    public Labor() {
    }

    public Labor(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Riesgo> getRiesgos() {
        return riesgos;
    }

    public void setRiesgos(List<Riesgo> riesgos) {
        this.riesgos = riesgos;
    }

    public List<EPP> getEpps() {
        return epps;
    }

    public void setEpps(List<EPP> epps) {
        this.epps = epps;
    }

    public void addRiesgo(Riesgo riesgo) {
        riesgos.add(riesgo);
        riesgo.setLabor(this);
    }

    public void removeRiesgo(Riesgo riesgo) {
        riesgos.remove(riesgo);
        riesgo.setLabor(null);
    }

    public void addEpp(EPP epp) {
        epps.add(epp);
        epp.setLabor(this);
    }

    public void removeEpp(EPP epp) {
        epps.remove(epp);
        epp.setLabor(null);
    }
}
