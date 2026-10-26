package com.gyg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;

import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

	@Autowired
	private AdministradorAutorizadoRepository autorizadoRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void seedsConfiguredAdminAuthorizationIdempotently() {
		var seeded = autorizadoRepository.findByEmail("seed-admin@example.com");

		assertTrue(seeded.isPresent());
		assertEquals(Rol.ADMIN, seeded.get().getRol());
		assertEquals("sistema", seeded.get().getCreadoPor());
	}

}