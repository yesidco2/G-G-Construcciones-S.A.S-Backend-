package com.gyg.backend.config;

import com.gyg.backend.service.CaptchaProviderUnavailableException;
import com.gyg.backend.service.CaptchaVerificationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class CaptchaProviderClient implements CaptchaVerificationClient {

    private static final String TURNSTILE_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestOperations restOperations;
    private final String provider;
    private final String secret;
    private final Set<String> expectedHostnames;

    public CaptchaProviderClient(
            RestOperations restOperations,
            @Value("${app.captcha.provider:turnstile}") String provider,
            @Value("${app.captcha.secret:}") String secret,
            @Value("${app.captcha.expected-hostnames:}") String expectedHostnames) {
        this.restOperations = restOperations;
        this.provider = provider;
        this.secret = secret;
        this.expectedHostnames = Arrays.stream(expectedHostnames.split(","))
                .map(String::trim)
                .map(hostname -> hostname.toLowerCase(Locale.ROOT))
                .filter(hostname -> !hostname.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean verify(String captchaToken, String clientIp, String expectedAction) {
        String url = switch (provider) {
            case "turnstile" -> TURNSTILE_URL;
            case "recaptcha" -> RECAPTCHA_URL;
            default -> throw new CaptchaProviderUnavailableException();
        };

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", captchaToken);
        if (clientIp != null && !clientIp.isBlank()) {
            form.add("remoteip", clientIp);
        }

        try {
            ResponseEntity<Map> response = restOperations.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(form, headers()), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return false;
            }
            Map<?, ?> result = response.getBody();
            if (!Boolean.TRUE.equals(result.get("success"))) {
                return false;
            }
            return !"turnstile".equals(provider)
                    || (expectedAction.equals(result.get("action"))
                    && result.get("hostname") instanceof String hostname
                    && expectedHostnames.contains(hostname.toLowerCase(Locale.ROOT)));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static org.springframework.http.HttpHeaders headers() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
