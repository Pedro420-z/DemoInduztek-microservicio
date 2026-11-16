package com.induztek.usuarios.controller;

import com.induztek.usuarios.dto.RolResponse;
import com.induztek.usuarios.service.RolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock
    private RolService rolService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new RolController(rolService)).build();
    }

    @Test
    void listar_respondeConLosRoles() throws Exception {
        when(rolService.listar()).thenReturn(List.of(new RolResponse(3L, "JEFE_TECNICO", "Jefe Técnico")));

        mvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JEFE_TECNICO")));
    }
}
