package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatEngineTest {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

    @Test
    void shouldSendOnlyTheCurrentQuestionAndExtractTheProviderAnswer() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("¿Qué EPP requiere esta tarea?"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(700))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[{"text":"razonamiento interno","thought":true},{"text":"Evalúa primero los riesgos."}]}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        GeminiChatEngine engine = new GeminiChatEngine(
                restTemplate, "test-api-key", "gemini-3.6-flash");

        assertEquals("Evalúa primero los riesgos.",
                engine.responder("¿Qué EPP requiere esta tarea?"));
        server.verify();
    }

    @Test
    void shouldReportProviderErrorsWithoutReturningAKeywordFallback() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(API_URL))
                .andRespond(withServerError());
        GeminiChatEngine engine = new GeminiChatEngine(
                restTemplate, "test-api-key", "gemini-3.6-flash");

        assertThrows(ChatProviderUnavailableException.class,
                () -> engine.responder("Consulta SST"));
        server.verify();
    }
}
