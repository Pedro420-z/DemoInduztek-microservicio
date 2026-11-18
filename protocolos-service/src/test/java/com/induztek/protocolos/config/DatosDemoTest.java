package com.induztek.protocolos.config;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.client.UsuariosCliente;
import com.induztek.protocolos.dto.ComunaExterno;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.InstalacionExterno;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.repository.InstrumentoRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.service.CierreVisitaService;
import com.induztek.protocolos.service.InstrumentoService;
import com.induztek.protocolos.service.ProtocoloService;
import com.induztek.protocolos.service.PruebaService;
import com.induztek.protocolos.service.VisitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatosDemoTest {

    @Mock
    private UsuariosCliente usuariosCliente;
    @Mock
    private EquiposCliente equiposCliente;
    @Mock
    private InstrumentoService instrumentoService;
    @Mock
    private InstrumentoRepository instrumentoRepository;
    @Mock
    private TipoPruebaRepository tipoPruebaRepository;
    @Mock
    private VisitaRepository visitaRepository;
    @Mock
    private VisitaService visitaService;
    @Mock
    private ProtocoloService protocoloService;
    @Mock
    private PruebaService pruebaService;
    @Mock
    private CierreVisitaService cierreVisitaService;

    private static UsuarioExterno usuario(Long id, String rol, String correo) {
        return new UsuarioExterno(id, 1L, rol, rol, "1-9", "Nombre", correo, null, true, null, null, null, false);
    }

    private DatosDemo demo(boolean activo, int reintentos) {
        return new DatosDemo(usuariosCliente, equiposCliente, instrumentoService, instrumentoRepository,
                tipoPruebaRepository, visitaRepository, visitaService, protocoloService, pruebaService,
                cierreVisitaService, activo, reintentos, 0, DatosDemo.CORREO_JEFE);
    }

    private List<UsuarioExterno> usuariosDeEjemplo() {
        return List.of(
                usuario(1L, "JEFE_TECNICO", DatosDemo.CORREO_JEFE),
                usuario(2L, "INGENIERO", DatosDemo.CORREO_INGENIERO),
                usuario(3L, "TECNICO", DatosDemo.CORREO_TECNICO),
                usuario(4L, "CLIENTE", DatosDemo.CORREO_ENVASADORA),
                usuario(5L, "CLIENTE", DatosDemo.CORREO_CLINICA));
    }

    private void prepararServiciosVacios() {
        when(usuariosCliente.listar()).thenReturn(usuariosDeEjemplo());
        when(equiposCliente.comunas()).thenReturn(List.of(new ComunaExterno(1L, "Lampa"), new ComunaExterno(2L, "Providencia")));
        when(instrumentoService.listar(any())).thenReturn(List.of());
        when(instrumentoService.crear(anyLong(), any())).thenReturn(
                new InstrumentoResponse(10L, "i", null, null, "s", null, null, true));
        when(equiposCliente.instalaciones(anyLong())).thenReturn(List.of());
        when(equiposCliente.crearInstalacion(any())).thenReturn(
                new InstalacionExterno(20L, 4L, "Planta", null, 1L, "Lampa", 1L, "RM", null));
        when(equiposCliente.equipos(anyLong())).thenReturn(List.of());
        when(equiposCliente.crearEquipo(any())).thenReturn(new EquipoExterno(30L, 20L, "Planta", 4L, "TR", 1L,
                "Transformador", "PRIMARIO", null, null, null, null, null, null, null, null, null, null));
        List<TipoPrueba> tipos = new ArrayList<>();
        long id = 1;
        for (String codigo : List.of("AISLACION", "TTR", "DEVANADOS", "CONTACTOS", "OPERACION_RELE")) {
            tipos.add(TipoPrueba.builder().id(id++).codigo(codigo).nombre(codigo).build());
        }
        when(tipoPruebaRepository.findAll()).thenReturn(tipos);
        when(visitaService.crear(anyLong(), any())).thenReturn(new VisitaResponse(40L, "OT", 20L,
                LocalDate.of(2026, 10, 14), null, 2L, List.of(), null, false, null));
        when(protocoloService.crear(anyLong(), any())).thenReturn(new ProtocoloResponse(50L, "PT", 40L, "OT", 30L,
                3L, null, null, null, null, null, null, null, null, false, List.of(), List.of()));
    }

    @Test
    void run_inactivo_noHaceNada() throws Exception {
        demo(false, 3).run();

        verify(usuariosCliente, never()).listar();
    }

    @Test
    void run_siYaEstanLosDatos_noVuelveACargar() throws Exception {
        when(visitaRepository.existsByNumeroOt(DatosDemo.OT_LAMPA)).thenReturn(true);

        demo(true, 3).run();

        verify(usuariosCliente, never()).listar();
    }

    @Test
    void run_conServiciosListos_cargaInstrumentosEquiposYActas() throws Exception {
        prepararServiciosVacios();

        demo(true, 3).run();

        verify(instrumentoService, times(4)).crear(anyLong(), any());
        verify(instrumentoService, times(4)).registrarCalibracion(anyLong(), anyLong(), any());
        verify(equiposCliente, times(2)).crearInstalacion(any());
        verify(equiposCliente, times(4)).crearEquipo(any());
        verify(visitaService, times(2)).crear(anyLong(), any());
        verify(protocoloService, times(4)).crear(anyLong(), any());
        verify(protocoloService, times(3)).formalizar(anyLong(), anyLong());
        verify(cierreVisitaService, times(2)).cerrar(anyLong(), anyLong());
        verify(visitaService).registrarRecepcion(anyLong(), any());
        verify(pruebaService, atLeastOnce()).registrar(anyLong(), anyLong(), any());
    }

    @Test
    void run_siYaExistenInstrumentosEInstalaciones_losReutiliza() throws Exception {
        prepararServiciosVacios();
        when(instrumentoService.listar(any())).thenReturn(List.of(
                new InstrumentoResponse(11L, "i", null, null, "MG-100245", null, null, true),
                new InstrumentoResponse(12L, "i", null, null, "DL-778812", null, null, true),
                new InstrumentoResponse(13L, "i", null, null, "TT-330991", null, null, true),
                new InstrumentoResponse(14L, "i", null, null, "OM-356120", null, null, true)));
        when(equiposCliente.instalaciones(4L)).thenReturn(List.of(
                new InstalacionExterno(20L, 4L, "Planta Lampa", null, 1L, "Lampa", 1L, "RM", null)));
        when(equiposCliente.equipos(4L)).thenReturn(List.of(
                new EquipoExterno(31L, 20L, "Planta Lampa", 4L, "TR-101", 1L, "Transformador", "PRIMARIO",
                        null, null, null, null, null, null, null, null, null, null)));

        demo(true, 3).run();

        verify(instrumentoService, never()).crear(anyLong(), any());
        verify(equiposCliente, times(1)).crearInstalacion(any());
        verify(equiposCliente, times(3)).crearEquipo(any());
    }

    @Test
    void run_siFaltanLasCuentasDeEjemplo_reintentaYNoCargaNada() throws Exception {
        when(usuariosCliente.listar()).thenReturn(List.of(usuario(1L, "JEFE_TECNICO", DatosDemo.CORREO_JEFE)));

        demo(true, 3).run();

        verify(usuariosCliente, times(3)).listar();
        verify(visitaService, never()).crear(anyLong(), any());
    }

    @Test
    void run_siFaltanLasComunas_noCarga() throws Exception {
        when(usuariosCliente.listar()).thenReturn(usuariosDeEjemplo());
        when(equiposCliente.comunas()).thenReturn(List.of());

        demo(true, 2).run();

        verify(visitaService, never()).crear(anyLong(), any());
    }

    @Test
    void run_siUnServicioNoResponde_reintentaHastaAgotarLosIntentos() throws Exception {
        when(usuariosCliente.listar()).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "caído"));

        demo(true, 2).run();

        verify(usuariosCliente, times(2)).listar();
    }
}
