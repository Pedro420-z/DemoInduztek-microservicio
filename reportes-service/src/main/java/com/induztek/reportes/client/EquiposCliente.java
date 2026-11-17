package com.induztek.reportes.client;

import com.induztek.reportes.dto.EquipoExterno;
import com.induztek.reportes.dto.InstalacionExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EquiposCliente extends ClienteRemoto {

    public EquiposCliente(@Value("${equipos.url}") String urlBase) {
        super(urlBase, "equipos");
    }

    public Optional<EquipoExterno> obtener(Long id, Long clienteId) {
        return consultar(uri -> uri.path("/api/equipos/{id}")
                .queryParamIfPresent("clienteId", Optional.ofNullable(clienteId)).build(id), EquipoExterno.class);
    }

    public List<EquipoExterno> listar(Long clienteId) {
        return consultarLista(uri -> uri.path("/api/equipos")
                .queryParamIfPresent("clienteId", Optional.ofNullable(clienteId)).build(),
                new ParameterizedTypeReference<List<EquipoExterno>>() {
                });
    }

    public List<InstalacionExterno> instalaciones(Long clienteId) {
        return consultarLista(uri -> uri.path("/api/instalaciones")
                .queryParamIfPresent("clienteId", Optional.ofNullable(clienteId)).build(),
                new ParameterizedTypeReference<List<InstalacionExterno>>() {
                });
    }
}
