package com.gyg.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ContactoRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    private String empresa;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "El correo debe ser un email valido")
    private String correo;

    private String telefono;

    private String tipoServicio;

    @NotBlank(message = "El mensaje es requerido")
    private String mensaje;

    public ContactoRequest() {
    }

    public ContactoRequest(String nombre, String empresa, String correo, String telefono, String tipoServicio, String mensaje) {
        this.nombre = nombre;
        this.empresa = empresa;
        this.correo = correo;
        this.telefono = telefono;
        this.tipoServicio = tipoServicio;
        this.mensaje = mensaje;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
