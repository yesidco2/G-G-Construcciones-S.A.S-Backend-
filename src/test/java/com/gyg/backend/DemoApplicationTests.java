package com.gyg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.gyg.backend.model.Rol;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;

import org.springframework.test.context.ActiveProfiles;
import org.flywaydb.core.Flyway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
class DemoApplicationTests {

	@Autowired
	private AdministradorAutorizadoRepository autorizadoRepository;

	@Autowired
	private Flyway flyway;

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

	@Test
	void appliesInitialFlywaySchemaBeforeHibernateValidation() {
		assertTrue(java.util.Arrays.stream(flyway.info().applied())
				.anyMatch(migration -> migration.getVersion() != null
						&& "1".equals(migration.getVersion().getVersion())));
	}

}