package com.induztek.protocolos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "visita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ot", nullable = false, unique = true, length = 20)
    private String numeroOt;

    @Column(name = "id_instalacion", nullable = false)
    private Long instalacionId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_visita", nullable = false, length = 20)
    private TipoVisita tipoVisita;

    @Column(name = "id_ingeniero_cargo", nullable = false)
    private Long ingenieroId;

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "visita_participante", joinColumns = @JoinColumn(name = "id_visita"))
    @Column(name = "id_usuario", nullable = false)
    @Builder.Default
    private Set<Long> participantesIds = new LinkedHashSet<>();
}
