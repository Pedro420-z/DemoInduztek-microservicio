package com.induztek.protocolos.client;

import com.induztek.protocolos.dto.ComunaExterno;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.EquipoNuevo;
import com.induztek.protocolos.dto.InstalacionNueva;
import com.induztek.protocolos.dto.InstalacionExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.core.ParameterizedTypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class EquiposCliente {

    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(2);
    private static final Duration TIEMPO_RESPUESTA = Duration.ofSeconds(5);

    private final RestClient cliente;

    public EquiposCliente(@Value("${equipos.url}") String urlBase) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TIEMPO_CONEXION);
        fabrica.setReadTimeout(TIEMPO_RESPUESTA);
        this.cliente = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    public Optional<EquipoExterno> buscar(Long id) {
        try {
            return Optional.ofNullable(cliente.get()
                    .uri("/api/equipos/{id}", id)
                    .retrieve()
                    .body(EquipoExterno.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el servicio de equipos");
        }
    }

    public Optional<InstalacionExterno> buscarInstalacion(Long id) {
        try {
            return Optional.ofNullable(cliente.get()
                    .uri("/api/instalaciones/{id}", id)
                    .retrieve()
                    .body(InstalacionExterno.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el servicio de equipos");
        }
    }

    public List<ComunaExterno> comunas() {
        return consultarLista("/api/comunas", new ParameterizedTypeReference<List<ComunaExterno>>() { });
    }

    public List<InstalacionExterno> instalaciones(Long clienteId) {
        return consultarLista("/api/instalaciones?clienteId=" + clienteId,
                new ParameterizedTypeReference<List<InstalacionExterno>>() { });
    }

    public List<EquipoExterno> equipos(Long clienteId) {
        return consultarLista("/api/equipos?clienteId=" + clienteId,
                new ParameterizedTypeReference<List<EquipoExterno>>() { });
    }

    public InstalacionExterno crearInstalacion(InstalacionNueva datos) {
        return enviar("/api/instalaciones", datos, InstalacionExterno.class);
    }

    public EquipoExterno crearEquipo(EquipoNuevo datos) {
        return enviar("/api/equipos", datos, EquipoExterno.class);
    }

    private <T> List<T> consultarLista(String ruta, ParameterizedTypeReference<List<T>> tipo) {
        try {
            List<T> lista = cliente.get().uri(ruta).retrieve().body(tipo);
            return lista == null ? List.of() : lista;
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    private <T> T enviar(String ruta, Object cuerpo, Class<T> tipo) {
        try {
            return cliente.post().uri(ruta).body(cuerpo).retrieve().body(tipo);
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar el servicio de equipos");
    }
}
