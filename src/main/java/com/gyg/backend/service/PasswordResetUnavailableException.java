package com.gyg.backend.service;

public class PasswordResetUnavailableException extends RuntimeException {

    public PasswordResetUnavailableException() {
        super("El servicio de recuperación de contraseña no está disponible.");
    }
}
