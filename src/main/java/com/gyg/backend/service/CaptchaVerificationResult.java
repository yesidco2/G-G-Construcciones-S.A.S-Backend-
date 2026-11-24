package com.gyg.backend.service;

public record CaptchaVerificationResult(boolean successful, CaptchaFailureCode failureCode) {

    public static CaptchaVerificationResult accepted() {
        return new CaptchaVerificationResult(true, null);
    }

    public static CaptchaVerificationResult rejected(CaptchaFailureCode failureCode) {
        return new CaptchaVerificationResult(false, failureCode);
    }
}
