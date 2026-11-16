package com.induztek.equipos.repository;

import com.induztek.equipos.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByNombreAsc();
}
