package com.induztek.equipos.config;

import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Region;
import com.induztek.equipos.model.TipoEquipo;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.RegionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    public static final String PRIMARIO = "PRIMARIO";
    public static final String SECUNDARIO = "SECUNDARIO";

    static final List<String> REGIONES = List.of(
            "Arica y Parinacota", "Tarapacá", "Antofagasta", "Atacama", "Coquimbo", "Valparaíso",
            "Metropolitana de Santiago", "Libertador General Bernardo O'Higgins", "Maule", "Ñuble",
            "Biobío", "La Araucanía", "Los Ríos", "Los Lagos", "Aysén del General Carlos Ibáñez del Campo",
            "Magallanes y de la Antártica Chilena");

    static final Map<String, List<String>> COMUNAS = Map.of(
            "Metropolitana de Santiago", List.of("Santiago", "Lampa", "Colina", "Quilicura", "Pudahuel", "Maipú",
                    "Til Til", "San Bernardo", "Puente Alto", "Providencia", "Las Condes"),
            "Valparaíso", List.of("Valparaíso", "Viña del Mar", "Quintero"),
            "Libertador General Bernardo O'Higgins", List.of("Rancagua"),
            "Biobío", List.of("Concepción", "Los Ángeles"));

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;

    @Override
    public void run(String... args) {
        if (regionRepository.count() == 0) {
            List<Region> regiones = regionRepository.saveAll(
                    REGIONES.stream().map(nombre -> Region.builder().nombre(nombre).build()).toList());
            List<Comuna> comunas = new ArrayList<>();
            for (Region region : regiones) {
                for (String nombre : COMUNAS.getOrDefault(region.getNombre(), List.of())) {
                    comunas.add(Comuna.builder().nombre(nombre).region(region).build());
                }
            }
            comunaRepository.saveAll(comunas);
        }
        if (tipoEquipoRepository.count() == 0) {
            tipoEquipoRepository.saveAll(List.of(
                    tipo("Transformador", PRIMARIO),
                    tipo("Interruptor", PRIMARIO),
                    tipo("Celda MT", PRIMARIO),
                    tipo("Cable", PRIMARIO),
                    tipo("Relé de protección", SECUNDARIO)));
        }
    }

    private TipoEquipo tipo(String nombre, String clasificacion) {
        return TipoEquipo.builder().nombre(nombre).clasificacion(clasificacion).build();
    }
}
