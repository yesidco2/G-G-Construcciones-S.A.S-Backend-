package com.gyg.backend.service;

public interface CaptchaVerificationClient {

    CaptchaVerificationResult verify(String captchaToken, String clientIp, String expectedAction);
}
