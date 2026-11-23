package com.induztek.equipos.service;

import com.induztek.equipos.client.UsuariosCliente;
import com.induztek.equipos.dto.UsuarioExterno;
import com.induztek.equipos.util.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ActorService {

    private final UsuariosCliente usuariosCliente;

    public UsuarioExterno exigirUsuarioActivo(Long actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta indicar quién realiza la acción");
        }
        UsuarioExterno actor = usuariosCliente.buscar(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "El usuario que realiza la acción no existe"));
        if (!actor.activo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }
        return actor;
    }

    public UsuarioExterno exigirJefeTecnico(Long actorId) {
        UsuarioExterno actor = exigirUsuarioActivo(actorId);
        if (!Roles.JEFE_TECNICO.equals(actor.rolCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Jefe técnico puede realizar esta acción");
        }
        return actor;
    }
}
