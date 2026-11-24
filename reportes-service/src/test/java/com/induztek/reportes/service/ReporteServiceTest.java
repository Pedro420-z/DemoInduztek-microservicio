package com.induztek.reportes.service;

import com.induztek.reportes.client.EquiposCliente;
import com.induztek.reportes.client.ProtocolosCliente;
import com.induztek.reportes.dto.ActaResponse;
import com.induztek.reportes.dto.EquipoExterno;
import com.induztek.reportes.dto.HistorialEquipoResponse;
import com.induztek.reportes.dto.InstalacionExterno;
import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.ResumenEquipoResponse;
import com.induztek.reportes.dto.UsuarioExterno;
import com.induztek.reportes.dto.VisitaExterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReporteServiceTest {

    @Mock
    private ActorService actorService;
    @Mock
    private EquiposCliente equiposCliente;
    @Mock
    private ProtocolosCliente protocolosCliente;

    @InjectMocks
    private ReporteService servicio;

    private UsuarioExterno cliente;
    private UsuarioExterno tecnico;

    private static UsuarioExterno usuario(Long id, String rol) {
        return new UsuarioExterno(id, 1L, rol, rol, "1-9", "N", "a@b.cl", null, true, null, null, null, false);
    }

    private static EquipoExterno equipo(Long id) {
        return new EquipoExterno(id, 2L, "Planta", 30L, "TR-" + id, 1L, "Transformador", "POTENCIA", null, null, null,
                null, null, null, null, null, null, null);
    }

    private static ProtocoloExterno protocolo(Long id, String estado, String resultado, LocalDateTime fecha) {
        return new ProtocoloExterno(id, "PT-" + id, 4L, "OT-1", 5L, 8L, fecha, null, null, null, null, estado, null,
                resultado, false, List.of(), List.of());
    }

    private static VisitaExterno visita(Long id, LocalDate fecha) {
        return new VisitaExterno(id, "OT-" + id, 2L, fecha, "SOLO_PRUEBAS", 10L, List.of(), LocalDateTime.now(), true, null);
    }

    @BeforeEach
    void preparar() {
        cliente = usuario(30L, "CLIENTE");
        tecnico = usuario(8L, "TECNICO");
        when(actorService.exigirUsuarioActivo(30L)).thenReturn(cliente);
        when(actorService.exigirUsuarioActivo(8L)).thenReturn(tecnico);
        when(actorService.exigirPersonalActivo(8L)).thenReturn(tecnico);
    }

    @Test
    void historialEquipo_paraCliente_filtraPorSuEmpresaYNoMuestraBorradoresNiAnulados() {
        when(equiposCliente.obtener(5L, 30L)).thenReturn(Optional.of(equipo(5L)));
        when(protocolosCliente.protocolos(5L, null, null, null)).thenReturn(List.of(
                protocolo(1L, "FORMALIZADO", "APROBADO", LocalDateTime.of(2026, 1, 1, 10, 0)),
                protocolo(2L, "FORMALIZADO", "RECHAZADO", LocalDateTime.of(2026, 6, 1, 10, 0)),
                protocolo(3L, "REGISTRADO_TERRENO", "APROBADO", LocalDateTime.of(2026, 3, 1, 10, 0)),
                protocolo(4L, "OBSERVADO", null, null),
                protocolo(5L, "BORRADOR", "APROBADO", LocalDateTime.of(2026, 7, 1, 10, 0)),
                protocolo(6L, "ANULADO", "RECHAZADO", LocalDateTime.of(2026, 8, 1, 10, 0))));

        HistorialEquipoResponse historial = servicio.historialEquipo(30L, 5L);

        assertEquals(4, historial.totalProtocolos());
        assertEquals(2L, historial.porResultado().get("APROBADO"));
        assertEquals(1L, historial.porResultado().get("RECHAZADO"));
        assertEquals(2L, historial.protocolos().get(0).id());
        assertEquals(4L, historial.protocolos().get(3).id());
    }

    @Test
    void historialEquipo_deOtraEmpresa_lanza404() {
        when(equiposCliente.obtener(5L, 30L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.historialEquipo(30L, 5L));

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getStatusCode().value());
        verify(protocolosCliente, never()).protocolos(5L, null, null, null);
    }

    @Test
    void historialEquipo_paraPersonal_noFiltraNiPorEmpresaNiPorEstado() {
        when(equiposCliente.obtener(5L, null)).thenReturn(Optional.of(equipo(5L)));
        when(protocolosCliente.protocolos(5L, null, null, null)).thenReturn(List.of(
                protocolo(1L, "BORRADOR", null, LocalDateTime.of(2026, 1, 1, 10, 0))));

        HistorialEquipoResponse historial = servicio.historialEquipo(8L, 5L);

        assertEquals(1, historial.totalProtocolos());
        assertEquals(true, historial.porResultado().isEmpty());
    }

    @Test
    void equipos_paraCliente_usaSuPropioIdAunqueIndiqueOtro() {
        when(equiposCliente.listar(30L)).thenReturn(List.of(equipo(5L), equipo(6L)));
        when(protocolosCliente.protocolos(5L, null, null, null)).thenReturn(List.of(
                protocolo(1L, "FORMALIZADO", "APROBADO", LocalDateTime.of(2026, 1, 1, 10, 0)),
                protocolo(2L, "FORMALIZADO", "OBSERVADO", LocalDateTime.of(2026, 5, 1, 10, 0)),
                protocolo(9L, "BORRADOR", "APROBADO", LocalDateTime.of(2026, 12, 1, 10, 0))));
        when(protocolosCliente.protocolos(6L, null, null, null)).thenReturn(List.of());

        List<ResumenEquipoResponse> resumenes = servicio.equipos(30L, 99L);

        assertEquals(2, resumenes.size());
        assertEquals(2, resumenes.get(0).totalProtocolos());
        assertEquals("PT-2", resumenes.get(0).ultimoNumero());
        assertEquals("OBSERVADO", resumenes.get(0).ultimoResultado());
        assertEquals(0, resumenes.get(1).totalProtocolos());
        assertNull(resumenes.get(1).ultimoNumero());
        verify(equiposCliente, never()).listar(99L);
    }

    @Test
    void equipos_paraPersonal_ignoraLosProtocolosAnuladosYPuedeFiltrarPorCliente() {
        when(equiposCliente.listar(30L)).thenReturn(List.of(equipo(5L)));
        when(protocolosCliente.protocolos(5L, null, null, null)).thenReturn(List.of(
                protocolo(1L, "REGISTRADO_TERRENO", "APROBADO", LocalDateTime.of(2026, 1, 1, 10, 0)),
                protocolo(2L, "ANULADO", "RECHAZADO", LocalDateTime.of(2026, 5, 1, 10, 0))));

        List<ResumenEquipoResponse> resumenes = servicio.equipos(8L, 30L);

        assertEquals(1, resumenes.get(0).totalProtocolos());
        assertEquals("PT-1", resumenes.get(0).ultimoNumero());
    }

    private void instalacionesYVisitas() {
        InstalacionExterno planta = new InstalacionExterno(2L, 30L, "Planta Norte", null, null, null, null, null, null);
        InstalacionExterno bodega = new InstalacionExterno(3L, 30L, "Bodega", null, null, null, null, null, null);
        when(equiposCliente.instalaciones(30L)).thenReturn(List.of(planta, bodega));
        when(equiposCliente.instalaciones(null)).thenReturn(List.of(planta, bodega));
        when(protocolosCliente.visitas(2L, null)).thenReturn(List.of(visita(1L, LocalDate.of(2026, 3, 1))));
        when(protocolosCliente.visitas(3L, null)).thenReturn(List.of(visita(2L, LocalDate.of(2026, 9, 1))));
        when(protocolosCliente.protocolos(null, 1L, null, null)).thenReturn(List.of(
                protocolo(1L, "FORMALIZADO", "APROBADO", LocalDateTime.of(2026, 3, 1, 10, 0)),
                protocolo(9L, "BORRADOR", null, LocalDateTime.of(2026, 3, 2, 10, 0))));
        when(protocolosCliente.protocolos(null, 2L, null, null)).thenReturn(List.of());
    }

    @Test
    void actas_paraCliente_listaLasVisitasCerradasDeSusInstalacionesDeLaMasReciente() {
        instalacionesYVisitas();

        List<ActaResponse> actas = servicio.actas(30L, null);

        assertEquals(2, actas.size());
        assertEquals("Bodega", actas.get(0).instalacionNombre());
        assertEquals("Planta Norte", actas.get(1).instalacionNombre());
        assertEquals(1, actas.get(1).protocolos().size());
        assertEquals("FORMALIZADO", actas.get(1).protocolos().get(0).estado());
    }

    @Test
    void actas_conInstalacionIndicada_soloDevuelveEsa() {
        instalacionesYVisitas();

        List<ActaResponse> actas = servicio.actas(30L, 2L);

        assertEquals(1, actas.size());
        assertEquals("OT-1", actas.get(0).visita().numeroOt());
    }

    @Test
    void actas_conInstalacionAjena_lanza404() {
        instalacionesYVisitas();

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.actas(30L, 77L));

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getStatusCode().value());
    }

    @Test
    void actas_paraPersonal_veTodasLasInstalacionesYTodosLosEstados() {
        instalacionesYVisitas();

        List<ActaResponse> actas = servicio.actas(8L, null);

        assertEquals(2, actas.size());
        assertEquals("BORRADOR", actas.get(1).protocolos().get(0).estado());
    }

    @Test
    void atrasados_devuelveLosProtocolosFueraDePlazoDelMasRecienteAlMasAntiguo() {
        when(protocolosCliente.protocolos(null, null, null, true)).thenReturn(List.of(
                protocolo(1L, "BORRADOR", null, LocalDateTime.of(2026, 1, 1, 10, 0)),
                protocolo(2L, "REGISTRADO_TERRENO", null, LocalDateTime.of(2026, 2, 1, 10, 0))));

        List<ProtocoloExterno> atrasados = servicio.atrasados(8L);

        assertEquals(2L, atrasados.get(0).id());
        verify(actorService).exigirPersonalActivo(8L);
    }
}
