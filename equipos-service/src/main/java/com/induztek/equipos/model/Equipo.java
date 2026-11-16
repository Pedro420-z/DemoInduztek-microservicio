package com.induztek.equipos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "equipo", uniqueConstraints = @UniqueConstraint(columnNames = {"id_instalacion", "tag"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_instalacion", nullable = false)
    private Instalacion instalacion;

    @Column(nullable = false, length = 30)
    private String tag;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_equipo", nullable = false)
    private TipoEquipo tipoEquipo;

    @Column(length = 60)
    private String marca;

    @Column(length = 60)
    private String modelo;

    @Column(name = "numero_serie", length = 60)
    private String numeroSerie;

    @Column(name = "anio_fabricacion")
    private Integer anioFabricacion;

    @Column(name = "potencia_kva", precision = 12, scale = 3)
    private BigDecimal potenciaKva;

    @Column(name = "tension_primaria_kv", precision = 12, scale = 3)
    private BigDecimal tensionPrimariaKv;

    @Column(name = "tension_secundaria_kv", precision = 12, scale = 3)
    private BigDecimal tensionSecundariaKv;

    @Column(name = "corriente_nominal_a", precision = 12, scale = 3)
    private BigDecimal corrienteNominalA;

    @Column(name = "max_contactos_uohm", precision = 12, scale = 3)
    private BigDecimal maxContactosUohm;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
