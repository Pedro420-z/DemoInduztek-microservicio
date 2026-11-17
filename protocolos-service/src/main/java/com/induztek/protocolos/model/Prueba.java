package com.induztek.protocolos.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prueba", uniqueConstraints = @UniqueConstraint(
        columnNames = {"id_protocolo", "id_tipo_prueba", "elemento", "momento"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prueba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_protocolo", nullable = false)
    private Protocolo protocolo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_prueba", nullable = false)
    private TipoPrueba tipoPrueba;

    @Column(nullable = false, length = 30)
    private String elemento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MomentoPrueba momento;

    @Column(nullable = false)
    private boolean realizada;

    @Column(name = "motivo_no_realizada", columnDefinition = "TEXT")
    private String motivoNoRealizada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_instrumento")
    private Instrumento instrumento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ResultadoPrueba resultado;

    @Column(name = "valor_calculado", precision = 14, scale = 4)
    private BigDecimal valorCalculado;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @OneToMany(mappedBy = "prueba", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Medicion> mediciones = new ArrayList<>();
}
