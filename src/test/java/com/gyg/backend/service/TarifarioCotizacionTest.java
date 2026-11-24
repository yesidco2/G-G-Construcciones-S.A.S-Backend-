package com.gyg.backend.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Transactional
class TarifarioCotizacionTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 10);
    private static final String SERVICIO = "interventoria";
    @Autowired private JdbcTemplate db;
    @Autowired private TarifarioCotizacion tarifario;

    @Test
    void priorizaLaTarifaAprobadaYLocalDentroDeSuPeriodoYUnidad() {
        tarifa("REFERENCIA", "Ciudad Prueba", 10, "servicio completo", FECHA, FECHA, true);
        assertThat(precio("Ciudad Prueba").tipo()).isEqualTo("REFERENCIA");
        assertThat(precio("Otra Ciudad")).isNull();
        tarifa("APROBADA", "*", 20, "servicio completo", FECHA, null, true);
        assertThat(precio("Ciudad Prueba").precioUnitario()).isEqualByComparingTo("20");
        tarifa("APROBADA", "Ciudad Prueba", 30, "servicio completo", FECHA, FECHA, true);
        assertThat(precio(" ciudad prueba ").precioUnitario()).isEqualByComparingTo("30");
        assertThat(precio("Otra Ciudad").ubicacion()).isEqualTo("Colombia");

        tarifa("APROBADA", "Ciudad Prueba", 40, "hora", FECHA, null, true);
        tarifa("APROBADA", "Ciudad Prueba", 50, "servicio completo", FECHA.plusDays(1), null, true);
        tarifa("APROBADA", "Ciudad Prueba", 60, "servicio completo", FECHA.minusDays(3), FECHA.minusDays(1), true);
        tarifa("APROBADA", "Ciudad Prueba", 70, "servicio completo", FECHA.minusDays(7), null, false);
        assertThat(precio("Ciudad Prueba").precioUnitario()).isEqualByComparingTo("30");
        db.update("UPDATE apoyo.cotizador_servicios SET unidad = 'visita' WHERE id = ?", SERVICIO);
        assertThat(precio("Ciudad Prueba")).isNull();
    }

    @Test
    void usaSoloLasCondicionesVigentesAplicablesAlMunicipio() {
        assertThat(tarifario.configuracion("Ciudad Prueba", FECHA).impuestoPorcentaje()).isNull();
        db.update("""
                INSERT INTO apoyo.cotizador_condiciones
                    (ubicacion, vigente_desde, vigente_hasta, dias_vigencia, desplazamiento, activa)
                VALUES ('Ciudad Prueba', ?, ?, 15, 1234, TRUE)
                """, FECHA, FECHA);
        var local = tarifario.configuracion("CIUDAD PRUEBA", FECHA);
        assertThat(local.diasVigencia()).isEqualTo(15);
        assertThat(local.desplazamiento()).isEqualByComparingTo("1234");
        assertThat(local.descuentoPorcentaje()).isNull();
        assertThat(tarifario.configuracion("Otra Ciudad", FECHA).desplazamiento()).isNull();
        assertThat(tarifario.configuracion("Ciudad Prueba", FECHA.plusDays(1)).desplazamiento()).isNull();
    }

    private TarifarioCotizacion.Tarifa precio(String ciudad) {
        return tarifario.servicios(ciudad, FECHA).stream().filter(s -> SERVICIO.equals(s.id()))
                .findFirst().orElseThrow().tarifa();
    }

    private void tarifa(String tipo, String ciudad, int precio, String unidad, LocalDate desde, LocalDate hasta,
                        boolean activa) {
        db.update("""
                INSERT INTO apoyo.cotizador_tarifas (servicio_id, unidad, tipo, precio_unitario, fuente,
                    fecha_fuente, ubicacion, alcance, vigente_desde, vigente_hasta, activa)
                VALUES (?, ?, ?, ?, 'Fuente de prueba', ?, ?, 'Alcance de prueba', ?, ?, ?)
                """, SERVICIO, unidad, tipo, precio, desde, ciudad, desde, hasta, activa);
    }
}
