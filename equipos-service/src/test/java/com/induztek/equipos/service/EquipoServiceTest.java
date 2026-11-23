package com.induztek.equipos.service;

import com.induztek.equipos.dto.EquipoRequest;
import com.induztek.equipos.dto.EquipoResponse;
import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Equipo;
import com.induztek.equipos.model.Instalacion;
import com.induztek.equipos.model.Region;
import com.induztek.equipos.model.TipoEquipo;
import com.induztek.equipos.repository.EquipoRepository;
import com.induztek.equipos.repository.InstalacionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private InstalacionRepository instalacionRepository;

    @Mock
    private TipoEquipoRepository tipoEquipoRepository;

    @Mock
    private ActorService actorService;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private EquipoService servicio;

    private final Region region = Region.builder().id(7L).nombre("Metropolitana de Santiago").build();
    private final Comuna comuna = Comuna.builder().id(10L).nombre("Lampa").region(region).build();
    private final Instalacion planta = Instalacion.builder()
            .id(1L).clienteId(5L).nombre("Planta Lampa").comuna(comuna).build();
    private final Instalacion bodega = Instalacion.builder()
            .id(2L).clienteId(6L).nombre("Bodega Norte").comuna(comuna).build();
    private final TipoEquipo transformador = TipoEquipo.builder().id(1L).nombre("Transformador").clasificacion("PRIMARIO").build();
    private final TipoEquipo rele = TipoEquipo.builder().id(5L).nombre("Relé de protección").clasificacion("SECUNDARIO").build();

    @BeforeEach
    void preparar() {
        servicio = new EquipoService(equipoRepository, instalacionRepository, tipoEquipoRepository, reloj, actorService);
    }

    private Equipo equipo(Long id, Instalacion instalacion, String tag, TipoEquipo tipo) {
        return Equipo.builder()
                .id(id)
                .instalacion(instalacion)
                .tag(tag)
                .tipoEquipo(tipo)
                .marca("ABB")
                .modelo("DTR-500")
                .numeroSerie("SN-" + id)
                .build();
    }

    private EquipoRequest peticion(Long instalacionId, String tag, Long tipoId) {
        return new EquipoRequest(instalacionId, tag, tipoId, " ABB ", " DTR-500 ", " SN-1 ", 2020,
                new BigDecimal("1000"), new BigDecimal("23"), new BigDecimal("0.4"),
                new BigDecimal("50"), new BigDecimal("100"), " Sin novedades ");
    }

    private EquipoRequest conNumeros(Integer anio, String potencia, String tensionP, String tensionS,
                                     String corriente, String contactos) {
        return new EquipoRequest(1L, "TR-01", 1L, null, null, null, anio,
                numero(potencia), numero(tensionP), numero(tensionS), numero(corriente), numero(contactos), null);
    }

    private BigDecimal numero(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

    private void instalacionYTipoExisten() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(tipoEquipoRepository.findById(1L)).thenReturn(Optional.of(transformador));
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void listar_sinFiltros_devuelveTodosLosEquipos() {
        when(equipoRepository.findAllByOrderByTagAsc())
                .thenReturn(List.of(equipo(1L, planta, "TR-01", transformador), equipo(2L, bodega, "RL-01", rele)));

        assertEquals(2, servicio.listar(null, null, null, null).size());
        verify(equipoRepository, never()).findByInstalacionClienteIdOrderByTagAsc(any());
    }

    @Test
    void listar_conCliente_usaLaConsultaPorEmpresa() {
        when(equipoRepository.findByInstalacionClienteIdOrderByTagAsc(5L))
                .thenReturn(List.of(equipo(1L, planta, "TR-01", transformador)));

        List<EquipoResponse> respuesta = servicio.listar(5L, null, null, null);

        assertEquals(1, respuesta.size());
        assertEquals(5L, respuesta.get(0).clienteId());
        verify(equipoRepository, never()).findAllByOrderByTagAsc();
    }

    @Test
    void listar_filtraPorInstalacion() {
        when(equipoRepository.findAllByOrderByTagAsc())
                .thenReturn(List.of(equipo(1L, planta, "TR-01", transformador), equipo(2L, bodega, "RL-01", rele)));

        List<EquipoResponse> respuesta = servicio.listar(null, 2L, null, null);

        assertEquals(1, respuesta.size());
        assertEquals("RL-01", respuesta.get(0).tag());
    }

    @Test
    void listar_filtraPorTipoDeEquipo() {
        when(equipoRepository.findAllByOrderByTagAsc())
                .thenReturn(List.of(equipo(1L, planta, "TR-01", transformador), equipo(2L, bodega, "RL-01", rele)));

        List<EquipoResponse> respuesta = servicio.listar(null, null, 1L, null);

        assertEquals(1, respuesta.size());
        assertEquals("Transformador", respuesta.get(0).tipoEquipoNombre());
    }

    @Test
    void listar_buscaPorTagMarcaModeloOSerieSinImportarMayusculas() {
        Equipo sinDatos = Equipo.builder().id(3L).instalacion(planta).tag("CB-09").tipoEquipo(transformador).build();
        when(equipoRepository.findAllByOrderByTagAsc())
                .thenReturn(List.of(equipo(1L, planta, "TR-01", transformador), sinDatos));

        assertEquals(1, servicio.listar(null, null, null, " tr-01 ").size());
        assertEquals(1, servicio.listar(null, null, null, "abb").size());
        assertEquals(1, servicio.listar(null, null, null, "dtr").size());
        assertEquals(1, servicio.listar(null, null, null, "sn-1").size());
        assertEquals(1, servicio.listar(null, null, null, "cb-09").size());
        assertEquals(0, servicio.listar(null, null, null, "no existe").size());
    }

    @Test
    void obtener_conIdExistente_devuelveElEquipo() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo(1L, planta, "TR-01", transformador)));

        EquipoResponse respuesta = servicio.obtener(1L, null);

        assertEquals("TR-01", respuesta.tag());
        assertEquals("Planta Lampa", respuesta.instalacionNombre());
        assertEquals("PRIMARIO", respuesta.clasificacion());
    }

    @Test
    void obtener_conElClienteDueno_devuelveElEquipo() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo(1L, planta, "TR-01", transformador)));

        assertEquals(1L, servicio.obtener(1L, 5L).id());
    }

    @Test
    void obtener_conOtroCliente_lanza404() {
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo(1L, planta, "TR-01", transformador)));

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(1L, 6L));
    }

    @Test
    void obtener_conIdInexistente_lanza404() {
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(99L, null));
    }

    @Test
    void crear_conDatosValidos_guardaElEquipoLimpiandoTextos() {
        instalacionYTipoExisten();
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));

        EquipoResponse respuesta = servicio.crear(peticion(1L, "  TR-01 ", 1L));

        assertEquals("TR-01", respuesta.tag());
        assertEquals("ABB", respuesta.marca());
        assertEquals("DTR-500", respuesta.modelo());
        assertEquals("SN-1", respuesta.numeroSerie());
        assertEquals(2020, respuesta.anioFabricacion());
        assertEquals(new BigDecimal("1000"), respuesta.potenciaKva());
        assertEquals("Sin novedades", respuesta.observaciones());
        assertEquals(5L, respuesta.clienteId());
    }

    @Test
    void crear_conDatosDeplacaOpcionales_losGuardaComoNulos() {
        instalacionYTipoExisten();
        when(equipoRepository.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));

        EquipoResponse respuesta = servicio.crear(conNumeros(null, null, null, null, null, null));

        assertNull(respuesta.marca());
        assertNull(respuesta.anioFabricacion());
        assertNull(respuesta.potenciaKva());
    }

    @Test
    void crear_sinInstalacion_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(null, "TR-01", 1L)));
    }

    @Test
    void crear_conInstalacionInexistente_lanza400() {
        when(instalacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(99L, "TR-01", 1L)));
    }

    @Test
    void crear_sinTag_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(1L, "  ", 1L)));
    }

    @Test
    void crear_conTagNulo_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(1L, null, 1L)));
    }

    @Test
    void crear_sinTipoDeEquipo_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(1L, "TR-01", null)));
    }

    @Test
    void crear_conTipoDeEquipoInexistente_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(planta));
        when(tipoEquipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(1L, "TR-01", 99L)));
    }

    @Test
    void crear_conAnioAnteriorA1900_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(1899, null, null, null, null, null)));
    }

    @Test
    void crear_conAnioFuturo_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2027, null, null, null, null, null)));
    }

    @Test
    void crear_conPotenciaCero_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2020, "0", null, null, null, null)));
    }

    @Test
    void crear_conTensionPrimariaNegativa_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2020, null, "-1", null, null, null)));
    }

    @Test
    void crear_conTensionSecundariaCero_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2020, null, null, "0", null, null)));
    }

    @Test
    void crear_conCorrienteNegativa_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2020, null, null, null, "-5", null)));
    }

    @Test
    void crear_conMaximoDeContactosCero_lanza400() {
        instalacionYTipoExisten();

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(conNumeros(2020, null, null, null, null, "0")));
    }

    @Test
    void crear_conTagRepetidoEnLaInstalacion_lanza409() {
        instalacionYTipoExisten();
        when(equipoRepository.existsByInstalacionIdAndTag(1L, "TR-01")).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(peticion(1L, "TR-01", 1L)));
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void actualizar_conDatosValidos_cambiaElEquipo() {
        Equipo existente = equipo(3L, planta, "TR-01", transformador);
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(tipoEquipoRepository.findById(5L)).thenReturn(Optional.of(rele));
        when(equipoRepository.save(existente)).thenReturn(existente);

        EquipoResponse respuesta = servicio.actualizar(1L, 3L, peticion(9L, " RL-02 ", 5L));

        assertEquals("RL-02", respuesta.tag());
        assertEquals("Relé de protección", respuesta.tipoEquipoNombre());
        assertEquals(1L, respuesta.instalacionId());
    }

    @Test
    void actualizar_conIdInexistente_lanza404() {
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.actualizar(1L, 99L, peticion(1L, "TR-01", 1L)));
    }

    @Test
    void actualizar_conTagVacio_lanza400() {
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(equipo(3L, planta, "TR-01", transformador)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizar(1L, 3L, peticion(1L, "", 1L)));
    }

    @Test
    void actualizar_conTipoInexistente_lanza400() {
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(equipo(3L, planta, "TR-01", transformador)));
        when(tipoEquipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizar(1L, 3L, peticion(1L, "TR-01", 99L)));
    }

    @Test
    void actualizar_conTagDeOtroEquipoDeLaMismaInstalacion_lanza409() {
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(equipo(3L, planta, "TR-01", transformador)));
        when(tipoEquipoRepository.findById(1L)).thenReturn(Optional.of(transformador));
        when(equipoRepository.existsByInstalacionIdAndTagAndIdNot(1L, "TR-02", 3L)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.actualizar(1L, 3L, peticion(1L, "TR-02", 1L)));
    }

    @Test
    void actualizar_siNoEsElJefeTecnico_lanza403SinTocarElEquipo() {
        when(actorService.exigirJefeTecnico(2L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Jefe técnico puede realizar esta acción"));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.actualizar(2L, 3L, peticion(1L, "TR-01", 1L)));
        verify(equipoRepository, never()).save(any());
    }
}
