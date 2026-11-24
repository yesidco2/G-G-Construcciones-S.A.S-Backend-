package com.gyg.backend.cotizacion;

import com.gyg.backend.cotizacion.VistaCotizacion.Linea;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class CotizacionPdf {
    private static final Color TINTA = new Color(17, 33, 48);
    private static final Color SECUNDARIO = new Color(68, 84, 96);
    private static final Color LINEA = new Color(203, 210, 214);
    private static final Color COBRE = new Color(153, 69, 24);
    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", CO);
    private final byte[] marca;

    public CotizacionPdf() throws IOException {
        marca = new ClassPathResource("cotizacion/marca-gg.png").getContentAsByteArray();
    }

    public byte[] generar(VistaCotizacion vista) {
        var salida = new ByteArrayOutputStream();
        try (var documento = new Document(PageSize.A4, 38, 38, 35, 48)) {
            var writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    var canvas = w.getDirectContent();
                    canvas.setColorStroke(LINEA);
                    canvas.moveTo(doc.left(), 35);
                    canvas.lineTo(doc.right(), 35);
                    canvas.stroke();
                    var pie = fuente(8, false, SECUNDARIO);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                            new Phrase(vista.empresa().nombre() + "   /   " + vista.referencia(), pie),
                            doc.left(), 22, 0);
                    ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                            new Phrase("Página " + w.getPageNumber(), pie), doc.right(), 22, 0);
                }
            });
            String titulo = vista.preliminar() ? "Estimación preliminar" : "Cotización";
            documento.addTitle(titulo + " · " + vista.referencia());
            documento.addAuthor(vista.empresa().nombre());
            writer.getExtraCatalog().put(PdfName.LANG, new PdfString(CO.toLanguageTag()));
            documento.open();
            encabezado(documento, vista, titulo);
            datosCliente(documento, vista);
            seccion(documento, "Servicios solicitados");
            detalle(documento, vista);
            resumen(documento, vista);
            if (vista.preliminar()) {
                boolean referencia = vista.items().stream().anyMatch(i -> i.servicio().tarifa() != null
                        && "REFERENCIA".equals(i.servicio().tarifa().tipo()));
                if (vista.pendientes() > 0) {
                    nota(documento, "Pendiente de confirmación",
                            "G&G debe definir el precio de los servicios marcados «Requiere revisión». "
                                    + "Esos servicios están excluidos de los importes calculados.");
                }
                if (referencia) {
                    nota(documento, "Precios de referencia",
                            "G&G debe confirmar la aplicabilidad y el valor definitivo de los precios de referencia.");
                }
            }
            if (vista.diasVigencia() != null) {
                nota(documento, "Vigencia", vista.diasVigencia() + " días. Hasta el "
                        + FECHA.format(vista.fecha().plusDays(vista.diasVigencia())) + ".");
            }
            nota(documento, "Alcance", vista.alcance());
            nota(documento, "Condiciones comerciales", vista.condiciones());
        } catch (IOException | DocumentException ex) {
            throw new IllegalStateException("No se pudo preparar la cotización en PDF.", ex);
        }
        return salida.toByteArray();
    }

    private void encabezado(Document doc, VistaCotizacion vista, String titulo) throws IOException {
        var tabla = tabla(38, 62);
        var logo = Image.getInstance(marca);
        logo.scaleToFit(99, 43);
        var celdaLogo = new PdfPCell();
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        celdaLogo.addElement(new Chunk(logo, 0, 0, true));
        tabla.addCell(celdaLogo);
        var datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        var empresa = vista.empresa();
        lineaDerecha(datos, empresa.nombre(), true);
        lineaDerecha(datos, empresa.ubicacion(), false);
        lineaDerecha(datos, empresa.telefono() + "   ·   " + empresa.correo(), false);
        if (!empresa.nit().isBlank()) lineaDerecha(datos, "NIT " + empresa.nit(), false);
        if (!empresa.direccion().isBlank()) lineaDerecha(datos, empresa.direccion(), false);
        doc.add(completar(tabla, datos));
        var rotulo = new Paragraph("COTIZACIONES   /   " + vista.referencia(), fuente(8, true, COBRE));
        rotulo.setSpacingBefore(15);
        doc.add(rotulo);
        var nombre = new Paragraph(titulo, fuente(28, true, TINTA));
        nombre.setSpacingAfter(1);
        doc.add(nombre);
        doc.add(new Paragraph(FECHA.format(vista.fecha()), fuente(10, false, SECUNDARIO)));
    }

    private static void lineaDerecha(PdfPCell celda, String contenido, boolean destacado) {
        var texto = new Paragraph(contenido, fuente(9, destacado, destacado ? TINTA : SECUNDARIO));
        texto.setAlignment(Element.ALIGN_RIGHT);
        celda.addElement(texto);
    }

    private static void datosCliente(Document doc, VistaCotizacion vista) {
        var cliente = vista.cliente();
        seccion(doc, "Cliente y proyecto");
        var tabla = tabla(57, 43);
        PdfPCell izquierda = dato("Solicitante", cliente.nombre());
        izquierda.addElement(new Paragraph(limpiar(cliente.correo()) + "\n" + limpiar(cliente.telefono()), fuente(10, false, TINTA)));
        if (cliente.nit() != null) izquierda.addElement(new Paragraph("NIT " + limpiar(cliente.nit()), fuente(10, false, TINTA)));
        tabla.addCell(izquierda);
        PdfPCell derecha = dato("Ubicación del proyecto", cliente.ciudad());
        if (cliente.direccion() != null) derecha.addElement(new Paragraph(limpiar(cliente.direccion()), fuente(10, false, TINTA)));
        tabla.addCell(derecha);
        doc.add(tabla);
        doc.add(new Paragraph(limpiar(cliente.descripcion()), fuente(10, false, TINTA)));
    }

    private static PdfPCell dato(String etiqueta, String contenido) {
        var celda = new PdfPCell();
        celda.setBorder(Rectangle.TOP);
        celda.setBorderColor(LINEA);
        celda.setPadding(6);
        celda.addElement(new Paragraph(etiqueta.toUpperCase(CO), fuente(8, true, SECUNDARIO)));
        celda.addElement(new Paragraph(limpiar(contenido), fuente(12, true, TINTA)));
        return celda;
    }

    private static void detalle(Document doc, VistaCotizacion vista) {
        var tabla = tabla(43, 12, 9, 17, 19);
        for (String titulo : new String[]{"Descripción", "Unidad", "Cant.", "Unitario", "Importe"}) {
            var cabecera = celda(titulo, fuente(9, true, Color.WHITE), false);
            cabecera.setBackgroundColor(TINTA);
            if (!titulo.equals("Descripción") && !titulo.equals("Unidad")) cabecera.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabla.addCell(cabecera);
        }
        tabla.setHeaderRows(1);
        tabla.setSplitLate(false);
        for (Linea linea : vista.items()) {
            var servicio = linea.servicio();
            var tarifa = servicio.tarifa();
            var info = celda("", fuente(10, true, TINTA), false);
            info.addElement(new Paragraph(servicio.nombre(), fuente(10, true, TINTA)));
            info.addElement(new Paragraph(servicio.descripcion(), fuente(9, false, SECUNDARIO)));
            if (tarifa == null) {
                info.addElement(new Paragraph("Requiere revisión", fuente(9, true, COBRE)));
            } else {
                if ("REFERENCIA".equals(tarifa.tipo())) {
                    info.addElement(new Paragraph("Precio de referencia", fuente(9, true, COBRE)));
                    info.addElement(new Paragraph(tarifa.fuente() + " · " + fechaFuente(tarifa.fechaFuente())
                            + " · " + tarifa.ubicacion() + " · " + servicio.unidad(), fuente(8, false, SECUNDARIO)));
                }
                info.addElement(new Paragraph("Alcance: " + tarifa.alcance(), fuente(8, false, SECUNDARIO)));
            }
            tabla.addCell(info);
            tabla.addCell(celda(servicio.unidad(), fuente(9, false, TINTA), false));
            tabla.addCell(celda(numero(linea.cantidad()), fuente(9, false, TINTA), true));
            tabla.addCell(celda(tarifa == null ? "—" : dinero(tarifa.precioUnitario()), fuente(9, false, TINTA), true));
            tabla.addCell(celda(linea.total() == null ? "—" : dinero(linea.total()), fuente(9, true, TINTA), true));
        }
        doc.add(tabla);
    }

    private static void resumen(Document doc, VistaCotizacion vista) {
        var tabla = tabla(68, 32);
        tabla.setSpacingBefore(12);
        tabla.setKeepTogether(true);
        tabla.addCell(celda(vista.pendientes() > 0 ? "Subtotal de ítems valorados" : "Subtotal",
                fuente(10, true, TINTA), false));
        tabla.addCell(celda(dinero(vista.subtotal()), fuente(11, true, TINTA), true));
        for (var ajuste : vista.ajustes()) {
            tabla.addCell(celda(ajuste.nombre(), fuente(10, false, SECUNDARIO), false));
            tabla.addCell(celda(dinero(ajuste.valor()), fuente(10, false, TINTA), true));
        }
        if (vista.pendientes() == 0 || !vista.ajustes().isEmpty()) {
            String rotulo = vista.pendientes() > 0 ? "Importe parcial con ajustes" : "Total";
            var total = celda(rotulo, fuente(11, true, Color.WHITE), false);
            total.setBackgroundColor(TINTA);
            tabla.addCell(total);
            var valor = celda(dinero(vista.total()), fuente(11, true, Color.WHITE), true);
            valor.setBackgroundColor(TINTA);
            tabla.addCell(valor);
        }
        doc.add(tabla);
        doc.add(new Paragraph("Valores expresados en pesos colombianos (COP).", fuente(8, false, SECUNDARIO)));
    }

    private static PdfPTable tabla(float... anchos) {
        var tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        return tabla;
    }

    private static PdfPTable completar(PdfPTable tabla, PdfPCell ultima) {
        tabla.addCell(ultima);
        return tabla;
    }

    private static PdfPCell celda(String texto, Font fuente, boolean derecha) {
        var celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(LINEA);
        celda.setPadding(6);
        if (derecha) celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    private static void seccion(Document doc, String titulo) {
        var seccion = new Paragraph(titulo, fuente(14, true, TINTA));
        seccion.setSpacingBefore(12);
        seccion.setSpacingAfter(6);
        doc.add(seccion);
    }

    private static void nota(Document doc, String titulo, String contenido) {
        if (contenido == null || contenido.isBlank()) return;
        var nota = new Paragraph();
        nota.setSpacingBefore(8);
        nota.add(new Chunk(titulo + "   ", fuente(9, true, TINTA)));
        nota.add(new Chunk(contenido, fuente(9, false, SECUNDARIO)));
        doc.add(nota);
    }

    private static Font fuente(float puntos, boolean negrita, Color color) {
        return new Font(Font.HELVETICA, puntos, negrita ? Font.BOLD : Font.NORMAL, color);
    }

    private static String numero(BigDecimal valor) {
        return NumberFormat.getNumberInstance(CO).format(valor);
    }

    private static String dinero(BigDecimal valor) {
        var moneda = NumberFormat.getCurrencyInstance(CO);
        moneda.setMinimumFractionDigits(0);
        moneda.setMaximumFractionDigits(2);
        return moneda.format(valor);
    }

    private static String limpiar(String valor) {
        return valor.replaceAll("\\s+", " ").strip();
    }

    private static String fechaFuente(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
    }
}
