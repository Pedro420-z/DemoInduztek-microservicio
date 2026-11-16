package com.induztek.equipos.service;

import com.induztek.equipos.dto.InstalacionRequest;
import com.induztek.equipos.dto.InstalacionResponse;
import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Instalacion;
import com.induztek.equipos.model.Region;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.InstalacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
class InstalacionServiceTest {

    @Mock
    private InstalacionRepository instalacionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    private InstalacionService servicio;

    private final Region region = Region.builder().id(7L).nombre("Metropolitana de Santiago").build();
    private final Comuna comuna = Comuna.builder().id(10L).nombre("Lampa").region(region).build();

    @BeforeEach
    void preparar() {
        servicio = new InstalacionService(instalacionRepository, comunaRepository);
    }

    private Instalacion instalacion(Long id, Long clienteId) {
        return Instalacion.builder()
                .id(id)
                .clienteId(clienteId)
                .nombre("Planta " + id)
                .ubicacion("Camino a Lampa km 3")
                .comuna(comuna)
                .observacionesAcceso("Inducción 30 min")
                .build();
    }

    private InstalacionRequest peticion(Long clienteId, String nombre, Long comunaId) {
        return new InstalacionRequest(clienteId, nombre, " Camino a Lampa ", comunaId, " Sin señal ");
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void listar_sinCliente_devuelveTodasLasInstalaciones() {
        when(instalacionRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(instalacion(1L, 5L), instalacion(2L, 6L)));

        List<InstalacionResponse> respuesta = servicio.listar(null);

        assertEquals(2, respuesta.size());
        verify(instalacionRepository, never()).findByClienteIdOrderByNombreAsc(any());
    }

    @Test
    void listar_conCliente_soloDevuelveLasDeEsaEmpresa() {
        when(instalacionRepository.findByClienteIdOrderByNombreAsc(5L)).thenReturn(List.of(instalacion(1L, 5L)));

        List<InstalacionResponse> respuesta = servicio.listar(5L);

        assertEquals(1, respuesta.size());
        assertEquals(5L, respuesta.get(0).clienteId());
        verify(instalacionRepository, never()).findAllByOrderByNombreAsc();
    }

    @Test
    void obtener_conIdExistente_devuelveLaInstalacionConSuComunaYRegion() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(instalacion(1L, 5L)));

        InstalacionResponse respuesta = servicio.obtener(1L, null);

        assertEquals("Planta 1", respuesta.nombre());
        assertEquals("Lampa", respuesta.comunaNombre());
        assertEquals("Metropolitana de Santiago", respuesta.regionNombre());
    }

    @Test
    void obtener_conElClienteDueno_devuelveLaInstalacion() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(instalacion(1L, 5L)));

        assertEquals(1L, servicio.obtener(1L, 5L).id());
    }

    @Test
    void obtener_conOtroCliente_lanza404() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(instalacion(1L, 5L)));

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(1L, 6L));
    }

    @Test
    void obtener_conIdInexistente_lanza404() {
        when(instalacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(99L, null));
    }

    @Test
    void crear_conDatosValidos_guardaLaInstalacionLimpiandoTextos() {
        when(comunaRepository.findById(10L)).thenReturn(Optional.of(comuna));
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(i -> i.getArgument(0));

        InstalacionResponse respuesta = servicio.crear(peticion(5L, "  Planta Lampa ", 10L));

        assertEquals("Planta Lampa", respuesta.nombre());
        assertEquals("Camino a Lampa", respuesta.ubicacion());
        assertEquals("Sin señal", respuesta.observacionesAcceso());
        assertEquals(5L, respuesta.clienteId());
    }

    @Test
    void crear_conTextosOpcionalesVacios_losGuardaComoNulos() {
        when(comunaRepository.findById(10L)).thenReturn(Optional.of(comuna));
        when(instalacionRepository.save(any(Instalacion.class))).thenAnswer(i -> i.getArgument(0));

        InstalacionResponse respuesta = servicio.crear(new InstalacionRequest(5L, "Planta", "  ", 10L, null));

        assertNull(respuesta.ubicacion());
        assertNull(respuesta.observacionesAcceso());
    }

    @Test
    void crear_sinCliente_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(null, "Planta", 10L)));
    }

    @Test
    void crear_sinNombre_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(5L, "  ", 10L)));
    }

    @Test
    void crear_conNombreNulo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(5L, null, 10L)));
    }

    @Test
    void crear_sinComuna_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(5L, "Planta", null)));
    }

    @Test
    void crear_conComunaInexistente_lanza400() {
        when(comunaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(peticion(5L, "Planta", 99L)));
    }

    @Test
    void actualizar_conDatosValidos_cambiaLosCamposPeroNoElCliente() {
        Instalacion existente = instalacion(1L, 5L);
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(comunaRepository.findById(10L)).thenReturn(Optional.of(comuna));
        when(instalacionRepository.save(existente)).thenReturn(existente);

        InstalacionResponse respuesta = servicio.actualizar(1L, peticion(9L, " Planta Nueva ", 10L));

        assertEquals("Planta Nueva", respuesta.nombre());
        assertEquals(5L, respuesta.clienteId());
    }

    @Test
    void actualizar_conIdInexistente_lanza404() {
        when(instalacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.actualizar(99L, peticion(5L, "Planta", 10L)));
    }

    @Test
    void actualizar_conNombreVacio_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(instalacion(1L, 5L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizar(1L, peticion(5L, "", 10L)));
    }

    @Test
    void actualizar_conComunaInexistente_lanza400() {
        when(instalacionRepository.findById(1L)).thenReturn(Optional.of(instalacion(1L, 5L)));
        when(comunaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizar(1L, peticion(5L, "Planta", 99L)));
    }
}
