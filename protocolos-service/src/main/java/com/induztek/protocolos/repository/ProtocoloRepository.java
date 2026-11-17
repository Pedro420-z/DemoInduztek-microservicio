package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.Protocolo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {

    List<Protocolo> findAllByOrderByIdDesc();

    List<Protocolo> findByNumeroContainingIgnoreCaseOrderByIdDesc(String numero);

    List<Protocolo> findByVisitaIdOrderByIdAsc(Long visitaId);

    boolean existsByVisitaIdAndEquipoId(Long visitaId, Long equipoId);

    long countByNumeroStartingWith(String prefijo);
}
