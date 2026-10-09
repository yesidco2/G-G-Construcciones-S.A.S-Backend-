package com.gyg.backend.controller;

import com.gyg.backend.dto.*;
import com.gyg.backend.service.GygService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "G&G Construcciones API", description = "Servicios REST para el portal de G&G Construcciones (Ingenieria Civil y Consultoria SST)")
public class GygController {

    @Autowired
    private GygService gygService;

    @GetMapping("/institucional")
    @Operation(summary = "Obtener mision, vision y valores de la empresa")
    public ResponseEntity<InstitucionalDTO> getInstitucional() {
        return ResponseEntity.ok(gygService.getInstitucional());
    }

    @GetMapping("/servicios")
    @Operation(summary = "Listar todos los servicios ofrecidos (CIVIL y SST)")
    public ResponseEntity<List<ServicioDTO>> getServicios() {
        return ResponseEntity.ok(gygService.getServicios());
    }

    @GetMapping("/datos-legales")
    @Operation(summary = "Obtener los datos legales (NIT y direccion) de la empresa")
    public ResponseEntity<DatosLegalesDTO> getDatosLegales() {
        return ResponseEntity.ok(gygService.getDatosLegales());
    }

    @PostMapping("/contacto")
    @Operation(summary = "Registrar una solicitud de contacto o cotizacion desde el formulario del portal")
    public ResponseEntity<ContactoResponse> crearContacto(@Valid @RequestBody ContactoRequest request) {
        ContactoResponse response = gygService.crearContacto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/chat")
    @Operation(summary = "Generar una respuesta automatizada basada en palabras clave de normativas SST")
    public ResponseEntity<ChatResponse> responderChat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(gygService.responderChat(request));
    }

    @GetMapping("/proyectos")
    @Operation(summary = "Listar obras del portafolio con filtro opcional por categoria (CIVIL o SST)")
    public ResponseEntity<List<ProyectoDTO>> getProyectos(@RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(gygService.getProyectos(categoria));
    }

    @GetMapping("/obras/seguimiento/{codigo}")
    @Operation(summary = "Consultar el seguimiento de una obra y su galeria de fotos por codigo de acceso")
    public ResponseEntity<SeguimientoObraDTO> getSeguimientoObra(
            @PathVariable("codigo") String codigo) {
        return ResponseEntity.ok(gygService.getSeguimientoProyecto(codigo));
    }

    @GetMapping("/riesgos/labores")
    @Operation(summary = "Listar las labores o actividades de alto riesgo disponibles")
    public ResponseEntity<List<LaborDTO>> getLabores() {
        return ResponseEntity.ok(gygService.getLabores());
    }

    @GetMapping("/riesgos/{laborId}")
    @Operation(summary = "Obtener la matriz de riesgos y EPP recomendados para una labor especifica")
    public ResponseEntity<LaborDetalleDTO> getLaborDetalle(
            @PathVariable("laborId") Long laborId) {
        return ResponseEntity.ok(gygService.getLaborDetalle(laborId));
    }

    @GetMapping("/politicas-ambientales")
    @Operation(summary = "Obtener las politicas y acciones de mitigacion ambiental de la empresa")
    public ResponseEntity<List<PoliticaAmbientalDTO>> getPoliticasAmbientales() {
        return ResponseEntity.ok(gygService.getPoliticasAmbientales());
    }
}
