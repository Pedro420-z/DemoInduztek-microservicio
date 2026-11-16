package com.induztek.protocolos.client;

import com.induztek.protocolos.dto.UsuarioExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class UsuariosCliente {

    private final RestClient cliente;

    public UsuariosCliente(@Value("${usuarios.url}") String urlBase) {
        this.cliente = RestClient.create(urlBase);
    }

    public Optional<UsuarioExterno> buscar(Long id) {
        try {
            return Optional.ofNullable(cliente.get()
                    .uri("/api/usuarios/{id}", id)
                    .retrieve()
                    .body(UsuarioExterno.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el servicio de usuarios");
        }
    }
}
