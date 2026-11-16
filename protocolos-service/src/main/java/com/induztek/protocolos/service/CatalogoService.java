package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.ParametroResponse;
import com.induztek.protocolos.dto.TipoPruebaResponse;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final TipoPruebaRepository tipoPruebaRepository;
    private final ParametroRepository parametroRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;

    public List<TipoPruebaResponse> listarTiposPrueba(Long tipoEquipoId) {
        List<TipoPrueba> tipos = tipoPruebaRepository.findAllByOrderByNombreAsc();
        if (tipoEquipoId != null) {
            Set<Long> permitidos = tipoEquipoPruebaRepository.findByTipoEquipoId(tipoEquipoId).stream()
                    .map(TipoEquipoPrueba::getTipoPrueba)
                    .map(TipoPrueba::getId)
                    .collect(Collectors.toSet());
            tipos = tipos.stream().filter(tipo -> permitidos.contains(tipo.getId())).toList();
        }
        return tipos.stream().map(this::aRespuesta).toList();
    }

    private TipoPruebaResponse aRespuesta(TipoPrueba tipo) {
        List<ParametroResponse> parametros = parametroRepository.findByTipoPruebaIdOrderByIdAsc(tipo.getId()).stream()
                .map(p -> new ParametroResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getUnidad()))
                .toList();
        return new TipoPruebaResponse(tipo.getId(), tipo.getCodigo(), tipo.getNombre(), parametros);
    }
}
