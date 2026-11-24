package com.gyg.backend.dto.panel;

public enum EstadoObra {
    PLANIFICADA,
    EN_PROGRESO,
    PAUSADA,
    FINALIZADA;

    public boolean permiteTransicionA(EstadoObra siguiente) {
        return switch (this) {
            case PLANIFICADA -> siguiente == EN_PROGRESO || siguiente == PAUSADA;
            case EN_PROGRESO -> siguiente == PAUSADA || siguiente == FINALIZADA;
            case PAUSADA -> siguiente == EN_PROGRESO || siguiente == FINALIZADA;
            case FINALIZADA -> false;
        };
    }
}
