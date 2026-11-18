package com.inventaya.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Bitacora de eventoId ya aplicados. Es el mecanismo de idempotencia del consumidor:
 * si un evento se reentrega (reintento de Kafka, rebalanceo, redeploy), su eventoId
 * ya estara en esta tabla y el efecto sobre el stock no se aplica dos veces.
 */
@Entity
@Table(name = "eventos_procesados")
@Getter
@Setter
@NoArgsConstructor
public class EventoProcesado {

    @Id
    @Column(name = "evento_id", nullable = false, updatable = false)
    private UUID eventoId;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    public static EventoProcesado de(UUID eventoId, String tipo) {
        EventoProcesado registro = new EventoProcesado();
        registro.eventoId = eventoId;
        registro.tipo = tipo;
        registro.procesadoEn = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return registro;
    }
}
