package com.gyg.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.service.VerifiedSessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class VerifiedSessionFilterTest {

    @Test
    void shouldRejectPanelAccessWithoutVerifiedSession() throws Exception {
        VerifiedSessionService sessions = mock(VerifiedSessionService.class);
        VerifiedSessionFilter filter = new VerifiedSessionFilter(sessions, new ObjectMapper(), true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/panel/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"CAPTCHA_REQUIRED\""));
        verifyNoInteractions(chain);
    }
}
