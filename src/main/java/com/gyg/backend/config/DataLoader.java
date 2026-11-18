package com.gyg.backend.config;

import com.gyg.backend.model.*;
import com.gyg.backend.repository.ObraRepository;
import com.gyg.backend.repository.UsuarioRepository;
import com.gyg.backend.service.TrackingCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.time.LocalDate;

@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private TrackingCodeService trackingCodeService;

    @Override
    public void run(String... args) throws Exception {
        seedObras();
    }

    private void seedObras() {
        if (obraRepository.count() == 0) {
            Usuario cliente = usuarioRepository.findAllByRolAndActivoTrueOrderByNombreAsc(Rol.CLIENTE)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (cliente == null) {
                System.out.println(">> No se precargaron obras: no existe un usuario activo con rol CLIENTE.");
                return;
            }

            Obra viaducto = obraConCodigo("Viaducto Metropolitano", "EN_PROGRESO", 74, cliente);
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 1, 15)));
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 3, 10)));
            viaducto.addFoto(new FotoObra("https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 5, 20)));

            Obra centroLogistico = obraConCodigo("Centro Logístico Industrial", "EN_PROGRESO", 58, cliente);
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 2, 5)));
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1581092160562-40aa08e78837?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 4, 12)));
            centroLogistico.addFoto(new FotoObra("https://images.unsplash.com/photo-1581092580497-e0d23cbdf1dc?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 6, 18)));

            Obra subestacion = obraConCodigo("Subestación Energía Norte", "FINALIZADA", 100, cliente);
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2025, 11, 8)));
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 2, 22)));
            subestacion.addFoto(new FotoObra("https://images.unsplash.com/photo-1516937941344-00b4e0337589?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 5, 30)));

            Obra programaSst = obraConCodigo("Programa SST Corredor Sur", "EN_PROGRESO", 40, cliente);
            programaSst.setCategoria("SST");
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1503387762-592dea58ef23?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 1, 28)));
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1521737711867-e3b97375f902?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 3, 25)));
            programaSst.addFoto(new FotoObra("https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=1200&q=80", LocalDate.of(2026, 6, 2)));

            obraRepository.saveAll(Arrays.asList(viaducto, centroLogistico, subestacion, programaSst));
            System.out.println(">> Se han precargado las obras y sus fotos de seguimiento.");
        }

    }

    private Obra obraConCodigo(String nombre, String estado, int progreso, Usuario cliente) {
        Obra obra = new Obra(nombre, estado, cliente.getId(), progreso);
        obra.setCliente(cliente.getNombre() == null || cliente.getNombre().isBlank()
                ? cliente.getEmail() : cliente.getNombre());
        obra.setCodigoHash(trackingCodeService.generate().hash());
        return obra;
    }

}
