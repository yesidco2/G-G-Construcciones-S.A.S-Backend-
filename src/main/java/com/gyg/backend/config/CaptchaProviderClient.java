package com.gyg.backend.config;

import com.gyg.backend.service.CaptchaProviderUnavailableException;
import com.gyg.backend.service.CaptchaVerificationClient;
import com.gyg.backend.service.CaptchaFailureCode;
import com.gyg.backend.service.CaptchaVerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
public class CaptchaProviderClient implements CaptchaVerificationClient {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaProviderClient.class);
    private static final String TURNSTILE_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Set<String> INVALID_SECRET_CODES =
            Set.of("invalid-input-secret", "missing-input-secret");
    private static final Set<String> EXPIRED_TOKEN_CODES = Set.of("timeout-or-duplicate");
    private static final Set<String> INVALID_TOKEN_CODES =
            Set.of("invalid-input-response", "missing-input-response");

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
    public CaptchaVerificationResult verify(String captchaToken, String clientIp, String expectedAction) {
        String url = switch (provider) {
            case "turnstile" -> TURNSTILE_URL;
            case "recaptcha" -> RECAPTCHA_URL;
            default -> {
                throw new CaptchaProviderUnavailableException();
            }
        };
        if (secret.isBlank() || ("turnstile".equals(provider) && expectedHostnames.isEmpty())) {
            return failure(CaptchaFailureCode.CONFIGURATION_ERROR);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", captchaToken);
        if (clientIp != null && !clientIp.isBlank()) {
            form.add("remoteip", clientIp);
        }

        try {
            ResponseEntity<Map> response = restOperations.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(form, headers()), Map.class);
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                return failure(CaptchaFailureCode.PROVIDER_ERROR);
            }
            if (response.getBody() == null) {
                return failure(CaptchaFailureCode.PROVIDER_ERROR);
            }

            Map<?, ?> result = response.getBody();
            if (!Boolean.TRUE.equals(result.get("success"))) {
                return failure(classifyProviderErrors(result.get("error-codes")));
            }
            if ("turnstile".equals(provider)) {
                if (!expectedAction.equals(result.get("action"))) {
                    return failure(CaptchaFailureCode.ACTION_MISMATCH);
                }
                if (!(result.get("hostname") instanceof String hostname)
                        || !expectedHostnames.contains(hostname.toLowerCase(Locale.ROOT))) {
                    return failure(CaptchaFailureCode.HOSTNAME_MISMATCH);
                }
            }
            return CaptchaVerificationResult.accepted();
        } catch (ResourceAccessException exception) {
            CaptchaFailureCode code = isTimeout(exception)
                    ? CaptchaFailureCode.PROVIDER_TIMEOUT
                    : CaptchaFailureCode.PROVIDER_UNAVAILABLE;
            logger.warn("CAPTCHA provider request failed: {}", code);
            return failure(code);
        } catch (HttpStatusCodeException exception) {
            CaptchaFailureCode code = exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? CaptchaFailureCode.PROVIDER_UNAVAILABLE
                    : CaptchaFailureCode.PROVIDER_ERROR;
            logger.warn("CAPTCHA provider returned HTTP status {}.", exception.getStatusCode().value());
            return failure(code);
        } catch (RestClientException exception) {
            logger.warn("CAPTCHA provider response could not be processed ({})",
                    exception.getClass().getSimpleName());
            return failure(CaptchaFailureCode.PROVIDER_ERROR);
        }
    }

    private static CaptchaFailureCode classifyProviderErrors(Object rawErrors) {
        if (rawErrors instanceof Iterable<?> errors) {
            for (Object error : errors) {
                if (!(error instanceof String code)) {
                    continue;
                }
                if (INVALID_SECRET_CODES.contains(code)) {
                    return CaptchaFailureCode.CONFIGURATION_ERROR;
                }
                if (EXPIRED_TOKEN_CODES.contains(code)) {
                    return CaptchaFailureCode.TOKEN_EXPIRED_OR_DUPLICATE;
                }
                if (INVALID_TOKEN_CODES.contains(code)) {
                    return CaptchaFailureCode.INVALID_TOKEN;
                }
                if ("internal-error".equals(code)) {
                    return CaptchaFailureCode.PROVIDER_UNAVAILABLE;
                }
            }
        }
        return CaptchaFailureCode.PROVIDER_ERROR;
    }

    private static boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException
                    || cause.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
        }
        return exception.getMessage() != null
                && exception.getMessage().toLowerCase(Locale.ROOT).contains("timeout");
    }

    private static CaptchaVerificationResult failure(CaptchaFailureCode code) {
        if (code.retryable()) {
            logger.warn("CAPTCHA provider verification failed: {}", code);
        }
        return CaptchaVerificationResult.rejected(code);
    }

    private static org.springframework.http.HttpHeaders headers() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
