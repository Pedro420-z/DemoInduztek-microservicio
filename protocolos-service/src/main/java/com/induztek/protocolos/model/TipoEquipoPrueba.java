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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_equipo_prueba", uniqueConstraints = @UniqueConstraint(columnNames = {"id_tipo_equipo", "id_tipo_prueba"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoEquipoPrueba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_tipo_equipo", nullable = false)
    private Long tipoEquipoId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_prueba", nullable = false)
    private TipoPrueba tipoPrueba;
}
