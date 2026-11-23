package com.induztek.equipos.client;

import com.induztek.equipos.dto.UsuarioExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;

@Component
public class UsuariosCliente {

    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(2);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(5);

    private final RestClient cliente;

    public UsuariosCliente(@Value("${usuarios.url}") String urlBase) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIEMPO_CONEXION);
        fabrica.setReadTimeout(TIEMPO_RESPUESTA);
        this.cliente = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
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
