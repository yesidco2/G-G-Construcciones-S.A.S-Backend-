package com.gyg.backend.config;

import com.gyg.backend.model.*;
import com.gyg.backend.repository.LaborRepository;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.PoliticaAmbientalRepository;
import com.gyg.backend.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private LaborRepository laborRepository;

    @Autowired
    private PoliticaAmbientalRepository politicaAmbientalRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Override
    public void run(String... args) throws Exception {
        seedServicios();
        seedLaboresRiesgosEpps();
        seedPoliticasAmbientales();
        seedObras();
    }

    private void seedObras() {
        if (obraRepository.count() == 0) {
            Obra viaducto = new Obra("Viaducto Metropolitano", "GG-2026-00125", "EN_PROGRESO", 1L, 74);
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 1, 15)));
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 3, 10)));
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 5, 20)));

            Obra centroLogistico = new Obra("Centro Logístico Industrial", "GG-2026-00218", "EN_PROGRESO", 1L, 58);
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 2, 5)));
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1581092160562-40aa08e78837?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 4, 12)));
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1581092580497-e0d23cbdf1dc?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 6, 18)));

            Obra subestacion = new Obra("Subestación Energía Norte", "GG-2026-00307", "FINALIZADA", 1L, 100);
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2025, 11, 8)));
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 2, 22)));
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1516937941344-00b4e0337589?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 5, 30)));

            Obra programaSst = new Obra("Programa SST Corredor Sur", "GYG-CIVIL-001", "EN_PROGRESO", 1L, 40);
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1503387762-592dea58ef23?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 1, 28)));
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 3, 25)));
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 6, 2)));

            obraRepository.saveAll(Arrays.asList(viaducto, centroLogistico, subestacion, programaSst));
            System.out.println(">> Se han precargado las obras y sus fotos de seguimiento.");
        }
    }

    private void seedServicios() {
        if (servicioRepository.count() == 0) {
            servicioRepository.saveAll(Arrays.asList(
                    new Servicio("Diseno de Estructuras y Cimentaciones", 
                            "Diseno y calculo estructural avanzado bajo norma NSR-10, garantizando cimentaciones solidas, seguras y optimizadas para edificaciones residenciales y comerciales.", 
                            Bloque.CIVIL),
                    new Servicio("Estudio de Suelos y Geotecnia", 
                            "Analisis geotécnico detallado del terreno, toma de muestras, sondeos y ensayos de laboratorio para determinar la capacidad portante y estabilidad del suelo.", 
                            Bloque.CIVIL),
                    new Servicio("Supervision Tecnica Independiente", 
                            "Verificacion y control de calidad riguroso en obra civil para asegurar el estricto cumplimiento de los planos estructurales, especificaciones de materiales y normativa legal.", 
                            Bloque.CIVIL),
                    new Servicio("Diseno e Implementacion de SG-SST", 
                            "Diseñamos y estructuramos su Sistema de Gestión de la Seguridad y Salud en el Trabajo adaptado al tamaño y sector de su empresa, bajo el Decreto 1072 de 2015.", 
                            Bloque.SST),
                    new Servicio("Capacitacion en Trabajo Seguro en Alturas", 
                            "Cursos teoricos y practicos dictados por entrenadores autorizados para la realizacion de labores seguras en alturas bajo la Resolucion 4272 de 2021.", 
                            Bloque.SST),
                    new Servicio("Auditorias de Seguridad e Higiene Industrial", 
                            "Evaluacion preventiva y diagnostico del cumplimiento de normas de higiene, uso correcto de EPP, senalizacion de emergencia y control de riesgos operativos.", 
                            Bloque.SST)
            ));
            System.out.println(">> Se han precargado los servicios en la base de datos.");
        }
    }

    private void seedLaboresRiesgosEpps() {
        if (laborRepository.count() == 0) {
            // Labor 1: Trabajo en Alturas
            Labor l1 = new Labor("Trabajo en Alturas");
            l1.addRiesgo(new Riesgo("Caida de personas a distinto nivel por perdida de equilibrio o falla de equipos", Severidad.ALTO));
            l1.addRiesgo(new Riesgo("Caida de herramientas o materiales sobre personal en niveles inferiores", Severidad.MEDIO));
            
            l1.addEpp(new EPP("Arnes de cuerpo completo dieléctrico / multiproposito (4 argollas)", "Protección Contra Caídas"));
            l1.addEpp(new EPP("Casco dieléctrico de alta resistencia con barbuquejo de 3 o 4 puntos de anclaje", "Protección Craneal"));
            l1.addEpp(new EPP("Eslinga de doble terminal con absorbedor de impacto", "Protección Contra Caídas"));
            l1.addEpp(new EPP("Lineas de vida portatiles / conectores de anclaje", "Protección Contra Caídas"));

            // Labor 2: Riesgo Electrónico / Eléctrico
            Labor l2 = new Labor("Riesgo Eléctrico");
            l2.addRiesgo(new Riesgo("Contacto eléctrico directo con conductores energizados sin proteccion", Severidad.ALTO));
            l2.addRiesgo(new Riesgo("Quemaduras de tercer grado originadas por arco eléctrico", Severidad.ALTO));
            l2.addRiesgo(new Riesgo("Contacto indirecto a través de carcasas o herramientas energizadas de forma accidental", Severidad.MEDIO));
            
            l2.addEpp(new EPP("Guantes dieléctricos certificados (Clase segun nivel de tension)", "Protección de Manos"));
            l2.addEpp(new EPP("Calzado dieléctrico con suela de goma antideslizante sin punta metálica", "Protección de Pies"));
            l2.addEpp(new EPP("Careta de proteccion facial contra arco eléctrico", "Protección Facial"));

            // Labor 3: Excavaciones
            Labor l3 = new Labor("Excavaciones");
            l3.addRiesgo(new Riesgo("Atrapamiento debido al desprendimiento de paredes de la excavación (derrumbes)", Severidad.ALTO));
            l3.addRiesgo(new Riesgo("Golpes o atropellamientos por maquinaria pesada operando cerca de zanjas", Severidad.MEDIO));
            l3.addRiesgo(new Riesgo("Deficiencia de oxigeno o acumulacion de gases nocivos a profundidades mayores a 1.20 metros", Severidad.MEDIO));
            
            l3.addEpp(new EPP("Casco de seguridad clase G o E con barbuquejo", "Protección Craneal"));
            l3.addEpp(new EPP("Botas de seguridad con puntera de acero o composite reforzado", "Protección de Pies"));
            l3.addEpp(new EPP("Gafas de seguridad ajustadas con proteccion lateral contra particulas", "Protección Ocular"));
            l3.addEpp(new EPP("Chaleco reflectivo de alta visibilidad para operacion diurna y nocturna", "Protección Corporal"));

            // Labor 4: Izaje de Cargas
            Labor l4 = new Labor("Izaje de Cargas");
            l4.addRiesgo(new Riesgo("Caida de la carga suspendida por falla mecánica o mala sujecion", Severidad.ALTO));
            l4.addRiesgo(new Riesgo("Volcamiento de la grua por sobrepasar capacidad de carga o inestabilidad del terreno", Severidad.ALTO));
            l4.addRiesgo(new Riesgo("Golpes provocados por la oscilacion incontrolada de la carga izada", Severidad.MEDIO));

            l4.addEpp(new EPP("Casco de seguridad industrial", "Protección Craneal"));
            l4.addEpp(new EPP("Guantes de cuero/vaqueta de alta resistencia para maniobra de vientos", "Protección de Manos"));
            l4.addEpp(new EPP("Botas de seguridad con puntera reforzada", "Protección de Pies"));

            // Labor 5: Espacios Confinados
            Labor l5 = new Labor("Espacios Confinados");
            l5.addRiesgo(new Riesgo("Asfixia por deficiencia de oxigeno en el recinto cerrado", Severidad.ALTO));
            l5.addRiesgo(new Riesgo("Intoxicacion por inhalacion de gases toxicos o vapores inflamables", Severidad.ALTO));
            l5.addRiesgo(new Riesgo("Dificultad de evacuacion rapida en caso de emergencia medica", Severidad.ALTO));

            l5.addEpp(new EPP("Medidor multigas portatil (monitoreo continuo de atmosfera)", "Protección Respiratoria"));
            l5.addEpp(new EPP("Arnes de rescate e izado", "Protección Contra Caídas"));
            l5.addEpp(new EPP("Equipo de respiracion autonoma o suministro de aire", "Protección Respiratoria"));

            laborRepository.saveAll(Arrays.asList(l1, l2, l3, l4, l5));
            System.out.println(">> Se han precargado las labores, riesgos y EPP en la base de datos.");
        }
    }

    private void seedPoliticasAmbientales() {
        if (politicaAmbientalRepository.count() == 0) {
            politicaAmbientalRepository.saveAll(Arrays.asList(
                    new PoliticaAmbiental(
                            "Gestion Integral de Residuos de Construccion y Demolicion (RCD)",
                            "Optimizacion de recursos y reduccion de residuos solidos en obras civiles mediante Plan de Gestion RCD, clasificacion en la fuente y disposicion final certificada."),
                    new PoliticaAmbiental(
                            "Prevencion de Contaminacion Atmosferica",
                            "Prevencion de la contaminacion atmosferica mediante monitoreo periodico de emisiones, humectacion de suelos en obra y control de material particulado."),
                    new PoliticaAmbiental(
                            "Uso Eficiente de Agua y Energia",
                            "Consumo responsable y eficiente de agua y energia en todas las instalaciones y proyectos activos, con metas anuales de reduccion medibles."),
                    new PoliticaAmbiental(
                            "Capacitacion Ambiental Permanente",
                            "Capacitacion y sensibilizacion constante al 100% de la mano de obra en manejo ambiental, clasificacion en la fuente y buenas practicas sostenibles."),
                    new PoliticaAmbiental(
                            "Materiales y Proveedores Responsables",
                            "Uso de materiales certificados de bajo impacto ambiental y seleccion de proveedores que cumplan criterios ambientales y de responsabilidad social.")
            ));
            System.out.println(">> Se han precargado las politicas ambientales en la base de datos.");
        }
    }
}
