package com.induztek.protocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrumento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 60)
    private String marca;

    @Column(length = 60)
    private String modelo;

    @Column(name = "numero_serie", nullable = false, unique = true, length = 60)
    private String numeroSerie;
}
