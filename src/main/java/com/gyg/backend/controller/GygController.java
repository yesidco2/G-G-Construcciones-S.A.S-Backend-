package com.gyg.backend.controller;

import com.gyg.backend.dto.*;
import com.gyg.backend.service.GygService;
import com.gyg.backend.service.RateLimitService;
import com.gyg.backend.service.CaptchaService;
import com.gyg.backend.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "G&G Construcciones API", description = "Servicios REST para el portal de G&G Construcciones (Ingenieria Civil y Consultoria SST)")
public class GygController {

        private static final java.util.regex.Pattern CONTACT_LINK_PATTERN =
            java.util.regex.Pattern.compile("(?i)(https?://|www\\.)");

    @Autowired
    private GygService gygService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private Validator validator;

    @Value("${app.captcha.enabled:true}")
    private boolean captchaEnabled;

    @Value("${app.rate-limit.contacto-per-hour:5}")
    private int contactoPerHour;

    @Value("${app.rate-limit.chat-per-minute:20}")
    private int chatPerMinute;

    @Value("${app.rate-limit.seguimiento-per-minute:10}")
    private int seguimientoPerMinute;

    @Value("${app.rate-limit.seguimiento-attempts-per-15-min:20}")
    private int seguimientoAttemptsPer15Min;

    @Value("${app.rate-limit.seguimiento-failed-per-15-min:5}")
    private int seguimientoFailedPer15Min;

    @GetMapping("/institucional")
    @Operation(summary = "Obtener mision, vision y valores de la empresa")
    public ResponseEntity<InstitucionalDTO> getInstitucional() {
        return ResponseEntity.ok(gygService.getInstitucional());
    }

    @GetMapping("/datos-legales")
    @Operation(summary = "Obtener los datos legales (NIT y direccion) de la empresa")
    public ResponseEntity<DatosLegalesDTO> getDatosLegales() {
        return ResponseEntity.ok(gygService.getDatosLegales());
    }

    @PostMapping("/contacto")
    @Operation(summary = "Registrar una solicitud de contacto o cotizacion desde el formulario del portal")
    public ResponseEntity<?> crearContacto(
            @RequestBody ContactoRequest request,
            @RequestHeader(name = "X-Captcha-Token", required = false) String captchaToken,
            HttpServletRequest httpRequest) {
        if (request.getSitioWeb() != null && !request.getSitioWeb().isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ContactoResponse(0L));
        }

        if (captchaEnabled) {
            captchaService.verifySubmissionToken(captchaToken, "contacto", httpRequest);
        }
        if (!Boolean.TRUE.equals(request.getAceptaPolitica())) {
            return contactoValidationError(List.of(
                    Map.of("campo", "aceptaPolitica", "motivo", "debe ser aceptada")));
        }

        rateLimitService.check(
                "contacto", httpRequest, contactoPerHour, Duration.ofHours(1),
                "Has alcanzado el limite de solicitudes de contacto. Intenta mas tarde.");

        request.normalizeOptionalFields();
        Set<ConstraintViolation<ContactoRequest>> violations = validator.validate(request);
        List<Map<String, String>> invalidFields = new ArrayList<>();
        for (ConstraintViolation<ContactoRequest> violation : violations) {
            invalidFields.add(Map.of(
                    "campo", violation.getPropertyPath().toString(),
                    "motivo", violation.getMessage()));
        }
        if (request.getCorreo() == null && request.getTelefono() == null) {
            invalidFields.add(Map.of(
                    "campo", "correo",
                    "motivo", "debe proporcionar correo o teléfono"));
        }
        if (request.getMensaje() != null && countLinks(request.getMensaje()) > 2) {
            invalidFields.add(Map.of(
                    "campo", "mensaje",
                    "motivo", "no puede contener más de 2 enlaces"));
        }
        if (request.getTipoServicio() != null
            && !ContactoRequest.TIPOS_SERVICIO_PERMITIDOS.contains(request.getTipoServicio())) {
            invalidFields.add(Map.of(
                "campo", "tipoServicio",
                "motivo", "el valor no está permitido"));
        }
        if (!invalidFields.isEmpty()) {
            return contactoValidationError(invalidFields);
        }

        gygService.crearContacto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ContactoResponse(0L));
    }

        private static ResponseEntity<Map<String, Object>> contactoValidationError(
            List<Map<String, String>> invalidFields) {
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "CONTACTO_VALIDATION_ERROR",
                "mensaje", "La solicitud contiene campos inválidos.",
                "camposInvalidos", invalidFields));
    }

    private static int countLinks(String value) {
        var matcher = CONTACT_LINK_PATTERN.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @PostMapping("/chat")
    @Operation(summary = "Generar una respuesta automatizada basada en palabras clave de normativas SST")
    public ResponseEntity<ChatResponse> responderChat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        rateLimitService.check(
                "chat", httpRequest, chatPerMinute, Duration.ofMinutes(1),
                "Has alcanzado el limite de mensajes. Intenta nuevamente en un minuto.");
        return ResponseEntity.ok(gygService.responderChat(request));
    }

    @PostMapping("/obras/seguimiento")
    @Operation(summary = "Consultar el seguimiento de una obra por código de acceso")
    public ResponseEntity<SeguimientoObraDTO> getSeguimientoObra(
            @Valid @RequestBody SeguimientoRequest request,
            HttpServletRequest httpRequest) {
        rateLimitSeguimiento(httpRequest);
        try {
            return ResponseEntity.ok(gygService.getSeguimientoProyecto(request.getCodigo()));
        } catch (ResourceNotFoundException notFound) {
            rateLimitService.check(
                    "seguimiento-fallido",
                    httpRequest, seguimientoFailedPer15Min, Duration.ofMinutes(15),
                    "Demasiados intentos de seguimiento. Intenta más tarde.");
            throw notFound;
        }
    }

    @PostMapping("/obras/seguimiento/fotos/{fotoId}")
    @Operation(summary = "Descargar una foto de seguimiento usando su código de acceso")
    public ResponseEntity<byte[]> getSeguimientoFoto(
            @PathVariable @Positive Long fotoId,
            @Valid @RequestBody SeguimientoRequest request,
            HttpServletRequest httpRequest) throws IOException {
        rateLimitSeguimiento(httpRequest);
        try {
            GygService.StoredImage image = gygService.getSeguimientoFoto(request.getCodigo(), fotoId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.contentType()));
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("foto-seguimiento.png", StandardCharsets.UTF_8)
                    .build());
            headers.set("X-Content-Type-Options", "nosniff");
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(image.content(), headers, HttpStatus.OK);
        } catch (ResourceNotFoundException notFound) {
            rateLimitService.check(
                    "seguimiento-fallido",
                    httpRequest, seguimientoFailedPer15Min, Duration.ofMinutes(15),
                    "Demasiados intentos de seguimiento. Intenta más tarde.");
            throw notFound;
        }
    }

    private void rateLimitSeguimiento(HttpServletRequest request) {
        rateLimitService.check(
                "seguimiento-intentos", request, seguimientoAttemptsPer15Min, Duration.ofMinutes(15),
                "Has alcanzado el límite de consultas de seguimiento. Intenta más tarde.");
        rateLimitService.check(
                "seguimiento", request, seguimientoPerMinute, Duration.ofMinutes(1),
                "Has alcanzado el limite de consultas de seguimiento. Intenta nuevamente en un minuto.");
    }

}
