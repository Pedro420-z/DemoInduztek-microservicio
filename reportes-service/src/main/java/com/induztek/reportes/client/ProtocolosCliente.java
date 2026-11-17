package com.induztek.reportes.client;

import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.VisitaExterno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProtocolosCliente extends ClienteRemoto {

    public ProtocolosCliente(@Value("${protocolos.url}") String urlBase) {
        super(urlBase, "protocolos");
    }

    public List<ProtocoloExterno> protocolos(Long equipoId, Long visitaId, String estado, Boolean atrasados) {
        return consultarLista(uri -> uri.path("/api/protocolos")
                .queryParamIfPresent("equipoId", Optional.ofNullable(equipoId))
                .queryParamIfPresent("visitaId", Optional.ofNullable(visitaId))
                .queryParamIfPresent("estado", Optional.ofNullable(estado))
                .queryParamIfPresent("atrasados", Optional.ofNullable(atrasados)).build(),
                new ParameterizedTypeReference<List<ProtocoloExterno>>() {
                });
    }

    public List<VisitaExterno> visitas(Long instalacionId, Boolean cerrada) {
        return consultarLista(uri -> uri.path("/api/visitas")
                .queryParamIfPresent("instalacionId", Optional.ofNullable(instalacionId))
                .queryParamIfPresent("cerrada", Optional.ofNullable(cerrada)).build(),
                new ParameterizedTypeReference<List<VisitaExterno>>() {
                });
    }
}
