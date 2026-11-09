package com.gyg.backend.config;

import com.gyg.backend.service.CaptchaProviderUnavailableException;
import com.gyg.backend.service.CaptchaVerificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

@Component
public class CaptchaProviderClient implements CaptchaVerificationClient {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaProviderClient.class);
    private static final String TURNSTILE_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestOperations restOperations;
    private final String provider;
    private final String secret;

    public CaptchaProviderClient(
            RestOperations restOperations,
            @Value("${app.captcha.provider:turnstile}") String provider,
            @Value("${app.captcha.secret:}") String secret) {
        this.restOperations = restOperations;
        this.provider = provider;
        this.secret = secret;
    }

    @Override
    public boolean verify(String captchaToken, String clientIp) {
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
            Map<?, ?> result = restOperations.postForObject(
                    url, new org.springframework.http.HttpEntity<>(form, headers()), Map.class);
            return result != null && Boolean.TRUE.equals(result.get("success"));
        } catch (RestClientException exception) {
            logger.warn("CAPTCHA provider verification request failed due to {}.",
                    exception.getClass().getSimpleName());
            throw new CaptchaProviderUnavailableException();
        }
    }

    private static org.springframework.http.HttpHeaders headers() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
