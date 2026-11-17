package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.Prueba;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.induztek.protocolos.model.MomentoPrueba;

public interface PruebaRepository extends JpaRepository<Prueba, Long> {

    List<Prueba> findByProtocoloIdOrderByIdAsc(Long protocoloId);

    boolean existsByProtocoloIdAndTipoPruebaIdAndElementoAndMomento(
            Long protocoloId, Long tipoPruebaId, String elemento, MomentoPrueba momento);
}
