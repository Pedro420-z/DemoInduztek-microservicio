package com.induztek.usuarios.controller;

import com.induztek.usuarios.dto.CrearUsuarioRequest;
import com.induztek.usuarios.dto.RecuperacionResponse;
import com.induztek.usuarios.dto.UsuarioResponse;
import com.induztek.usuarios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(usuarioService)).build();
    }

    private UsuarioResponse respuesta(Long id, boolean activo) {
        return new UsuarioResponse(id, 1L, "TECNICO", "Técnico", "14226791-8", "Matías Godoy",
                "mgodoy@induztek.cl", null, activo, null, null, null, false);
    }

    @Test
    void listar_pasaLosFiltrosAlServicio() throws Exception {
        when(usuarioService.listar("mat", 1L)).thenReturn(List.of(respuesta(1L, true)));

        mvc.perform(get("/api/usuarios").param("buscar", "mat").param("rolId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Matías Godoy")));
    }

    @Test
    void obtener_devuelveElUsuario() throws Exception {
        when(usuarioService.obtener(1L)).thenReturn(respuesta(1L, true));

        mvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mgodoy@induztek.cl")));
    }

    @Test
    void obtener_siNoExiste_respondeConElCodigoDelServicio() throws Exception {
        when(usuarioService.obtener(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        mvc.perform(get("/api/usuarios/9")).andExpect(status().isNotFound());
    }

    @Test
    void iniciarSesion_conDatosCorrectos_responde200() throws Exception {
        when(usuarioService.iniciarSesion(any())).thenReturn(respuesta(1L, true));

        mvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"mgodoy@induztek.cl\",\"contrasena\":\"secreto1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"id\":1")));
    }

    @Test
    void iniciarSesion_conClaveIncorrecta_responde401() throws Exception {
        when(usuarioService.iniciarSesion(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));

        mvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"mgodoy@induztek.cl\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crear_leeElActorDeLaCabeceraYResponde201() throws Exception {
        when(usuarioService.crear(eq(7L), any(CrearUsuarioRequest.class))).thenReturn(respuesta(2L, true));

        mvc.perform(post("/api/usuarios")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"14.226.791-8\",\"nombre\":\"Ana\",\"correo\":\"ana@induztek.cl\","
                                + "\"rolId\":1}"))
                .andExpect(status().isCreated());

        verify(usuarioService).crear(eq(7L), any(CrearUsuarioRequest.class));
    }

    @Test
    void actualizarPerfil_responde200() throws Exception {
        when(usuarioService.actualizarPerfil(eq(1L), eq(1L), any())).thenReturn(respuesta(1L, true));

        mvc.perform(put("/api/usuarios/1")
                        .header("X-Usuario-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Matías\",\"correo\":\"mgodoy@induztek.cl\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarContrasena_responde204() throws Exception {
        mvc.perform(put("/api/usuarios/1/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"secreto1\",\"nueva\":\"otraClave2\"}"))
                .andExpect(status().isNoContent());

        verify(usuarioService).cambiarContrasena(eq(1L), any());
    }

    @Test
    void cambiarEstado_pasaElActorYElNuevoEstado() throws Exception {
        when(usuarioService.cambiarEstado(7L, 2L, false)).thenReturn(respuesta(2L, false));

        mvc.perform(put("/api/usuarios/2/estado")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"activo\":false")));
    }

    @Test
    void solicitarRecuperacion_devuelveElMensajeSinElCodigo() throws Exception {
        when(usuarioService.solicitarRecuperacion("mgodoy@induztek.cl"))
                .thenReturn(new RecuperacionResponse("Enviamos un código a tu correo", LocalDateTime.of(2026, 11, 15, 12, 15)));

        mvc.perform(post("/api/usuarios/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"mgodoy@induztek.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enviamos un código")));
    }

    @Test
    void confirmarRecuperacion_responde204() throws Exception {
        mvc.perform(post("/api/usuarios/recuperacion/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"mgodoy@induztek.cl\",\"codigo\":\"123456\",\"nueva\":\"claveNueva1\"}"))
                .andExpect(status().isNoContent());

        verify(usuarioService).confirmarRecuperacion(any());
    }
}
