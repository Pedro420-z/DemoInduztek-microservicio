package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.Calibracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.time.LocalDate;
import java.util.Optional;

public interface CalibracionRepository extends JpaRepository<Calibracion, Long> {

    List<Calibracion> findByInstrumentoIdOrderByFechaVencimientoDesc(Long instrumentoId);

    Optional<Calibracion> findFirstByInstrumentoIdOrderByFechaVencimientoDesc(Long instrumentoId);

    boolean existsByInstrumentoIdAndFechaCalibracionLessThanEqualAndFechaVencimientoGreaterThanEqual(
            Long instrumentoId, LocalDate calibradoHasta, LocalDate vigenteDesde);
}
