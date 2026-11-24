package com.gyg.backend.service;

public class CaptchaProviderUnavailableException extends CaptchaVerificationException {

    public CaptchaProviderUnavailableException() {
        super(CaptchaFailureCode.CONFIGURATION_ERROR);
    }
}
