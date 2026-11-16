package com.induztek.protocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recepcion_acta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionActa {

    @Id
    @Column(name = "id_visita")
    private Long visitaId;

    @Column(name = "receptor_nombre", nullable = false, length = 120)
    private String receptorNombre;

    @Column(name = "receptor_rut", nullable = false, length = 12)
    private String receptorRut;

    @Column(name = "receptor_cargo", nullable = false, length = 80)
    private String receptorCargo;

    @Column(name = "fecha_hora_recepcion", nullable = false)
    private LocalDateTime fechaHoraRecepcion;
}
