package com.gyg.backend.cotizacion;

import com.gyg.backend.service.RateLimitService;
import com.gyg.backend.service.TarifarioCotizacion;
import com.gyg.backend.service.TarifarioCotizacion.Servicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {
    private final TarifarioCotizacion tarifario;
    private final CotizadorService cotizador;
    private final CotizacionPdf pdf;
    private final RateLimitService limites;

    public CotizacionController(TarifarioCotizacion tarifario, CotizadorService cotizador,
                                CotizacionPdf pdf, RateLimitService limites) {
        this.tarifario = tarifario;
        this.cotizador = cotizador;
        this.pdf = pdf;
        this.limites = limites;
    }

    @GetMapping("/catalogo")
    public List<Servicio> catalogo(@RequestParam(defaultValue = "") String ciudad) {
        if (ciudad.length() > 120) {
            throw new IllegalArgumentException("Municipio demasiado largo.");
        }
        return tarifario.servicios(ciudad.strip(), CotizadorService.hoy());
    }

    @PostMapping("/vista-previa")
    public ResponseEntity<VistaCotizacion> vistaPrevia(@Valid @RequestBody SolicitudCotizacion solicitud,
                                                      HttpServletRequest request) {
        limites.check("cotizacion-vista", request, 60, Duration.ofMinutes(1), "Espera un momento para volver a cotizar.");
        var vista = cotizador.calcular(solicitud);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(vista);
    }

    @PostMapping("/pdf")
    public ResponseEntity<?> descargar(@Valid @RequestBody SolicitudCotizacion solicitud,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String revision, HttpServletRequest request) {
        limites.check("cotizacion-pdf", request, 15, Duration.ofMinutes(1), "Espera un momento para generar otro PDF.");
        var vista = cotizador.calcular(solicitud);
        if (revision != null && !revision.equals("\"" + vista.referencia() + "\"")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(CacheControl.noStore())
                    .body(Map.of("message", "La cotización cambió. Actualiza la vista previa para revisar los valores."));
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(vista.referencia() + ".pdf").build().toString())
                .body(pdf.generar(vista));
    }
}
