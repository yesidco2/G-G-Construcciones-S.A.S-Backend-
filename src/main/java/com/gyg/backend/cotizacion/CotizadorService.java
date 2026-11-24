package com.gyg.backend.cotizacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyg.backend.cotizacion.VistaCotizacion.*;
import com.gyg.backend.service.TarifarioCotizacion;
import com.gyg.backend.service.TarifarioCotizacion.Servicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;

@Service
public class CotizadorService {
    private final TarifarioCotizacion tarifario;
    private final ObjectMapper json;
    private final Empresa empresa;

    public CotizadorService(TarifarioCotizacion tarifario, ObjectMapper json,
            @Value("${app.cotizador.empresa.nombre:G&G CONSTRUCCIONES SST S.A.S.}") String nombre,
            @Value("${app.cotizador.empresa.telefono:+57 302 201 9694}") String telefono,
            @Value("${app.cotizador.empresa.correo:gygconstrucciones@gmail.com}") String correo,
            @Value("${app.cotizador.empresa.ubicacion:Medellín, Colombia}") String ubicacion,
            @Value("${app.cotizador.empresa.nit:}") String nit,
            @Value("${app.cotizador.empresa.direccion:}") String direccion) {
        this.tarifario = tarifario;
        this.json = json;
        this.empresa = new Empresa(nombre, telefono, correo, ubicacion, nit, direccion);
    }

    static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public VistaCotizacion calcular(SolicitudCotizacion solicitud) {
        var fecha = hoy();
        var cliente = solicitud.cliente();
        var servicios = new HashMap<String, Servicio>();
        tarifario.servicios(cliente.ciudad(), fecha).forEach(s -> servicios.put(s.id(), s));
        var config = tarifario.configuracion(cliente.ciudad(), fecha);
        var items = new ArrayList<Linea>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int pendientes = 0;
        boolean preliminar = false;
        for (var item : solicitud.items()) {
            var servicio = servicios.remove(item.servicioId());
            if (servicio == null) {
                throw new IllegalArgumentException("Servicio desconocido o repetido.");
            }
            BigDecimal cantidad = item.cantidad().stripTrailingZeros();
            if (cantidad.scale() > 0 && ("servicio completo".equalsIgnoreCase(servicio.unidad())
                    || "visita".equalsIgnoreCase(servicio.unidad()))) {
                throw new IllegalArgumentException("La unidad requiere una cantidad entera.");
            }
            BigDecimal importe = null;
            if (servicio.tarifa() == null) {
                pendientes++;
                preliminar = true;
            } else {
                importe = moneda(cantidad.multiply(servicio.tarifa().precioUnitario()));
                subtotal = subtotal.add(importe);
                preliminar |= "REFERENCIA".equals(servicio.tarifa().tipo());
            }
            items.add(new Linea(servicio, cantidad, importe));
        }

        // El descuento se aplica a los servicios valorados. El impuesto se calcula
        // sobre el importe descontado más el desplazamiento configurado.
        var ajustes = new ArrayList<Ajuste>();
        BigDecimal total = subtotal;
        if (config.descuentoPorcentaje() != null) {
            var descuento = porcentaje(subtotal, config.descuentoPorcentaje()).negate();
            ajustes.add(new Ajuste("Descuento (" + tasa(config.descuentoPorcentaje()) + "%)", descuento));
            total = total.add(descuento);
        }
        if (config.desplazamiento() != null) {
            ajustes.add(new Ajuste("Desplazamiento", config.desplazamiento()));
            total = total.add(config.desplazamiento());
        }
        if (config.impuestoPorcentaje() != null) {
            var impuesto = porcentaje(total, config.impuestoPorcentaje());
            ajustes.add(new Ajuste("Impuesto (" + tasa(config.impuestoPorcentaje()) + "%)", impuesto));
            total = total.add(impuesto);
        }
        String referencia = referencia(fecha, cliente, items, config);
        return new VistaCotizacion(fecha, referencia, empresa, cliente, preliminar, items,
                subtotal, pendientes, ajustes, total, config.diasVigencia(), config.condiciones(), config.alcance());
    }

    private String referencia(LocalDate fecha, Cliente cliente, List<Linea> lineas, TarifarioCotizacion.Configuracion config) {
        try {
            byte[] contenido = json.writeValueAsBytes(List.of(fecha, empresa, cliente, lineas, config));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(contenido);
            return "GG-" + fecha.getYear() + "-" + HexFormat.of().withUpperCase().formatHex(digest, 0, 8);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo generar la referencia de la cotización.", ex);
        }
    }

    private static BigDecimal moneda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal porcentaje(BigDecimal base, BigDecimal tasa) {
        return moneda(base.multiply(tasa).movePointLeft(2));
    }

    private static String tasa(BigDecimal tasa) {
        return tasa.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
