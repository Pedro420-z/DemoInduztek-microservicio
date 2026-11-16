package com.induztek.equipos.service;

import com.induztek.equipos.dto.EquipoRequest;
import com.induztek.equipos.dto.EquipoResponse;
import com.induztek.equipos.model.Equipo;
import com.induztek.equipos.model.Instalacion;
import com.induztek.equipos.model.TipoEquipo;
import com.induztek.equipos.repository.EquipoRepository;
import com.induztek.equipos.repository.InstalacionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private static final int ANIO_MINIMO = 1900;

    private final EquipoRepository equipoRepository;
    private final InstalacionRepository instalacionRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final Clock reloj;

    public List<EquipoResponse> listar(Long clienteId, Long instalacionId, Long tipoEquipoId, String buscar) {
        String texto = buscar == null ? "" : buscar.trim().toLowerCase();
        List<Equipo> base = clienteId == null
                ? equipoRepository.findAllByOrderByTagAsc()
                : equipoRepository.findByInstalacionClienteIdOrderByTagAsc(clienteId);
        return base.stream()
                .filter(equipo -> instalacionId == null || equipo.getInstalacion().getId().equals(instalacionId))
                .filter(equipo -> tipoEquipoId == null || equipo.getTipoEquipo().getId().equals(tipoEquipoId))
                .filter(equipo -> texto.isEmpty() || coincide(equipo, texto))
                .map(this::aRespuesta)
                .toList();
    }

    public EquipoResponse obtener(Long id, Long clienteId) {
        return aRespuesta(buscar(id, clienteId));
    }

    public EquipoResponse crear(EquipoRequest datos) {
        Instalacion instalacion = buscarInstalacion(datos.instalacionId());
        String tag = validarTag(datos.tag());
        TipoEquipo tipo = buscarTipo(datos.tipoEquipoId());
        validarNumeros(datos);
        if (equipoRepository.existsByInstalacionIdAndTag(instalacion.getId(), tag)) {
            throw error(HttpStatus.CONFLICT, "Ya existe un equipo con ese TAG en la instalación");
        }
        Equipo nuevo = Equipo.builder().instalacion(instalacion).tag(tag).tipoEquipo(tipo).build();
        copiarDatos(nuevo, datos);
        return aRespuesta(equipoRepository.save(nuevo));
    }

    public EquipoResponse actualizar(Long id, EquipoRequest datos) {
        Equipo equipo = buscar(id, null);
        String tag = validarTag(datos.tag());
        TipoEquipo tipo = buscarTipo(datos.tipoEquipoId());
        validarNumeros(datos);
        if (equipoRepository.existsByInstalacionIdAndTagAndIdNot(equipo.getInstalacion().getId(), tag, id)) {
            throw error(HttpStatus.CONFLICT, "Ya existe un equipo con ese TAG en la instalación");
        }
        equipo.setTag(tag);
        equipo.setTipoEquipo(tipo);
        copiarDatos(equipo, datos);
        return aRespuesta(equipoRepository.save(equipo));
    }

    private boolean coincide(Equipo equipo, String texto) {
        return contiene(equipo.getTag(), texto)
                || contiene(equipo.getMarca(), texto)
                || contiene(equipo.getModelo(), texto)
                || contiene(equipo.getNumeroSerie(), texto);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    private Equipo buscar(Long id, Long clienteId) {
        return equipoRepository.findById(id)
                .filter(equipo -> clienteId == null || clienteId.equals(equipo.getInstalacion().getClienteId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
    }

    private Instalacion buscarInstalacion(Long instalacionId) {
        if (instalacionId == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar la instalación");
        }
        return instalacionRepository.findById(instalacionId)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "La instalación indicada no existe"));
    }

    private TipoEquipo buscarTipo(Long tipoEquipoId) {
        if (tipoEquipoId == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el tipo de equipo");
        }
        return tipoEquipoRepository.findById(tipoEquipoId)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El tipo de equipo indicado no existe"));
    }

    private String validarTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El TAG del equipo es obligatorio");
        }
        return tag.trim();
    }

    private void validarNumeros(EquipoRequest datos) {
        int anioActual = LocalDate.now(reloj).getYear();
        if (datos.anioFabricacion() != null
                && (datos.anioFabricacion() < ANIO_MINIMO || datos.anioFabricacion() > anioActual)) {
            throw error(HttpStatus.BAD_REQUEST, "El año de fabricación debe estar entre 1900 y " + anioActual);
        }
        validarPositivo(datos.potenciaKva(), "La potencia");
        validarPositivo(datos.tensionPrimariaKv(), "La tensión primaria");
        validarPositivo(datos.tensionSecundariaKv(), "La tensión secundaria");
        validarPositivo(datos.corrienteNominalA(), "La corriente nominal");
        validarPositivo(datos.maxContactosUohm(), "El máximo de resistencia de contactos");
    }

    private void validarPositivo(BigDecimal valor, String nombre) {
        if (valor != null && valor.signum() <= 0) {
            throw error(HttpStatus.BAD_REQUEST, nombre + " debe ser mayor que cero");
        }
    }

    private void copiarDatos(Equipo equipo, EquipoRequest datos) {
        equipo.setMarca(limpiar(datos.marca()));
        equipo.setModelo(limpiar(datos.modelo()));
        equipo.setNumeroSerie(limpiar(datos.numeroSerie()));
        equipo.setAnioFabricacion(datos.anioFabricacion());
        equipo.setPotenciaKva(datos.potenciaKva());
        equipo.setTensionPrimariaKv(datos.tensionPrimariaKv());
        equipo.setTensionSecundariaKv(datos.tensionSecundariaKv());
        equipo.setCorrienteNominalA(datos.corrienteNominalA());
        equipo.setMaxContactosUohm(datos.maxContactosUohm());
        equipo.setObservaciones(limpiar(datos.observaciones()));
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private EquipoResponse aRespuesta(Equipo equipo) {
        Instalacion instalacion = equipo.getInstalacion();
        TipoEquipo tipo = equipo.getTipoEquipo();
        return new EquipoResponse(
                equipo.getId(),
                instalacion.getId(),
                instalacion.getNombre(),
                instalacion.getClienteId(),
                equipo.getTag(),
                tipo.getId(),
                tipo.getNombre(),
                tipo.getClasificacion(),
                equipo.getMarca(),
                equipo.getModelo(),
                equipo.getNumeroSerie(),
                equipo.getAnioFabricacion(),
                equipo.getPotenciaKva(),
                equipo.getTensionPrimariaKv(),
                equipo.getTensionSecundariaKv(),
                equipo.getCorrienteNominalA(),
                equipo.getMaxContactosUohm(),
                equipo.getObservaciones()
        );
    }
}
