package com.induztek.protocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "protocolo", uniqueConstraints = @UniqueConstraint(columnNames = {"id_visita", "id_equipo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Protocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_visita", nullable = false)
    private Visita visita;

    @Column(name = "id_equipo", nullable = false)
    private Long equipoId;

    @Column(name = "id_usuario_registra", nullable = false)
    private Long usuarioRegistraId;

    @Column(name = "fecha_hora_prueba", nullable = false)
    private LocalDateTime fechaHoraPrueba;

    @Column(name = "temperatura_c", precision = 5, scale = 2)
    private BigDecimal temperaturaC;

    @Column(name = "humedad_pct", precision = 5, scale = 2)
    private BigDecimal humedadPct;

    @Column(name = "descripcion_mantenimiento", columnDefinition = "TEXT")
    private String descripcionMantenimiento;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProtocolo estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_protocolo_reemplazado")
    private Protocolo protocoloReemplazado;
}
