package com.induztek.reportes.client;

import com.induztek.reportes.dto.UsuarioExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UsuariosCliente extends ClienteRemoto {

    public UsuariosCliente(@Value("${usuarios.url}") String urlBase) {
        super(urlBase, "usuarios");
    }

    public Optional<UsuarioExterno> buscar(Long id) {
        return consultar(uri -> uri.path("/api/usuarios/{id}").build(id), UsuarioExterno.class);
    }
}
