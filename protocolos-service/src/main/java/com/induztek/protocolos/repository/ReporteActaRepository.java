package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.ReporteActa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteActaRepository extends JpaRepository<ReporteActa, Long> {

    List<ReporteActa> findAllByOrderByIdDesc();

    List<ReporteActa> findByClienteIdOrderByIdDesc(Long clienteId);

    List<ReporteActa> findByVisitaIdOrderByIdDesc(Long visitaId);
}
