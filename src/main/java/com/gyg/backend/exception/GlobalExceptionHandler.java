package com.gyg.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.gyg.backend.service.CaptchaProviderUnavailableException;
import com.gyg.backend.service.CaptchaService;
import com.gyg.backend.service.PasswordResetUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex, WebRequest request) {
        return createErrorResponse(
                HttpStatus.CONFLICT, "Conflict", "La solicitud no se puede completar por un conflicto.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND, "Not Found", "No se encontró el recurso solicitado.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        
        body.put("message", "La solicitud contiene datos no válidos.");
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST, "Bad Request", "La solicitud contiene datos no válidos.", request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidParameter(Exception ex, WebRequest request) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST, "Bad Request", "La solicitud contiene datos no válidos.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN, "Forbidden", "No tienes permisos para acceder a este recurso.", request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(CaptchaService.InvalidCaptchaException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCaptcha(
            CaptchaService.InvalidCaptchaException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN, "Forbidden", "La verificación CAPTCHA no fue válida.", request);
    }

    @ExceptionHandler(CaptchaProviderUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCaptchaUnavailable(
            CaptchaProviderUnavailableException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN, "Forbidden", "La verificación CAPTCHA no fue válida.", request);
    }

    @ExceptionHandler(PasswordResetUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordResetUnavailable(
            PasswordResetUnavailableException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                "No fue posible procesar la solicitud. Intenta nuevamente más tarde.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        if (request.getDescription(false).contains("/api/contacto")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "codigo", "CONTACTO_VALIDATION_ERROR",
                    "mensaje", "La solicitud contiene campos inválidos.",
                    "camposInvalidos", java.util.List.of(Map.of(
                            "campo", "cuerpo", "motivo", "formato inválido"))));
        }
        return createErrorResponse(
                HttpStatus.BAD_REQUEST, "Bad Request", "El cuerpo de la solicitud no tiene un formato valido.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "El metodo HTTP no esta permitido.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "El tipo de contenido de la solicitud no esta soportado.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            NoResourceFoundException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND, "Not Found", "No se encontro el recurso solicitado.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(
            MaxUploadSizeExceededException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large",
                "El archivo supera el tamaño permitido.", request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMultipart(
            MultipartException ex,
            WebRequest request) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST, "Bad Request", "El formulario de carga no es válido.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Error interno no controlado al procesar la solicitud ({})", ex.getClass().getSimpleName());
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocurrió un error interno en el servidor.", request);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status,
            String error,
            String message,
            WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getRequestURI()
                : request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, status);
    }
}
