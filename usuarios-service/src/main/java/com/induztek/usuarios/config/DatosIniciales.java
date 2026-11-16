package com.induztek.usuarios.config;

import com.induztek.usuarios.model.Rol;
import com.induztek.usuarios.model.Usuario;
import com.induztek.usuarios.repository.RolRepository;
import com.induztek.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    public static final String TECNICO = "TECNICO";
    public static final String INGENIERO = "INGENIERO";
    public static final String JEFE_TECNICO = "JEFE_TECNICO";
    public static final String CLIENTE = "CLIENTE";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${inicial.jefe.rut}")
    private String jefeRut;

    @Value("${inicial.jefe.nombre}")
    private String jefeNombre;

    @Value("${inicial.jefe.correo}")
    private String jefeCorreo;

    @Value("${inicial.jefe.contrasena}")
    private String jefeContrasena;

    @Override
    public void run(String... args) {
        if (rolRepository.count() == 0) {
            rolRepository.saveAll(List.of(
                    rol(TECNICO, "Técnico"),
                    rol(INGENIERO, "Ingeniero"),
                    rol(JEFE_TECNICO, "Jefe Técnico"),
                    rol(CLIENTE, "Cliente")
            ));
        }
        if (usuarioRepository.count() == 0) {
            Rol rolJefe = rolRepository.findAllByOrderByIdAsc().stream()
                    .filter(r -> JEFE_TECNICO.equals(r.getCodigo()))
                    .findFirst()
                    .orElseThrow();
            usuarioRepository.save(Usuario.builder()
                    .rol(rolJefe)
                    .rut(jefeRut)
                    .nombre(jefeNombre)
                    .correo(jefeCorreo)
                    .passwordHash(passwordEncoder.encode(jefeContrasena))
                    .activo(true)
                    .debeCambiarContrasena(true)
                    .build());
        }
    }

    private Rol rol(String codigo, String nombre) {
        return Rol.builder().codigo(codigo).nombre(nombre).build();
    }
}
