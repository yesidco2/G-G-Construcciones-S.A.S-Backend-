package com.gyg.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TarifarioCotizacion {
    public record Tarifa(String tipo, BigDecimal precioUnitario, String fuente, LocalDate fechaFuente,
                         String ubicacion, String alcance) {}
    public record Servicio(String id, String categoria, String nombre, String descripcion, String unidad,
                           Tarifa tarifa) {}
    public record Configuracion(Integer diasVigencia, String condiciones, String alcance, BigDecimal desplazamiento,
                                BigDecimal descuentoPorcentaje, BigDecimal impuestoPorcentaje) {}

    private final JdbcTemplate db;

    public TarifarioCotizacion(JdbcTemplate db) {
        this.db = db;
    }

    public List<Servicio> servicios(String ciudad, LocalDate fecha) {
        return db.query("""
                SELECT s.id, s.categoria, s.nombre, s.descripcion, s.unidad,
                       t.tipo, t.precio_unitario, t.fuente, t.fecha_fuente, t.ubicacion, t.alcance
                FROM apoyo.cotizador_servicios s
                LEFT JOIN apoyo.cotizador_tarifas t ON t.id = (
                    SELECT p.id FROM apoyo.cotizador_tarifas p
                    WHERE p.servicio_id = s.id AND p.unidad = s.unidad AND p.activa
                      AND p.vigente_desde <= ? AND (p.vigente_hasta IS NULL OR p.vigente_hasta >= ?)
                      AND (p.ubicacion = '*' OR LOWER(TRIM(p.ubicacion)) = LOWER(TRIM(?)))
                    ORDER BY CASE WHEN p.tipo = 'APROBADA' THEN 0 ELSE 1 END,
                             CASE WHEN p.ubicacion = '*' THEN 1 ELSE 0 END,
                             p.vigente_desde DESC, p.id DESC
                    FETCH FIRST 1 ROW ONLY
                )
                WHERE s.activo ORDER BY s.orden, s.id
                """, (rs, index) -> {
            Tarifa tarifa = null;
            if (rs.getString("tipo") != null) {
                String ubicacion = rs.getString("ubicacion");
                tarifa = new Tarifa(rs.getString("tipo"), rs.getBigDecimal("precio_unitario"),
                        rs.getString("fuente"), rs.getObject("fecha_fuente", LocalDate.class),
                        "*".equals(ubicacion) ? "Colombia" : ubicacion, rs.getString("alcance"));
            }
            return new Servicio(rs.getString("id"), rs.getString("categoria"), rs.getString("nombre"),
                    rs.getString("descripcion"), rs.getString("unidad"), tarifa);
        }, fecha, fecha, ciudad);
    }

    public Configuracion configuracion(String ciudad, LocalDate fecha) {
        return db.query("""
                SELECT dias_vigencia, condiciones, alcance, desplazamiento, descuento_porcentaje, impuesto_porcentaje
                FROM apoyo.cotizador_condiciones
                WHERE activa AND vigente_desde <= ? AND (vigente_hasta IS NULL OR vigente_hasta >= ?)
                  AND (ubicacion = '*' OR LOWER(TRIM(ubicacion)) = LOWER(TRIM(?)))
                ORDER BY CASE WHEN ubicacion = '*' THEN 1 ELSE 0 END, vigente_desde DESC, id DESC
                FETCH FIRST 1 ROW ONLY
                """, (rs, index) -> new Configuracion(rs.getObject("dias_vigencia", Integer.class),
                rs.getString("condiciones"), rs.getString("alcance"), rs.getBigDecimal("desplazamiento"),
                rs.getBigDecimal("descuento_porcentaje"), rs.getBigDecimal("impuesto_porcentaje")),
                fecha, fecha, ciudad).stream().findFirst()
                .orElseGet(() -> new Configuracion(null, null, null, null, null, null));
    }
}
