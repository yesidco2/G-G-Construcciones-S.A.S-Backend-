package com.gyg.backend.dto;

import java.time.LocalDate;

public record FotoObraDTO(Long id, String nombre, String url, LocalDate fecha, String descripcion) {}
