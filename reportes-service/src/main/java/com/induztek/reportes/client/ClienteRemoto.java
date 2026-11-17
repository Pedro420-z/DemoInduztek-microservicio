package com.induztek.reportes.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class ClienteRemoto {

    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(2);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(5);

    private final RestClient cliente;
    private final String servicio;

    protected ClienteRemoto(String urlBase, String servicio) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIEMPO_CONEXION);
        fabrica.setReadTimeout(TIEMPO_RESPUESTA);
        this.cliente = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
        this.servicio = servicio;
    }

    protected <T> Optional<T> consultar(Function<UriBuilder, URI> uri, Class<T> tipo) {
        try {
            return Optional.ofNullable(cliente.get().uri(uri).retrieve().body(tipo));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    protected <T> List<T> consultarLista(Function<UriBuilder, URI> uri, ParameterizedTypeReference<List<T>> tipo) {
        try {
            List<T> lista = cliente.get().uri(uri).retrieve().body(tipo);
            return lista == null ? List.of() : lista;
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar el servicio de " + servicio);
    }
}
