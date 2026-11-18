package com.gyg.backend.service;

import org.springframework.http.HttpStatus;

public enum CaptchaFailureCode {
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "CAPTCHA_INVALID", false),
    TOKEN_EXPIRED_OR_DUPLICATE(HttpStatus.BAD_REQUEST, "CAPTCHA_TOKEN_EXPIRED_OR_DUPLICATE", false),
    TOKEN_REUSED(HttpStatus.BAD_REQUEST, "CAPTCHA_TOKEN_REUSED", false),
    HOSTNAME_MISMATCH(HttpStatus.BAD_REQUEST, "CAPTCHA_HOSTNAME_MISMATCH", false),
    ACTION_MISMATCH(HttpStatus.BAD_REQUEST, "CAPTCHA_ACTION_MISMATCH", false),
    CONFIGURATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_CONFIGURATION_ERROR", true),
    PROVIDER_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_PROVIDER_TIMEOUT", true),
    PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_PROVIDER_UNAVAILABLE", true),
    PROVIDER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_PROVIDER_ERROR", true),
    STATE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_STATE_UNAVAILABLE", true);

    private final HttpStatus httpStatus;
    private final String responseCode;
    private final boolean retryable;

    CaptchaFailureCode(HttpStatus httpStatus, String responseCode, boolean retryable) {
        this.httpStatus = httpStatus;
        this.responseCode = responseCode;
        this.retryable = retryable;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String responseCode() {
        return responseCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
