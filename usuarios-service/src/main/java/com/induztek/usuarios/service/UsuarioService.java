package com.induztek.usuarios.service;

import com.induztek.usuarios.config.DatosIniciales;
import com.induztek.usuarios.dto.ActualizarPerfilRequest;
import com.induztek.usuarios.dto.CambiarContrasenaRequest;
import com.induztek.usuarios.dto.ConfirmarRecuperacionRequest;
import com.induztek.usuarios.dto.CrearUsuarioRequest;
import com.induztek.usuarios.dto.LoginRequest;
import com.induztek.usuarios.dto.RecuperacionResponse;
import com.induztek.usuarios.dto.UsuarioResponse;
import com.induztek.usuarios.model.Rol;
import com.induztek.usuarios.model.Usuario;
import com.induztek.usuarios.repository.RolRepository;
import com.induztek.usuarios.repository.UsuarioRepository;
import com.induztek.usuarios.util.GeneradorCodigo;
import com.induztek.usuarios.util.RutValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int LARGO_MINIMO_CONTRASENA = 6;
    private static final Duration VIGENCIA_CODIGO = Duration.ofMinutes(15);
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorCodigo generadorCodigo;
    private final Clock reloj;

    public List<UsuarioResponse> listar(String buscar, Long rolId) {
        String texto = buscar == null ? "" : buscar.trim();
        List<Usuario> base = texto.isEmpty()
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(texto, texto);
        return base.stream()
                .filter(usuario -> rolId == null || usuario.getRol().getId().equals(rolId))
                .map(this::aRespuesta)
                .toList();
    }

    public UsuarioResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public UsuarioResponse crear(Long actorId, CrearUsuarioRequest datos) {
        Usuario actor = exigirActorActivo(actorId);
        boolean actorEsJefe = DatosIniciales.JEFE_TECNICO.equals(actor.getRol().getCodigo());

        if (datos.rolId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar el rol");
        }
        Rol rol = rolRepository.findById(datos.rolId())
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "El rol indicado no existe"));
        boolean creaCliente = DatosIniciales.CLIENTE.equals(rol.getCodigo());

        if (!creaCliente && !actorEsJefe) {
            throw error(HttpStatus.FORBIDDEN, "Solo el Jefe técnico puede crear personal");
        }

        validarNombre(datos.nombre());
        validarCorreo(datos.correo());
        validarContrasena(datos.contrasena());
        if (!RutValidator.esValido(datos.rut())) {
            throw error(HttpStatus.BAD_REQUEST, "RUT inválido (revisa el dígito verificador)");
        }

        String rut = RutValidator.normalizar(datos.rut());
        String correo = datos.correo().trim().toLowerCase();
        if (usuarioRepository.existsByRut(rut)) {
            throw error(HttpStatus.CONFLICT, "Ese RUT ya está registrado");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw error(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }

        Usuario nuevo = Usuario.builder()
                .rol(rol)
                .rut(rut)
                .nombre(datos.nombre().trim())
                .correo(correo)
                .telefono(limpiar(datos.telefono()))
                .passwordHash(passwordEncoder.encode(datos.contrasena()))
                .activo(actorEsJefe)
                .giro(creaCliente ? limpiar(datos.giro()) : null)
                .contactoNombre(creaCliente ? limpiar(datos.contactoNombre()) : null)
                .contactoCargo(creaCliente ? limpiar(datos.contactoCargo()) : null)
                .build();
        return aRespuesta(usuarioRepository.save(nuevo));
    }

    public UsuarioResponse iniciarSesion(LoginRequest datos) {
        String correo = datos.correo() == null ? "" : datos.correo().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .filter(u -> datos.contrasena() != null && passwordEncoder.matches(datos.contrasena(), u.getPasswordHash()))
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        if (!usuario.isActivo()) {
            throw error(HttpStatus.FORBIDDEN, "La cuenta está inactiva. Contacta al Jefe técnico");
        }
        return aRespuesta(usuario);
    }

    public UsuarioResponse actualizarPerfil(Long id, ActualizarPerfilRequest datos) {
        Usuario usuario = buscar(id);
        validarNombre(datos.nombre());
        validarCorreo(datos.correo());

        String correo = datos.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw error(HttpStatus.CONFLICT, "Ese correo ya está registrado");
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(limpiar(datos.telefono()));
        if (DatosIniciales.CLIENTE.equals(usuario.getRol().getCodigo())) {
            usuario.setGiro(limpiar(datos.giro()));
            usuario.setContactoNombre(limpiar(datos.contactoNombre()));
            usuario.setContactoCargo(limpiar(datos.contactoCargo()));
        }
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public void cambiarContrasena(Long id, CambiarContrasenaRequest datos) {
        Usuario usuario = buscar(id);
        if (datos.actual() == null || !passwordEncoder.matches(datos.actual(), usuario.getPasswordHash())) {
            throw error(HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta");
        }
        validarContrasena(datos.nueva());
        if (datos.nueva().equals(datos.actual())) {
            throw error(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPasswordHash(passwordEncoder.encode(datos.nueva()));
        usuarioRepository.save(usuario);
    }

    public UsuarioResponse cambiarEstado(Long actorId, Long id, Boolean activo) {
        Usuario actor = exigirActorActivo(actorId);
        if (!DatosIniciales.JEFE_TECNICO.equals(actor.getRol().getCodigo())) {
            throw error(HttpStatus.FORBIDDEN, "Solo el Jefe técnico puede activar o desactivar cuentas");
        }
        if (activo == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debes indicar si la cuenta queda activa");
        }
        if (actor.getId().equals(id) && !activo) {
            throw error(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta");
        }
        Usuario usuario = buscar(id);
        usuario.setActivo(activo);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public RecuperacionResponse solicitarRecuperacion(String correo) {
        Usuario usuario = buscarPorCorreo(correo);
        if (!usuario.isActivo()) {
            throw error(HttpStatus.FORBIDDEN, "La cuenta está inactiva. Contacta al Jefe técnico");
        }
        String codigo = generadorCodigo.generar();
        LocalDateTime expira = LocalDateTime.now(reloj).plus(VIGENCIA_CODIGO);
        usuario.setCodigoRecuperacion(codigo);
        usuario.setRecuperacionExpira(expira);
        usuarioRepository.save(usuario);
        return new RecuperacionResponse(codigo, expira);
    }

    public void confirmarRecuperacion(ConfirmarRecuperacionRequest datos) {
        Usuario usuario = buscarPorCorreo(datos.correo());
        if (usuario.getCodigoRecuperacion() == null || !usuario.getCodigoRecuperacion().equals(datos.codigo())) {
            throw error(HttpStatus.BAD_REQUEST, "El código de recuperación no es válido");
        }
        if (usuario.getRecuperacionExpira().isBefore(LocalDateTime.now(reloj))) {
            throw error(HttpStatus.BAD_REQUEST, "El código de recuperación expiró");
        }
        validarContrasena(datos.nueva());
        usuario.setPasswordHash(passwordEncoder.encode(datos.nueva()));
        usuario.setCodigoRecuperacion(null);
        usuario.setRecuperacionExpira(null);
        usuarioRepository.save(usuario);
    }

    private Usuario exigirActorActivo(Long actorId) {
        if (actorId == null) {
            throw error(HttpStatus.UNAUTHORIZED, "Falta indicar quién realiza la acción");
        }
        Usuario actor = usuarioRepository.findById(actorId)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "El usuario que realiza la acción no existe"));
        if (!actor.isActivo()) {
            throw error(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }
        if (DatosIniciales.CLIENTE.equals(actor.getRol().getCodigo())) {
            throw error(HttpStatus.FORBIDDEN, "Un cliente no puede realizar esta acción");
        }
        return actor;
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Usuario buscarPorCorreo(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase();
        return usuarioRepository.findByCorreo(limpio)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No existe una cuenta con ese correo"));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
    }

    private void validarCorreo(String correo) {
        if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
            throw error(HttpStatus.BAD_REQUEST, "Ingresa un correo válido");
        }
    }

    private void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            throw error(HttpStatus.BAD_REQUEST, "La contraseña debe tener mínimo 6 caracteres");
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        Rol rol = usuario.getRol();
        return new UsuarioResponse(
                usuario.getId(),
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre(),
                usuario.getRut(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.isActivo(),
                usuario.getGiro(),
                usuario.getContactoNombre(),
                usuario.getContactoCargo()
        );
    }
}
