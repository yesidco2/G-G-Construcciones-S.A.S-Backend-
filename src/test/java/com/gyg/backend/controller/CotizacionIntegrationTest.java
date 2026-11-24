package com.gyg.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
@Transactional
class CotizacionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    private ObjectNode solicitud() {
        var body = json.createObjectNode();
        body.put("nombre", "Cliente de prueba").put("correo", "prueba@example.com")
                .put("telefono", "3000000000").put("ciudad", "Ciudad Prueba")
                .put("descripcion", "Proyecto ficticio para verificar la cotización");
        var items = body.putArray("items");
        db.queryForList("SELECT id FROM apoyo.cotizador_servicios WHERE activo ORDER BY orden", String.class)
                .forEach(id -> items.addObject().put("servicioId", id).put("cantidad", 2));
        return body;
    }

    @Test
    void generaUnPdfPaginadoYRevisaLaTarifaAntesDeDescargar() throws Exception {
        String body = solicitud().toString();
        db.update("""
                INSERT INTO apoyo.cotizador_tarifas
                    (servicio_id, unidad, tipo, precio_unitario, fuente, fecha_fuente, ubicacion, alcance,
                     vigente_desde, vigente_hasta, activa)
                VALUES ('interventoria','servicio completo','REFERENCIA',12500,'Fuente de prueba',
                    CURRENT_DATE,'Ciudad Prueba','Prueba local',CURRENT_DATE,CURRENT_DATE,TRUE)
                """);
        var result = mvc.perform(post("/api/cotizaciones/vista-previa").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(25000))
                .andExpect(jsonPath("$.preliminar").value(true)).andReturn();
        String revision = json.readTree(result.getResponse().getContentAsByteArray()).get("referencia").toString();
        var pdf = mvc.perform(post("/api/cotizaciones/pdf").header("If-Match", revision)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var reader = new PdfReader(pdf)) {
            assertThat(reader.getCatalog().getAsString(PdfName.LANG).toUnicodeString()).isEqualTo("es-CO");
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            var extractor = new PdfTextExtractor(reader);
            assertThat(extractor.getTextFromPage(1)).contains("Estimación preliminar", "Descripción");
            assertThat(extractor.getTextFromPage(2)).contains("Descripción");
        }
        db.update("UPDATE apoyo.cotizador_tarifas SET precio_unitario = 13000 WHERE fuente = 'Fuente de prueba'");
        mvc.perform(post("/api/cotizaciones/pdf").header("If-Match", revision)
                .contentType("application/json").content(body)).andExpect(status().isConflict());
    }

    @Test
    void rechazaCantidadInvalidaYPrecioEnviadoPorElNavegador() throws Exception {
        var body = solicitud();
        var item = (ObjectNode) body.get("items").get(0);
        item.put("cantidad", 0);
        mvc.perform(post("/api/cotizaciones/pdf").contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
        item.put("cantidad", 1).put("precioUnitario", 1);
        mvc.perform(post("/api/cotizaciones/vista-previa").contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
    }
}
