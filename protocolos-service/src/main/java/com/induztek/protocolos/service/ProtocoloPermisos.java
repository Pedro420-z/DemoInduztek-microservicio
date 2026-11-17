package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.util.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProtocoloPermisos {

    public void exigirEditable(UsuarioExterno actor, Protocolo protocolo) {
        if (protocolo.getEstado() != EstadoProtocolo.BORRADOR && protocolo.getEstado() != EstadoProtocolo.OBSERVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El protocolo está " + protocolo.getEstado() + " y ya no se puede modificar");
        }
        if (Roles.TECNICO.equals(actor.rolCodigo()) && !protocolo.getUsuarioRegistraId().equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un técnico solo puede modificar los protocolos que él creó");
        }
    }
}
