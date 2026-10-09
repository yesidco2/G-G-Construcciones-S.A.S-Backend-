package com.gyg.backend.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class KeywordChatEngine implements ChatEngine {

    @Override
    public String responder(String pregunta) {
        if (pregunta == null || pregunta.trim().isEmpty()) {
            return "Para ayudarte, indica qué norma, riesgo o procedimiento quieres revisar.";
        }

        String texto = pregunta.toLowerCase(Locale.ROOT);

        if (texto.contains("trabajo en alturas") || texto.contains("trabajar en alturas")
                || texto.contains("trabajo en altura") || texto.contains("trabajar en altura")
                || texto.contains("caida de altura") || texto.contains("caídas de altura")) {
            return "Para trabajo en alturas se debe realizar una evaluacion de riesgos, contar con permiso de trabajo, inspeccionar arneses y sistemas de anclaje, usar linea de vida cuando aplique y garantizar capacitacion y un plan de rescate.";
        }

        if (texto.contains("epp") || texto.contains("elementos de proteccion") || texto.contains("proteccion personal")) {
            return "Los EPP requeridos deben seleccionarse según el riesgo: casco, guantes, botas de seguridad, gafas, tapabocas o arnés, según la tarea.";
        }

        if (texto.contains("sst") || texto.contains("seguridad") || texto.contains("salud en el trabajo")) {
            return "Para SST, es clave identificar riesgos, capacitar al personal, definir procedimientos y garantizar el uso de EPP, inspecciones y reporte de incidentes.";
        }

        if (texto.contains("norma") || texto.contains("reglamento") || texto.contains("ley")) {
            return "Las normas principales a revisar son la legislación laboral, la normativa de seguridad industrial y los procedimientos internos de la empresa.";
        }

        if (texto.contains("riesgo") || texto.contains("peligro") || texto.contains("alto riesgo")) {
            return "Se debe evaluar la actividad, identificar el peligro, determinar la severidad y aplicar controles de ingeniería, administrativos y EPP.";
        }

        if (texto.contains("incidente") || texto.contains("accidente") || texto.contains("evento")) {
            return "En caso de incidente, se debe reportar inmediatamente, aislar la zona, atender a la persona, investigar causas y documentar acciones correctivas.";
        }

        if (texto.contains("corte") || texto.contains("construccion") || texto.contains("obra") || texto.contains("civil")) {
            return "En obra civil es esencial verificar acceso seguro, señalización, equipos de excavación, mantenimiento y controles de tránsito y altura.";
        }

        return "Revisa la actividad, los riesgos asociados, la capacitación y los EPP requeridos; si lo deseas, puedo ayudarte a formular un checklist de SST.";
    }
}
