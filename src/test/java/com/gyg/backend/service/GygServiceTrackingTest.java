package com.gyg.backend.service;

import com.gyg.backend.exception.ResourceNotFoundException;
import com.gyg.backend.model.FotoObra;
import com.gyg.backend.model.Obra;
import com.gyg.backend.repository.FotoObraRepository;
import com.gyg.backend.repository.ObraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GygServiceTrackingTest {

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private FotoObraRepository fotoObraRepository;

    @Mock
    private TrackingCodeService trackingCodeService;

    @Mock
    private ObraPhotoStorageService photoStorage;

    private GygService service;

    @BeforeEach
    void setUp() {
        service = new GygService();
        ReflectionTestUtils.setField(service, "obraRepository", obraRepository);
        ReflectionTestUtils.setField(service, "fotoObraRepository", fotoObraRepository);
        ReflectionTestUtils.setField(service, "trackingCodeService", trackingCodeService);
        ReflectionTestUtils.setField(service, "photoStorage", photoStorage);
        ReflectionTestUtils.setField(service, "legacyTrackingCodesEnabled", true);
    }

    @Test
    void shouldRequireTheWorkCodeToReadAStoredPhoto() throws Exception {
        String code = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        Obra obra = new Obra("Proyecto", "EN_PROGRESO", 1L, 50);
        obra.setCodigoHash("stored-hash");
        FotoObra photo = new FotoObra("pending", LocalDate.now(), obra);
        photo.setId(7L);
        photo.setStorageKey("01234567-89ab-cdef-0123-456789abcdef.png");
        when(trackingCodeService.hash(code)).thenReturn("stored-hash");
        when(trackingCodeService.matches(code, "stored-hash")).thenReturn(true);
        when(obraRepository.findByCodigoHashWithFotosAndHitos("stored-hash"))
                .thenReturn(Optional.of(obra));
        when(fotoObraRepository.findByIdAndObra_CodigoHashAndStorageKeyIsNotNull(7L, "stored-hash"))
                .thenReturn(Optional.of(photo));
        when(photoStorage.read(photo.getStorageKey())).thenReturn(new byte[] {1, 2, 3});

        GygService.StoredImage image = service.getSeguimientoFoto(code, 7L);

        assertEquals("image/png", image.contentType());
        assertEquals(3, image.content().length);
    }

    @Test
    void shouldSupportLegacyCodesOnlyWhileCompatibilityIsEnabled() {
        String legacyCode = "LEGACY-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Obra obra = new Obra("Proyecto legado", "EN_PROGRESO", 1L, 50);
        obra.setCodigoHash("legacy-hash");
        obra.setCodigoLegacy(true);
        when(trackingCodeService.hash(legacyCode)).thenReturn("legacy-hash");
        when(trackingCodeService.matches(legacyCode, "legacy-hash")).thenReturn(true);
        when(obraRepository.findByCodigoHashWithFotosAndHitos("legacy-hash"))
                .thenReturn(Optional.of(obra));

        assertEquals("Proyecto legado", service.getSeguimientoProyecto(legacyCode).getNombre());

        ReflectionTestUtils.setField(service, "legacyTrackingCodesEnabled", false);
        assertThrows(ResourceNotFoundException.class, () -> service.getSeguimientoProyecto(legacyCode));
    }
}
