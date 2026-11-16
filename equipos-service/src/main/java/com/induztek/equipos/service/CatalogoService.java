package com.induztek.equipos.service;

import com.induztek.equipos.dto.ComunaResponse;
import com.induztek.equipos.dto.RegionResponse;
import com.induztek.equipos.dto.TipoEquipoResponse;
import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.RegionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;

    public List<RegionResponse> listarRegiones() {
        return regionRepository.findAllByOrderByNombreAsc().stream()
                .map(region -> new RegionResponse(region.getId(), region.getNombre()))
                .toList();
    }

    public List<ComunaResponse> listarComunas(Long regionId) {
        List<Comuna> comunas = regionId == null
                ? comunaRepository.findAllByOrderByNombreAsc()
                : comunaRepository.findByRegionIdOrderByNombreAsc(regionId);
        return comunas.stream()
                .map(comuna -> new ComunaResponse(comuna.getId(), comuna.getNombre(),
                        comuna.getRegion().getId(), comuna.getRegion().getNombre()))
                .toList();
    }

    public List<TipoEquipoResponse> listarTiposEquipo() {
        return tipoEquipoRepository.findAllByOrderByNombreAsc().stream()
                .map(tipo -> new TipoEquipoResponse(tipo.getId(), tipo.getNombre(), tipo.getClasificacion()))
                .toList();
    }
}
