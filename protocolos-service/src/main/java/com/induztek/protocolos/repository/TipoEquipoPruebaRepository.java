package com.induztek.protocolos.repository;

import com.induztek.protocolos.model.TipoEquipoPrueba;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoEquipoPruebaRepository extends JpaRepository<TipoEquipoPrueba, Long> {

    List<TipoEquipoPrueba> findByTipoEquipoId(Long tipoEquipoId);
}
