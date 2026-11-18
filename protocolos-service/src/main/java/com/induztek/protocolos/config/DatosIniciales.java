package com.induztek.protocolos.config;

import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    public static final long TRANSFORMADOR = 1L;
    public static final long INTERRUPTOR = 2L;
    public static final long CELDA_MT = 3L;
    public static final long CABLE = 4L;
    public static final long RELE = 5L;

    private final TipoPruebaRepository tipoPruebaRepository;
    private final ParametroRepository parametroRepository;
    private final TipoEquipoPruebaRepository tipoEquipoPruebaRepository;

    @Override
    public void run(String... args) {
        if (tipoPruebaRepository.count() > 0) {
            return;
        }
        List<Parametro> parametros = new ArrayList<>();
        List<TipoEquipoPrueba> aplicaciones = new ArrayList<>();

        TipoPrueba aislacion = guardar("AISLACION", "Aislación",
                List.of(TRANSFORMADOR, INTERRUPTOR, CELDA_MT, CABLE), aplicaciones);
        parametros.add(parametro(aislacion, "TENSION_PRUEBA", "Tensión de prueba", "kV"));
        parametros.add(parametro(aislacion, "LECTURA_1MIN", "Lectura a 1 minuto", "MΩ"));
        parametros.add(parametro(aislacion, "LECTURA_10MIN", "Lectura a 10 minutos", "MΩ"));

        TipoPrueba ttr = guardar("TTR", "Relación de transformación", List.of(TRANSFORMADOR), aplicaciones);
        parametros.add(parametro(ttr, "POSICION_TAP", "Posición del tap", "-"));
        parametros.add(parametro(ttr, "RELACION_TEORICA", "Relación teórica", "-"));
        parametros.add(parametro(ttr, "RELACION_MEDIDA", "Relación medida", "-"));

        TipoPrueba devanados = guardar("DEVANADOS", "Resistencia de devanados", List.of(TRANSFORMADOR), aplicaciones);
        parametros.add(parametro(devanados, "RESISTENCIA", "Resistencia", "mΩ"));

        TipoPrueba contactos = guardar("CONTACTOS", "Resistencia de contactos",
                List.of(INTERRUPTOR, CELDA_MT), aplicaciones);
        parametros.add(parametro(contactos, "RESISTENCIA_CONTACTO", "Resistencia de contacto", "µΩ"));

        TipoPrueba rele = guardar("OPERACION_RELE", "Operación de relé", List.of(RELE), aplicaciones);
        parametros.add(parametro(rele, "CORRIENTE_AJUSTE", "Corriente de ajuste", "A"));
        parametros.add(parametro(rele, "CORRIENTE_OPERACION", "Corriente de operación", "A"));
        parametros.add(parametro(rele, "TIEMPO_ESPERADO", "Tiempo esperado", "ms"));
        parametros.add(parametro(rele, "TIEMPO_MEDIDO", "Tiempo medido", "ms"));

        parametroRepository.saveAll(parametros);
        tipoEquipoPruebaRepository.saveAll(aplicaciones);
    }

    private TipoPrueba guardar(String codigo, String nombre, List<Long> tiposEquipo, List<TipoEquipoPrueba> aplicaciones) {
        TipoPrueba tipo = tipoPruebaRepository.save(TipoPrueba.builder().codigo(codigo).nombre(nombre).build());
        for (Long tipoEquipoId : tiposEquipo) {
            aplicaciones.add(TipoEquipoPrueba.builder().tipoEquipoId(tipoEquipoId).tipoPrueba(tipo).build());
        }
        return tipo;
    }

    private Parametro parametro(TipoPrueba tipo, String codigo, String nombre, String unidad) {
        return Parametro.builder().tipoPrueba(tipo).codigo(codigo).nombre(nombre).unidad(unidad).build();
    }
}
