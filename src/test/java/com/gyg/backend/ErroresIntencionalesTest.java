package com.gyg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ErroresIntencionalesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void errorIntencionalInstitucional() throws Exception {
        mockMvc.perform(get("/api/institucional"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void errorIntencionalServicios() throws Exception {
        mockMvc.perform(get("/api/servicios"))
                .andExpect(status().isCreated());
    }

    @Test
    void errorIntencionalRutaInexistente() throws Exception {
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isOk());
    }
}
