package com.gyg.backend.service;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(Throwable cause) {
        super("No fue posible procesar el correo electrónico.", cause);
    }

    public EmailDeliveryException() {
        super("No fue posible procesar el correo electrónico.");
    }
}
