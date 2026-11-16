package com.induztek.protocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "calibracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calibracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_instrumento", nullable = false)
    private Instrumento instrumento;

    @Column(name = "fecha_calibracion", nullable = false)
    private LocalDate fechaCalibracion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;
}
