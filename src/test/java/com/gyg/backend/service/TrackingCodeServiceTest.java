package com.gyg.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingCodeServiceTest {

    private static final String PEPPER = "test-pepper-for-hmac-must-be-at-least-32";

    private final TrackingCodeService service = new TrackingCodeService(PEPPER);

    @Test
    void shouldGenerateHighEntropyCodesAndStoreOnlyTheirHmac() {
        TrackingCodeService.GeneratedTrackingCode generated = service.generate();

        assertTrue(generated.value().matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{32}"));
        assertEquals(64, generated.hash().length());
        assertFalse(generated.hash().contains(generated.value()));
        assertEquals(service.hash(generated.value()), generated.hash());
    }

    @Test
    void shouldVerifyCodesUsingTheirHmac() {
        TrackingCodeService.GeneratedTrackingCode generated = service.generate();

        assertTrue(service.matches(generated.value(), generated.hash()));
        assertFalse(service.matches(generated.value() + "X", generated.hash()));
        assertFalse(service.matches(generated.value(), "not-a-hash"));
    }
}
