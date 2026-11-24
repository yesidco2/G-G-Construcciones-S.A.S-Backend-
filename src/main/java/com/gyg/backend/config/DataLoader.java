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

            Obra centroLogistico = obraConCodigo("Centro Logístico Industrial", "EN_PROGRESO", 58, cliente);

            Obra subestacion = obraConCodigo("Subestación Energía Norte", "FINALIZADA", 100, cliente);

            Obra programaSst = obraConCodigo("Programa SST Corredor Sur", "EN_PROGRESO", 40, cliente);
            programaSst.setCategoria("SST");

            obraRepository.saveAll(Arrays.asList(viaducto, centroLogistico, subestacion, programaSst));
            System.out.println(">> Se han precargado las obras de seguimiento.");
        }

    }

    private Obra obraConCodigo(String nombre, String estado, int progreso, Usuario cliente) {
        Obra obra = new Obra(nombre, estado, cliente.getId(), progreso);
        obra.setCliente(cliente.getNombre() == null || cliente.getNombre().isBlank()
                ? cliente.getEmail() : cliente.getNombre());
        var code = trackingCodeService.generate();
        obra.setCodigoHash(code.hash());
        obra.setCodigoCifrado(trackingCodeService.encrypt(code.value(), code.hash()));
        return obra;
    }

}
