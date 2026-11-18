package com.induztek.protocolos.config;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.client.UsuariosCliente;
import com.induztek.protocolos.dto.CalibracionRequest;
import com.induztek.protocolos.dto.ComunaExterno;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.EquipoNuevo;
import com.induztek.protocolos.dto.InstalacionExterno;
import com.induztek.protocolos.dto.InstalacionNueva;
import com.induztek.protocolos.dto.InstrumentoRequest;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.dto.MedicionRequest;
import com.induztek.protocolos.dto.ProtocoloRequest;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.PruebaRequest;
import com.induztek.protocolos.dto.RecepcionRequest;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.dto.VisitaRequest;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.ResultadoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.repository.InstrumentoRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.service.CierreVisitaService;
import com.induztek.protocolos.service.InstrumentoService;
import com.induztek.protocolos.service.ProtocoloService;
import com.induztek.protocolos.service.PruebaService;
import com.induztek.protocolos.service.VisitaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(10)
public class DatosDemo implements CommandLineRunner {

    static final String CORREO_JEFE = "jefe@induztek.cl";
    static final String CORREO_INGENIERO = "camila.rojas@induztek.ejemplo";
    static final String CORREO_TECNICO = "diego.soto@induztek.ejemplo";
    static final String CORREO_ENVASADORA = "mfuentes@envpacifico.ejemplo";
    static final String CORREO_CLINICA = "tleiva@losaromos.ejemplo";
    static final String OT_LAMPA = "OT-DEMO-001";
    static final String OT_PROVIDENCIA = "OT-DEMO-002";

    private final UsuariosCliente usuariosCliente;
    private final EquiposCliente equiposCliente;
    private final InstrumentoService instrumentoService;
    private final InstrumentoRepository instrumentoRepository;
    private final TipoPruebaRepository tipoPruebaRepository;
    private final VisitaRepository visitaRepository;
    private final VisitaService visitaService;
    private final ProtocoloService protocoloService;
    private final PruebaService pruebaService;
    private final CierreVisitaService cierreVisitaService;
    private final boolean activo;
    private final int reintentos;
    private final long esperaMs;
    private final String jefeCorreo;

    public DatosDemo(UsuariosCliente usuariosCliente, EquiposCliente equiposCliente,
                     InstrumentoService instrumentoService, InstrumentoRepository instrumentoRepository,
                     TipoPruebaRepository tipoPruebaRepository, VisitaRepository visitaRepository,
                     VisitaService visitaService, ProtocoloService protocoloService, PruebaService pruebaService,
                     CierreVisitaService cierreVisitaService,
                     @Value("${demo.cargar:true}") boolean activo,
                     @Value("${demo.reintentos:20}") int reintentos,
                     @Value("${demo.espera-ms:3000}") long esperaMs,
                     @Value("${inicial.jefe.correo:" + CORREO_JEFE + "}") String jefeCorreo) {
        this.usuariosCliente = usuariosCliente;
        this.equiposCliente = equiposCliente;
        this.instrumentoService = instrumentoService;
        this.instrumentoRepository = instrumentoRepository;
        this.tipoPruebaRepository = tipoPruebaRepository;
        this.visitaRepository = visitaRepository;
        this.visitaService = visitaService;
        this.protocoloService = protocoloService;
        this.pruebaService = pruebaService;
        this.cierreVisitaService = cierreVisitaService;
        this.activo = activo;
        this.reintentos = reintentos;
        this.esperaMs = esperaMs;
        this.jefeCorreo = jefeCorreo;
    }

    @Override
    public void run(String... args) throws InterruptedException {
        if (!activo || visitaRepository.existsByNumeroOt(OT_LAMPA)) {
            return;
        }
        for (int intento = 1; intento <= reintentos; intento++) {
            try {
                if (cargar()) {
                    log.info("Datos de ejemplo cargados");
                    return;
                }
            } catch (RuntimeException e) {
                log.warn("Datos de ejemplo: {}", e.getMessage());
            }
            if (intento < reintentos) {
                Thread.sleep(esperaMs);
            }
        }
        log.warn("No se pudieron cargar los datos de ejemplo. Reinicia protocolos-service con los otros servicios arriba.");
    }

    private boolean cargar() {
        Map<String, UsuarioExterno> usuarios = usuariosCliente.listar().stream()
                .collect(Collectors.toMap(UsuarioExterno::correo, Function.identity(), (a, b) -> a));
        UsuarioExterno jefe = usuarios.get(jefeCorreo);
        UsuarioExterno ingeniero = usuarios.get(CORREO_INGENIERO);
        UsuarioExterno tecnico = usuarios.get(CORREO_TECNICO);
        UsuarioExterno envasadora = usuarios.get(CORREO_ENVASADORA);
        UsuarioExterno clinica = usuarios.get(CORREO_CLINICA);
        if (jefe == null || ingeniero == null || tecnico == null || envasadora == null || clinica == null) {
            log.info("Datos de ejemplo: esperando a que usuarios-service cree las cuentas de ejemplo");
            return false;
        }
        Map<String, Long> comunas = equiposCliente.comunas().stream()
                .collect(Collectors.toMap(ComunaExterno::nombre, ComunaExterno::id, (a, b) -> a));
        if (!comunas.containsKey("Lampa") || !comunas.containsKey("Providencia")) {
            return false;
        }

        long megohmetro = instrumento(jefe, "Megóhmetro", "Megger", "MIT1025", "MG-100245");
        long microohmetro = instrumento(jefe, "Microhmímetro", "Megger", "DLRO10", "DL-778812");
        long ttr = instrumento(jefe, "Medidor de relación de transformación", "Megger", "TTR330", "TT-330991");
        long probadorRele = instrumento(jefe, "Probador de relés", "Omicron", "CMC356", "OM-356120");

        InstalacionExterno lampa = instalacion(envasadora, "Planta Lampa", "Camino Lo Echevers 1250",
                comunas.get("Lampa"));
        EquipoExterno trLampa = equipo(envasadora, lampa, "TR-101", 1L, "Siemens", "GEAFOL 1000", "SI-TR-5521",
                2016, "1000", "13.2", "0.4", null, null);
        EquipoExterno intLampa = equipo(envasadora, lampa, "INT-201", 2L, "ABB", "VD4", "AB-INT-3307",
                2018, null, "13.2", null, "630", "150");

        InstalacionExterno providencia = instalacion(clinica, "Sede Providencia", "Av. Providencia 2100",
                comunas.get("Providencia"));
        EquipoExterno trProv = equipo(clinica, providencia, "TR-01", 1L, "Schneider", "Trihal 500", "SC-TR-8812",
                2019, "500", "12", "0.4", null, null);
        EquipoExterno releProv = equipo(clinica, providencia, "REL-01", 5L, "SEL", "751A", "SE-RL-1190",
                2020, null, null, null, null, null);

        actaLampa(ingeniero, tecnico, trLampa, intLampa, megohmetro, microohmetro, ttr);
        actaProvidencia(ingeniero, tecnico, trProv, releProv, megohmetro, ttr, microohmetro, probadorRele);
        return true;
    }

    private long instrumento(UsuarioExterno jefe, String nombre, String marca, String modelo, String serie) {
        Optional<InstrumentoResponse> existente = instrumentoService.listar(null).stream()
                .filter(i -> serie.equals(i.numeroSerie()))
                .findFirst();
        if (existente.isPresent()) {
            return existente.get().id();
        }
        InstrumentoResponse nuevo = instrumentoService.crear(jefe.id(), new InstrumentoRequest(nombre, marca, modelo, serie));
        instrumentoService.registrarCalibracion(jefe.id(), nuevo.id(),
                new CalibracionRequest(LocalDate.of(2026, 2, 1), LocalDate.of(2027, 2, 1)));
        return nuevo.id();
    }

    private InstalacionExterno instalacion(UsuarioExterno cliente, String nombre, String ubicacion, Long comunaId) {
        return equiposCliente.instalaciones(cliente.id()).stream()
                .filter(i -> nombre.equals(i.nombre()))
                .findFirst()
                .orElseGet(() -> equiposCliente.crearInstalacion(
                        new InstalacionNueva(cliente.id(), nombre, ubicacion, comunaId, "Presentarse en portería")));
    }

    private EquipoExterno equipo(UsuarioExterno cliente, InstalacionExterno instalacion, String tag, Long tipoId,
                                 String marca, String modelo, String serie, int anio, String kva, String kvPrimario,
                                 String kvSecundario, String corriente, String maxContactos) {
        return equiposCliente.equipos(cliente.id()).stream()
                .filter(e -> tag.equals(e.tag()) && instalacion.id().equals(e.instalacionId()))
                .findFirst()
                .orElseGet(() -> equiposCliente.crearEquipo(new EquipoNuevo(instalacion.id(), tag, tipoId, marca,
                        modelo, serie, anio, numero(kva), numero(kvPrimario), numero(kvSecundario),
                        numero(corriente), numero(maxContactos), null)));
    }

    private BigDecimal numero(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

    private void actaLampa(UsuarioExterno ingeniero, UsuarioExterno tecnico, EquipoExterno transformador,
                           EquipoExterno interruptor, long megohmetro, long microohmetro, long ttr) {
        VisitaResponse visita = visita(OT_LAMPA, transformador.instalacionId(), LocalDate.of(2026, 10, 14),
                TipoVisita.SOLO_PRUEBAS, ingeniero, tecnico);
        LocalDateTime hora = LocalDateTime.of(2026, 10, 14, 10, 0);

        ProtocoloResponse p1 = protocolo(tecnico, visita, transformador, hora, null);
        prueba(tecnico, p1, "AISLACION", "AT-BT", megohmetro, null,
                med("TENSION_PRUEBA", "5"), med("LECTURA_1MIN", "1500"), med("LECTURA_10MIN", "4200"));
        prueba(tecnico, p1, "TTR", "Fase A", ttr, null,
                med("POSICION_TAP", "3"), med("RELACION_TEORICA", "33"), med("RELACION_MEDIDA", "33.05"));
        prueba(tecnico, p1, "DEVANADOS", "AT", microohmetro, ResultadoPrueba.APROBADO, med("RESISTENCIA", "812.4"));
        registrarYFormalizar(tecnico, ingeniero, p1);

        ProtocoloResponse p2 = protocolo(tecnico, visita, interruptor, hora.plusHours(2), null);
        prueba(tecnico, p2, "AISLACION", "Polo R", megohmetro, null,
                med("TENSION_PRUEBA", "5"), med("LECTURA_1MIN", "2500"));
        prueba(tecnico, p2, "CONTACTOS", "Polo R", microohmetro, null, med("RESISTENCIA_CONTACTO", "120"));
        prueba(tecnico, p2, "CONTACTOS", "Polo S", microohmetro, null, med("RESISTENCIA_CONTACTO", "165"));
        registrarYFormalizar(tecnico, ingeniero, p2);

        cierreVisitaService.cerrar(ingeniero.id(), visita.id());
        visitaService.registrarRecepcion(visita.id(),
                new RecepcionRequest("Marcela Fuentes Rojas", "11111111-1", "Jefa de Mantenimiento"));
    }

    private void actaProvidencia(UsuarioExterno ingeniero, UsuarioExterno tecnico, EquipoExterno transformador,
                                 EquipoExterno rele, long megohmetro, long ttr, long microohmetro,
                                 long probadorRele) {
        VisitaResponse visita = visita(OT_PROVIDENCIA, transformador.instalacionId(), LocalDate.of(2026, 10, 20),
                TipoVisita.PRUEBAS_Y_MANT, ingeniero, tecnico);
        LocalDateTime hora = LocalDateTime.of(2026, 10, 20, 9, 30);

        ProtocoloResponse p1 = protocolo(tecnico, visita, transformador, hora,
                "Limpieza de bornes y reapriete de conexiones");
        prueba(tecnico, p1, "AISLACION", "AT-BT", megohmetro, null,
                MomentoPrueba.PREVIA, med("TENSION_PRUEBA", "5"), med("LECTURA_1MIN", "900"), med("LECTURA_10MIN", "2300"));
        prueba(tecnico, p1, "AISLACION", "AT-BT", megohmetro, null,
                MomentoPrueba.POSTERIOR, med("TENSION_PRUEBA", "5"), med("LECTURA_1MIN", "1800"), med("LECTURA_10MIN", "5100"));
        prueba(tecnico, p1, "TTR", "Fase A", ttr, null,
                med("POSICION_TAP", "2"), med("RELACION_TEORICA", "30"), med("RELACION_MEDIDA", "30.1"));
        prueba(tecnico, p1, "DEVANADOS", "AT", microohmetro, ResultadoPrueba.APROBADO, med("RESISTENCIA", "905.2"));
        registrarYFormalizar(tecnico, ingeniero, p1);

        ProtocoloResponse p2 = protocolo(tecnico, visita, rele, hora.plusHours(2),
                "Revisión de ajustes de la protección");
        prueba(tecnico, p2, "OPERACION_RELE", "Fase R", probadorRele, null, MomentoPrueba.POSTERIOR,
                med("CORRIENTE_AJUSTE", "5"), med("CORRIENTE_OPERACION", "5.1"),
                med("TIEMPO_ESPERADO", "100"), med("TIEMPO_MEDIDO", "103"));
        protocoloService.registrarEnTerreno(tecnico.id(), p2.id());

        cierreVisitaService.cerrar(ingeniero.id(), visita.id());
    }

    private VisitaResponse visita(String ot, Long instalacionId, LocalDate fecha, TipoVisita tipo,
                                  UsuarioExterno ingeniero, UsuarioExterno tecnico) {
        return visitaService.crear(ingeniero.id(),
                new VisitaRequest(ot, instalacionId, fecha, tipo, ingeniero.id(), List.of(tecnico.id())));
    }

    private ProtocoloResponse protocolo(UsuarioExterno tecnico, VisitaResponse visita, EquipoExterno equipo,
                                        LocalDateTime hora, String mantenimiento) {
        return protocoloService.crear(tecnico.id(), new ProtocoloRequest(visita.id(), equipo.id(), hora,
                new BigDecimal("22"), new BigDecimal("48"), mantenimiento, null, null));
    }

    private void prueba(UsuarioExterno tecnico, ProtocoloResponse protocolo, String codigo, String elemento,
                        long instrumentoId, ResultadoPrueba manual, MedicionRequest... mediciones) {
        prueba(tecnico, protocolo, codigo, elemento, instrumentoId, manual, MomentoPrueba.UNICA, mediciones);
    }

    private void prueba(UsuarioExterno tecnico, ProtocoloResponse protocolo, String codigo, String elemento,
                        long instrumentoId, ResultadoPrueba manual, MomentoPrueba momento,
                        MedicionRequest... mediciones) {
        TipoPrueba tipo = tipoPruebaRepository.findAll().stream()
                .filter(t -> codigo.equals(t.getCodigo()))
                .findFirst()
                .orElseThrow();
        pruebaService.registrar(tecnico.id(), protocolo.id(), new PruebaRequest(tipo.getId(), elemento, momento,
                true, null, instrumentoId, false, manual, null, new ArrayList<>(List.of(mediciones))));
    }

    private MedicionRequest med(String parametro, String valor) {
        return new MedicionRequest(parametro, new BigDecimal(valor));
    }

    private void registrarYFormalizar(UsuarioExterno tecnico, UsuarioExterno ingeniero, ProtocoloResponse protocolo) {
        protocoloService.registrarEnTerreno(tecnico.id(), protocolo.id());
        protocoloService.formalizar(ingeniero.id(), protocolo.id());
    }
}
