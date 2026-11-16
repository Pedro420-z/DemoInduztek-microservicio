package com.induztek.usuarios.repository;

import com.induztek.usuarios.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolRepository extends JpaRepository<Rol, Long> {

    List<Rol> findAllByOrderByIdAsc();
}
