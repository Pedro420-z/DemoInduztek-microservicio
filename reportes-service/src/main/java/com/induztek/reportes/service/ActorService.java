package com.induztek.reportes.service;

import com.induztek.reportes.client.UsuariosCliente;
import com.induztek.reportes.dto.UsuarioExterno;
import com.induztek.reportes.util.Roles;
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta indicar quién consulta");
        }
        UsuarioExterno actor = usuariosCliente.buscar(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "El usuario que consulta no existe"));
        if (!actor.activo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }
        return actor;
    }

    public UsuarioExterno exigirPersonalActivo(Long actorId) {
        UsuarioExterno actor = exigirUsuarioActivo(actorId);
        if (Roles.CLIENTE.equals(actor.rolCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un cliente no puede consultar este reporte");
        }
        return actor;
    }
}
