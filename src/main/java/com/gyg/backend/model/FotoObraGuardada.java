package com.gyg.backend.model;

import java.time.LocalDate;

public record FotoObraGuardada(Long id, String nombre, LocalDate fecha, String descripcion, String subidoPor) {}
