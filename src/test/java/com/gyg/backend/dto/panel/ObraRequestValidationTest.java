package com.gyg.backend.dto.panel;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void shouldValidateOptionalClientIdAndDateOrder() {
        ObraRequest request = new ObraRequest();
        request.setNombre("Obra");
        request.setCategoria("CIVIL");
        request.setEstado(EstadoObra.PLANIFICADA);
        request.setProgreso(0);
        request.setClienteId(0L);
        request.setFechaInicio(LocalDate.of(2026, 12, 1));
        request.setFechaFinEstimada(LocalDate.of(2026, 1, 1));

        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(request);

        assertFalse(violations.isEmpty());
        assertTrueHasViolation(violations, "clienteId");
        assertTrueHasViolation(violations, "rangoFechasValido");
    }

    @Test
    void shouldRejectClientSuppliedTrackingCode() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThrows(Exception.class, () -> objectMapper.readValue(
                "{\"nombre\":\"Obra\",\"clienteId\":1,\"categoria\":\"CIVIL\","
                        + "\"estado\":\"PLANIFICADA\",\"progreso\":0,"
                        + "\"codigo\":\"codigo-proporcionado-por-cliente\"}",
                ObraRequest.class));
    }

    private static void assertTrueHasViolation(
            java.util.Set<jakarta.validation.ConstraintViolation<ObraRequest>> violations,
            String property) {
        org.junit.jupiter.api.Assertions.assertTrue(
                violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(property)));
    }
}
