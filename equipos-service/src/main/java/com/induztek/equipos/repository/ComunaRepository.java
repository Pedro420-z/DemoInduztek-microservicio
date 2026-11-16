package com.induztek.equipos.repository;

import com.induztek.equipos.model.Comuna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    List<Comuna> findAllByOrderByNombreAsc();

    List<Comuna> findByRegionIdOrderByNombreAsc(Long regionId);
}
