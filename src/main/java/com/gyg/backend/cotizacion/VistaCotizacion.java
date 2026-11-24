package com.gyg.backend.cotizacion;

import com.gyg.backend.service.TarifarioCotizacion.Servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VistaCotizacion(LocalDate fecha, String referencia, Empresa empresa, Cliente cliente,
                             boolean preliminar, List<Linea> items, BigDecimal subtotal, int pendientes,
                             List<Ajuste> ajustes, BigDecimal total, Integer diasVigencia,
                             String condiciones, String alcance) {
    public record Empresa(String nombre, String telefono, String correo, String ubicacion, String nit, String direccion) {}
    public record Cliente(String nombre, String correo, String telefono, String ciudad, String descripcion,
                          String nit, String direccion) {}
    public record Linea(Servicio servicio, BigDecimal cantidad, BigDecimal total) {}
    public record Ajuste(String nombre, BigDecimal valor) {}
}
