package com.gyg.backend.model;

import java.time.LocalDate;

public record HitoObra(Long id, String nombre, String descripcion, LocalDate fechaObjetivo, boolean completado) {}
