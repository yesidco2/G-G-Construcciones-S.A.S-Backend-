package com.gyg.backend.service;

public class CaptchaProviderUnavailableException extends RuntimeException {

    public CaptchaProviderUnavailableException() {
        super("El proveedor CAPTCHA no está disponible.");
    }
}
