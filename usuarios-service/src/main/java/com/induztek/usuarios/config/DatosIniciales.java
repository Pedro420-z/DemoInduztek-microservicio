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

    @Value("${inicial.clientes.contrasena}")
    private String clientesContrasena;

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
        cargarPersonalDeEjemplo();
        cargarEmpresasDeEjemplo();
    }

    private void cargarPersonalDeEjemplo() {
        List<Rol> roles = rolRepository.findAllByOrderByIdAsc();
        crearPersonal(roles, INGENIERO, "15234567-4", "Camila Rojas Vera", "camila.rojas@induztek.ejemplo");
        crearPersonal(roles, TECNICO, "16345678-8", "Diego Soto Pino", "diego.soto@induztek.ejemplo");
    }

    private void crearPersonal(List<Rol> roles, String codigoRol, String rut, String nombre, String correo) {
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }
        Rol rol = roles.stream().filter(r -> codigoRol.equals(r.getCodigo())).findFirst().orElseThrow();
        usuarioRepository.save(Usuario.builder()
                .rol(rol)
                .rut(rut)
                .nombre(nombre)
                .correo(correo)
                .passwordHash(passwordEncoder.encode(clientesContrasena))
                .activo(true)
                .debeCambiarContrasena(true)
                .build());
    }

    private void cargarEmpresasDeEjemplo() {
        Rol rolCliente = rolRepository.findAllByOrderByIdAsc().stream()
                .filter(r -> CLIENTE.equals(r.getCodigo()))
                .findFirst()
                .orElseThrow();
        crearEmpresa(rolCliente, "76482915-8", "Envasadora del Pacífico S.A.", "mfuentes@envpacifico.ejemplo",
                "+56 9 0000 0101", "Agroindustria", "Marcela Fuentes Rojas", "Jefa de Mantenimiento");
        crearEmpresa(rolCliente, "76903218-5", "Clínica Los Aromos Ltda.", "tleiva@losaromos.ejemplo",
                "+56 9 0000 0104", "Salud", "Tomás Leiva Araya", "Supervisor Eléctrico");
    }

    private void crearEmpresa(Rol rolCliente, String rut, String nombre, String correo, String telefono,
                              String giro, String contactoNombre, String contactoCargo) {
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .rol(rolCliente)
                .rut(rut)
                .nombre(nombre)
                .correo(correo)
                .telefono(telefono)
                .passwordHash(passwordEncoder.encode(clientesContrasena))
                .activo(true)
                .debeCambiarContrasena(true)
                .giro(giro)
                .contactoNombre(contactoNombre)
                .contactoCargo(contactoCargo)
                .build());
    }

    private Rol rol(String codigo, String nombre) {
        return Rol.builder().codigo(codigo).nombre(nombre).build();
    }
}
