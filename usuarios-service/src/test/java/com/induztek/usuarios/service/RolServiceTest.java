package com.induztek.usuarios.service;

import com.induztek.usuarios.dto.RolResponse;
import com.induztek.usuarios.model.Rol;
import com.induztek.usuarios.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    @Test
    void listar_convierteLosRolesAResponse() {
        when(rolRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                Rol.builder().id(1L).codigo("TECNICO").nombre("Técnico").build(),
                Rol.builder().id(4L).codigo("CLIENTE").nombre("Cliente").build()));

        List<RolResponse> roles = rolService.listar();

        assertEquals(2, roles.size());
        assertEquals("TECNICO", roles.get(0).codigo());
        assertEquals("Cliente", roles.get(1).nombre());
    }
}
