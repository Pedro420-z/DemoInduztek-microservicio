package com.induztek.equipos.repository;

import com.induztek.equipos.model.TipoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoEquipoRepository extends JpaRepository<TipoEquipo, Long> {

    List<TipoEquipo> findAllByOrderByNombreAsc();
}
