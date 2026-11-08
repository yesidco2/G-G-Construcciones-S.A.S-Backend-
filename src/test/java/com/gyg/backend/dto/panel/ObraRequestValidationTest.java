package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObraRequestValidationTest {

    @Test
    void shouldIgnoreClientSuppliedIdsAndRoles() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ObraRequest request = objectMapper.readValue(
                "{\"id\":77,\"rol\":\"ADMIN\",\"nombre\":\"Obra\",\"cliente\":\"Cliente\","
                        + "\"clienteId\":1,\"categoria\":\"CIVIL\","
                        + "\"estado\":\"PLANIFICADA\",\"progreso\":0}",
                ObraRequest.class);

        assertEquals("Obra", request.getNombre());
    }

    @Test
    void shouldRejectUnrecognizedProperties() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () -> objectMapper.readValue(
                "{\"nombre\":\"Obra\",\"cliente\":\"Cliente\",\"clienteId\":1,"
                        + "\"categoria\":\"CIVIL\",\"estado\":\"PLANIFICADA\",\"progreso\":0,"
                        + "\"campoDesconocido\":true}",
                ObraRequest.class));
    }
}
