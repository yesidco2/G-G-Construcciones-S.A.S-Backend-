package com.gyg.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseAuth {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final boolean configured;

    public SupabaseAuth(ObjectMapper mapper,
            @Value("${app.supabase.url:}") String url,
            @Value("${app.supabase.publishable-key:}") String key) {
        this.mapper = mapper;
        this.configured = !url.isBlank() && !key.isBlank();
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().requestFactory(factory)
                .baseUrl(url.replaceFirst("/+$", "") + "/auth/v1")
                .defaultHeader("apikey", key).build();
    }

    public Identity verify(String token) throws java.io.IOException {
        if (!configured) throw new IllegalStateException("Supabase Auth no está configurado.");
        JsonNode user = client.get().uri("/user").headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(JsonNode.class);
        // Supabase has authenticated this exact token before we read its session claims.
        JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        String uid = UUID.fromString(user.path("id").asText()).toString();
        if (!uid.equals(payload.path("sub").asText()) || !"authenticated".equals(payload.path("role").asText())
                || user.path("is_anonymous").asBoolean()) {
            throw new IllegalArgumentException("Identidad de Supabase no válida.");
        }
        String sessionId = UUID.fromString(payload.path("session_id").asText()).toString();
        Instant expiration = Instant.ofEpochSecond(payload.path("exp").asLong());
        if (!expiration.isAfter(Instant.now())) throw new IllegalArgumentException("Token vencido.");
        JsonNode confirmedAt = user.path("email_confirmed_at");
        return new Identity(uid, user.path("email").asText(null), confirmedAt.isTextual(),
                sessionId, expiration);
    }

    public void recover(String email) {
        if (!configured) throw new IllegalStateException("Supabase Auth no está configurado.");
        client.post().uri("/recover").body(Map.of("email", email)).retrieve().toBodilessEntity();
    }

    public record Identity(String uid, String email, boolean emailVerified, String sessionId, Instant expiration) {}
}
