package com.induztek.usuarios.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.induztek.usuarios.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    List<Rol> findAllByOrderByidAsc();
}
