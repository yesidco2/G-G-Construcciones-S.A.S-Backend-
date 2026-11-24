package com.gyg.backend.service;

import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.Obra;
import com.gyg.backend.repository.ObraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GygServiceTrackingTest {

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private TrackingCodeService trackingCodeService;

    private GygService service;

    @BeforeEach
    void setUp() {
        service = new GygService();
        ReflectionTestUtils.setField(service, "documentos", org.mockito.Mockito.mock(DocumentosObraService.class));
        ReflectionTestUtils.setField(service, "obraRepository", obraRepository);
        ReflectionTestUtils.setField(service, "trackingCodeService", trackingCodeService);
        ReflectionTestUtils.setField(service, "legacyTrackingCodesEnabled", true);
    }

    @Test
    void shouldSupportLegacyCodesOnlyWhileCompatibilityIsEnabled() {
        String legacyCode = "LEGACY-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Obra obra = new Obra("Proyecto legado", "EN_PROGRESO", 1L, 50);
        obra.setId(1L);
        obra.setCodigoHash("legacy-hash");
        obra.setCodigoLegacy(true);
        when(trackingCodeService.hash(legacyCode)).thenReturn("legacy-hash");
        when(trackingCodeService.matches(legacyCode, "legacy-hash")).thenReturn(true);
        when(obraRepository.findByCodigoHashWithHitos("legacy-hash"))
                .thenReturn(Optional.of(obra));

        assertEquals("Proyecto legado", service.getSeguimientoProyecto(legacyCode).getNombre());

        ReflectionTestUtils.setField(service, "legacyTrackingCodesEnabled", false);
        assertThrows(ResourceNotFoundException.class, () -> service.getSeguimientoProyecto(legacyCode));
    }
}
