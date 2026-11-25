package com.gyg.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(2048))
                .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("LOW"))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"razonamiento interno","thought":true},{"text":"Evalúa primero los riesgos."}]}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        GeminiChatEngine engine = new GeminiChatEngine(
                restTemplate, "test-api-key", "gemini-3.6-flash");

        assertEquals("Evalúa primero los riesgos.",
                engine.responder("¿Qué EPP requiere esta tarea?"));
        server.verify();
    }

    @Test
    void regeneraUnaRespuestaQueAgotoLosTokens() {
        var rest = new RestTemplateBuilder().build();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(API_URL)).andRespond(withSuccess("""
                {"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"Se basa en el"}]}}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(API_URL)).andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(4096))
                .andRespond(withSuccess("""
                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Consulta al responsable SST."}]}}]}
                """, MediaType.APPLICATION_JSON));
        assertEquals("Consulta al responsable SST.", new GeminiChatEngine(rest, "test", "gemini-3.6-flash").responder("Consulta SST"));
        server.verify();
    }

    @Test
    void respondeSiGeminiSeRecuperaTrasUn503() {
        var rest = new RestTemplateBuilder().build();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(API_URL)).andRespond(withSuccess("""
                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Sí."}]}}]}
                """, MediaType.APPLICATION_JSON));
        var engine = new GeminiChatEngine(rest, "test", "gemini-3.6-flash");
        assertEquals("Sí.", engine.responder("¿Eres una IA?"));
        server.verify();
    }

    @Test
    void terminaTrasElSegundo503() {
        var rest = new RestTemplateBuilder().build();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var engine = new GeminiChatEngine(rest, "test", "gemini-3.6-flash");
        assertThrows(ChatProviderUnavailableException.class, () -> engine.responder("Consulta SST"));
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
