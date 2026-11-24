package com.gyg.backend.service;

public class SecurityStateUnavailableException extends RuntimeException {

    public SecurityStateUnavailableException(Throwable cause) {
        super("No se pudo consultar o actualizar el estado de autenticación.", cause);
    }
}
