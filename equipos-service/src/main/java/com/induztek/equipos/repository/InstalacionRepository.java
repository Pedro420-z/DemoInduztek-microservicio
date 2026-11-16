package com.induztek.equipos.repository;

import com.induztek.equipos.model.Instalacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {

    List<Instalacion> findAllByOrderByNombreAsc();

    List<Instalacion> findByClienteIdOrderByNombreAsc(Long clienteId);
}
