package com.induztek.equipos.repository;

import com.induztek.equipos.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findAllByOrderByTagAsc();

    List<Equipo> findByInstalacionClienteIdOrderByTagAsc(Long clienteId);

    boolean existsByInstalacionIdAndTag(Long instalacionId, String tag);

    boolean existsByInstalacionIdAndTagAndIdNot(Long instalacionId, String tag, Long id);
}
