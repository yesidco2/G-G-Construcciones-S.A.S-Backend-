package com.gyg.backend.service;

public class CaptchaVerificationException extends RuntimeException {

    private final CaptchaFailureCode failureCode;

    public CaptchaVerificationException(CaptchaFailureCode failureCode) {
        super("La verificación CAPTCHA no pudo completarse.");
        this.failureCode = failureCode;
    }

    public CaptchaVerificationException(CaptchaFailureCode failureCode, Throwable cause) {
        super("La verificación CAPTCHA no pudo completarse.", cause);
        this.failureCode = failureCode;
    }

    public CaptchaFailureCode getFailureCode() {
        return failureCode;
    }
}
