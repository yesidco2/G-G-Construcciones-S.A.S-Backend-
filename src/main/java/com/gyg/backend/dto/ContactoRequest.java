package com.gyg.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ContactoRequest {

    public static final List<String> TIPOS_SERVICIO_PERMITIDOS = List.of(
            "Obra Civil",
            "Consultoría SST",
            "Interventoría Técnica",
            "Capacitación Integral"
    );

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "El nombre contiene caracteres no permitidos")
    private String nombre;

    @Size(max = 120, message = "La empresa no puede superar 120 caracteres")
    @Pattern(regexp = "^[^<>]*$", message = "La empresa contiene caracteres no permitidos")
    private String empresa;

    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    @Email(message = "El correo debe ser un email valido")
    @Pattern(
            regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo debe incluir un dominio valido"
    )
    private String correo;

    @Pattern(regexp = "^$|^\\+?[0-9][0-9 ()-]{6,19}$", message = "El telefono no tiene un formato valido")
    private String telefono;

    @NotBlank(message = "El tipo de servicio es requerido")
    private String tipoServicio;

    @NotBlank(message = "El mensaje es requerido")
    @Size(min = 10, max = 1500, message = "El mensaje debe tener entre 10 y 1500 caracteres")
    @Pattern(regexp = "^[^<>]*$", message = "El mensaje no puede contener HTML")
    private String mensaje;

    private String sitioWeb;

    private Boolean aceptaPolitica;

    @Size(max = 50, message = "La versión de la política no puede superar 50 caracteres")
    private String politicaVersion;

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

    public String getSitioWeb() {
        return sitioWeb;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public Boolean getAceptaPolitica() {
        return aceptaPolitica;
    }

    public void setAceptaPolitica(Boolean aceptaPolitica) {
        this.aceptaPolitica = aceptaPolitica;
    }

    public String getPoliticaVersion() {
        return politicaVersion;
    }

    public void setPoliticaVersion(String politicaVersion) {
        this.politicaVersion = politicaVersion;
    }

    public void normalizeOptionalFields() {
        empresa = normalizeOptional(empresa);
        correo = normalizeOptional(correo);
        telefono = normalizeOptional(telefono);
        politicaVersion = normalizeOptional(politicaVersion);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
