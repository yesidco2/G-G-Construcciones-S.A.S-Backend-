package com.gyg.backend.service;

import com.gyg.backend.model.AdministradorAutorizado;
import com.gyg.backend.model.Rol;
import com.gyg.backend.model.Usuario;
import com.gyg.backend.repository.AdministradorAutorizadoRepository;
import com.gyg.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Transactional
class UnifiedAccountsTest {
    @Autowired UsuarioRepository users;
    @Autowired AdministradorAutorizadoRepository authorizations;
    @Autowired UsuarioService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void verifiedLoginClaimsPendingAuthorizationWithoutCreatingAnotherRow() {
        AdministradorAutorizado authorization = authorize("pending@example.com");
        assertTrue(users.findAllByOrderByEmailAsc().stream()
                .noneMatch(user -> user.getId().equals(authorization.getId())));
        long before = users.count();
        Usuario result = service.obtenerOCrear("verified-pending", "pending@example.com", true, Map.of());
        assertEquals(authorization.getId(), result.getId());
        assertEquals(Rol.ADMIN, result.getRol());
        assertEquals(before, users.count());
        assertEquals("verified-pending", users.findById(result.getId()).orElseThrow().getFirebaseUid());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE LOWER(table_name) = 'usuarios'", Integer.class));
    }

    @Test
    void unverifiedTokenCannotClaimOrPromotePendingAccount() {
        AdministradorAutorizado authorization = authorize("unverified@example.com");
        Usuario result = service.obtenerOCrear("unverified-uid", "unverified@example.com", false, Map.of());
        assertEquals(Rol.USUARIO, result.getRol());
        assertNull(users.findById(authorization.getId()).orElseThrow().getFirebaseUid());
        assertTrue(users.findByFirebaseUid("unverified-uid").isEmpty());
    }

    @Test
    void revokingAuthorizationKeepsTheAccountAndRemovesPrivilegesOnNextToken() {
        AdministradorAutorizado authorization = authorize("revoked@example.com");
        Usuario user = service.obtenerOCrear("revoked-uid", "revoked@example.com", true, Map.of());
        authorizations.delete(authorization);
        users.flush();
        Usuario result = service.obtenerOCrear("revoked-uid", "revoked@example.com", true, Map.of());
        assertEquals(user.getId(), result.getId());
        assertEquals(Rol.USUARIO, result.getRol());
        assertTrue(authorizations.findByEmail("revoked@example.com").isEmpty());
    }

    @Test
    void authorizingExistingUserKeepsTheirIdAndDisabledState() {
        Usuario user = new Usuario();
        user.setFirebaseUid("disabled-uid");
        user.setEmail("disabled@example.com");
        user.setActivo(false);
        users.saveAndFlush(user);
        AdministradorAutorizado authorization = authorize("disabled@example.com");
        assertEquals(user.getId(), authorization.getId());
        assertFalse(users.findById(user.getId()).orElseThrow().isActivo());
    }

    private AdministradorAutorizado authorize(String email) {
        AdministradorAutorizado authorization = new AdministradorAutorizado();
        authorization.setEmail(email);
        authorization.setRol(Rol.ADMIN);
        authorization.setCreadoPor("test");
        return authorizations.saveAndFlush(authorization);
    }
}
