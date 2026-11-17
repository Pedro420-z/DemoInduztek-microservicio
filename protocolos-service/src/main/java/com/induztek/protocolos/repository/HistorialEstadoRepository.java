package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findByProtocoloIdOrderByIdAsc(Long protocoloId);
}
