package com.induztek.usuarios.controller;

import com.induztek.usuarios.dto.ActualizarPerfilRequest;
import com.induztek.usuarios.dto.CambiarContrasenaRequest;
import com.induztek.usuarios.dto.CambiarEstadoRequest;
import com.induztek.usuarios.dto.ConfirmarRecuperacionRequest;
import com.induztek.usuarios.dto.CrearUsuarioRequest;
import com.induztek.usuarios.dto.LoginRequest;
import com.induztek.usuarios.dto.RecuperacionRequest;
import com.induztek.usuarios.dto.RecuperacionResponse;
import com.induztek.usuarios.dto.UsuarioResponse;
import com.induztek.usuarios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) String buscar,
                                        @RequestParam(required = false) Long rolId) {
        return usuarioService.listar(buscar, rolId);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                 @RequestBody CrearUsuarioRequest datos) {
        return usuarioService.crear(actorId, datos);
    }

    @PostMapping("/login")
    public UsuarioResponse iniciarSesion(@RequestBody LoginRequest datos) {
        return usuarioService.iniciarSesion(datos);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarPerfil(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                            @PathVariable Long id,
                                            @RequestBody ActualizarPerfilRequest datos) {
        return usuarioService.actualizarPerfil(actorId, id, datos);
    }

    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@PathVariable Long id, @RequestBody CambiarContrasenaRequest datos) {
        usuarioService.cambiarContrasena(id, datos);
    }

    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                         @PathVariable Long id,
                                         @RequestBody CambiarEstadoRequest datos) {
        return usuarioService.cambiarEstado(actorId, id, datos.activo());
    }

    @PostMapping("/recuperacion")
    public RecuperacionResponse solicitarRecuperacion(@RequestBody RecuperacionRequest datos) {
        return usuarioService.solicitarRecuperacion(datos.correo());
    }

    @PostMapping("/recuperacion/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarRecuperacion(@RequestBody ConfirmarRecuperacionRequest datos) {
        usuarioService.confirmarRecuperacion(datos);
    }
}
