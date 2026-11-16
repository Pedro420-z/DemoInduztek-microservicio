package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.Instrumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentoRepository extends JpaRepository<Instrumento, Long> {

    List<Instrumento> findAllByOrderByNombreAsc();

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, Long id);
}
