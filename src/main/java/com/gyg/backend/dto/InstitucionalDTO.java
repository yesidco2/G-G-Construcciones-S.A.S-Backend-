package com.gyg.backend.dto;

import java.util.List;

public class InstitucionalDTO {
    private String mision;
    private String vision;
    private List<String> valores;

    public InstitucionalDTO() {
    }

    public InstitucionalDTO(String mision, String vision, List<String> valores) {
        this.mision = mision;
        this.vision = vision;
        this.valores = valores;
    }

    public String getMision() {
        return mision;
    }

    public void setMision(String mision) {
        this.mision = mision;
    }

    public String getVision() {
        return vision;
    }

    public void setVision(String vision) {
        this.vision = vision;
    }

    public List<String> getValores() {
        return valores;
    }

    public void setValores(List<String> valores) {
        this.valores = valores;
    }
}
