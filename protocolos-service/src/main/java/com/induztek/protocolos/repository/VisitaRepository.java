package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.Visita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    List<Visita> findAllByOrderByFechaDescIdDesc();

    List<Visita> findByNumeroOtContainingIgnoreCaseOrderByFechaDescIdDesc(String numeroOt);

    boolean existsByNumeroOt(String numeroOt);
}
