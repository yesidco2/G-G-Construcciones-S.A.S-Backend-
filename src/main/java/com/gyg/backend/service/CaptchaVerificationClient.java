package com.gyg.backend.service;

public interface CaptchaVerificationClient {

    boolean verify(String captchaToken, String clientIp);
}
