package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.config.DatosIniciales;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.MedicionRequest;
import com.induztek.protocolos.dto.PruebaRequest;
import com.induztek.protocolos.dto.PruebaResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.Instrumento;
import com.induztek.protocolos.model.Medicion;
import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.repository.InstrumentoRepository;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import com.induztek.protocolos.util.CalculadorResultados;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PruebaService {

    private static final int LARGO_MAXIMO_ELEMENTO = 30;

    private final ProtocoloRepository protocoloRepository;
    private final PruebaRepository pruebaRepository;
    private final TipoPruebaRepository tipoPruebaRepository;
    private final ParametroRepository parametroRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    private final InstrumentoRepository instrumentoRepository;
    private final InstrumentoService instrumentoService;
    private final ActorService actorService;
    private final EquiposCliente equiposCliente;
    private final ProtocoloPermisos permisos;
    private final ProtocoloMapper mapper;

    public PruebaResponse registrar(Long actorId, Long protocoloId, PruebaRequest datos) {
        UsuarioExterno actor = actorService.exigirPersonalActivo(actorId);
        Protocolo protocolo = buscarProtocolo(protocoloId);
        permisos.exigirEditable(actor, protocolo);

        TipoPrueba tipo = buscarTipoPrueba(datos.tipoPruebaId());
        EquipoExterno equipo = equiposCliente.buscar(protocolo.getEquipoId())
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El equipo del protocolo ya no existe"));
        if (!tipoEquipoPruebaRepository.existsByTipoEquipoIdAndTipoPruebaId(equipo.tipoEquipoId(), tipo.getId())) {
            throw error(HttpStatus.BAD_REQUEST, "La prueba " + tipo.getCodigo()
                    + " no corresponde a un equipo de tipo " + equipo.tipoEquipoNombre());
        }
        String elemento = validarElemento(datos.elemento());
        MomentoPrueba momento = datos.momento() == null ? MomentoPrueba.UNICA : datos.momento();
        if (protocolo.getVisita().getTipoVisita() == TipoVisita.SOLO_PRUEBAS && momento != MomentoPrueba.UNICA) {
            throw error(HttpStatus.BAD_REQUEST, "En una visita solo de pruebas el momento de la medición es único");
        }
        if (datos.realizada() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar si la prueba se realizó");
        }
        if (pruebaRepository.existsByProtocoloIdAndTipoPruebaIdAndElementoAndMomento(
                protocoloId, tipo.getId(), elemento, momento)) {
            throw error(HttpStatus.CONFLICT, "Esa prueba ya está registrada para ese elemento y momento");
        }

        Prueba prueba = Prueba.builder()
                .protocolo(protocolo)
                .tipoPrueba(tipo)
                .elemento(elemento)
                .momento(momento)
                .realizada(datos.realizada())
                .comentario(limpiar(datos.comentario()))
                .build();
        if (datos.realizada()) {
            completarPruebaRealizada(prueba, datos, equipo, protocolo);
        } else {
            String motivo = limpiar(datos.motivoNoRealizada());
            if (motivo == null) {
                throw error(HttpStatus.BAD_REQUEST, "Una prueba no realizada necesita un motivo");
            }
            prueba.setMotivoNoRealizada(motivo);
        }
        return mapper.aPrueba(pruebaRepository.save(prueba));
    }

    public void eliminar(Long actorId, Long protocoloId, Long pruebaId) {
        UsuarioExterno actor = actorService.exigirPersonalActivo(actorId);
        Protocolo protocolo = buscarProtocolo(protocoloId);
        permisos.exigirEditable(actor, protocolo);
        Prueba prueba = pruebaRepository.findById(pruebaId)
                .filter(p -> p.getProtocolo().getId().equals(protocoloId))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Prueba no encontrada en ese protocolo"));
        pruebaRepository.delete(prueba);
    }

    private void completarPruebaRealizada(Prueba prueba, PruebaRequest datos, EquipoExterno equipo, Protocolo protocolo) {
        TipoPrueba tipo = prueba.getTipoPrueba();
        Instrumento instrumento = buscarInstrumento(datos.instrumentoId());
        LocalDate fechaPrueba = protocolo.getFechaHoraPrueba().toLocalDate();
        if (!instrumentoService.estaVigente(instrumento.getId(), fechaPrueba)) {
            throw error(HttpStatus.BAD_REQUEST, "El instrumento no tiene calibración vigente a la fecha de la prueba ("
                    + fechaPrueba + ")");
        }
        boolean noOpera = Boolean.TRUE.equals(datos.noOpera());
        if (noOpera && !CalculadorResultados.OPERACION_RELE.equals(tipo.getCodigo())) {
            throw error(HttpStatus.BAD_REQUEST, "Solo la prueba de operación de relé puede marcarse como \"no opera\"");
        }

        Map<String, Parametro> parametros = parametroRepository.findByTipoPruebaIdOrderByIdAsc(tipo.getId()).stream()
                .collect(Collectors.toMap(Parametro::getCodigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        List<Medicion> mediciones = new ArrayList<>();
        List<MedicionRequest> pedidas = datos.mediciones() == null ? List.of() : datos.mediciones();
        for (MedicionRequest pedida : pedidas) {
            if (pedida.parametro() == null || !parametros.containsKey(pedida.parametro())) {
                throw error(HttpStatus.BAD_REQUEST, "El parámetro " + pedida.parametro()
                        + " no pertenece a la prueba " + tipo.getCodigo());
            }
            if (pedida.valor() == null || pedida.valor().signum() <= 0) {
                throw error(HttpStatus.BAD_REQUEST, "El valor de " + pedida.parametro() + " debe ser mayor que cero");
            }
            if (valores.put(pedida.parametro(), pedida.valor()) != null) {
                throw error(HttpStatus.BAD_REQUEST, "El parámetro " + pedida.parametro() + " está repetido");
            }
            mediciones.add(Medicion.builder()
                    .prueba(prueba)
                    .parametro(parametros.get(pedida.parametro()))
                    .valor(pedida.valor())
                    .build());
        }
        for (String codigo : parametros.keySet()) {
            if (!valores.containsKey(codigo) && esRequerido(codigo, equipo, noOpera)) {
                throw error(HttpStatus.BAD_REQUEST, "Falta la medición " + codigo);
            }
        }

        if (CalculadorResultados.DEVANADOS.equals(tipo.getCodigo())) {
            if (datos.resultadoManual() == null) {
                throw error(HttpStatus.BAD_REQUEST,
                        "El resultado de la resistencia de devanados lo indica el ingeniero");
            }
            prueba.setResultado(datos.resultadoManual());
        } else {
            BigDecimal maximo = equipo.maxContactosUohm();
            if (CalculadorResultados.CONTACTOS.equals(tipo.getCodigo()) && (maximo == null || maximo.signum() <= 0)) {
                throw error(HttpStatus.BAD_REQUEST,
                        "El equipo no tiene registrado el máximo de resistencia de contactos");
            }
            CalculadorResultados.Calculo calculo = CalculadorResultados.calcular(tipo.getCodigo(), valores, maximo, noOpera);
            prueba.setResultado(calculo.resultado());
            prueba.setValorCalculado(calculo.valor());
        }
        prueba.setInstrumento(instrumento);
        prueba.getMediciones().addAll(mediciones);
    }

    private boolean esRequerido(String codigoParametro, EquipoExterno equipo, boolean noOpera) {
        boolean sinDiezMinutos = equipo.tipoEquipoId() == DatosIniciales.INTERRUPTOR
                || equipo.tipoEquipoId() == DatosIniciales.CELDA_MT;
        if ("LECTURA_10MIN".equals(codigoParametro) && sinDiezMinutos) {
            return false;
        }
        return !("TIEMPO_MEDIDO".equals(codigoParametro) && noOpera);
    }

    private Protocolo buscarProtocolo(Long id) {
        return protocoloRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Protocolo no encontrado"));
    }

    private TipoPrueba buscarTipoPrueba(Long id) {
        if (id == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el tipo de prueba");
        }
        return tipoPruebaRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El tipo de prueba indicado no existe"));
    }

    private Instrumento buscarInstrumento(Long id) {
        if (id == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el instrumento usado");
        }
        return instrumentoRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El instrumento indicado no existe"));
    }

    private String validarElemento(String elemento) {
        String limpio = limpiar(elemento);
        if (limpio == null || limpio.length() > LARGO_MAXIMO_ELEMENTO) {
            throw error(HttpStatus.BAD_REQUEST,
                    "El elemento probado es obligatorio (máximo " + LARGO_MAXIMO_ELEMENTO + " caracteres)");
        }
        return limpio;
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
