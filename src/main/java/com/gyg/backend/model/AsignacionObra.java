package com.gyg.backend.model;

import java.time.LocalDateTime;

public record AsignacionObra(Long usuarioId, String asignadoPor, LocalDateTime asignadoEn) {}
