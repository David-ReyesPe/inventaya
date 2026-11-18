package com.inventaya.ventas.kafka;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Sobre comun de todos los eventos de InventaYa.
 *
 * <pre>
 * {
 *   "eventoId": "uuid",
 *   "tipo": "venta-realizada | venta-anulada | stock-bajo",
 *   "fecha": "2026-08-19T15:32:00Z",
 *   "payload": { }
 * }
 * </pre>
 *
 * El {@code eventoId} es la clave de idempotencia que usan los consumidores.
 */
public record EventoEnvelope<T>(UUID eventoId, String tipo, Instant fecha, T payload) {

    public static <T> EventoEnvelope<T> nuevo(String tipo, T payload) {
        return new EventoEnvelope<>(
                UUID.randomUUID(),
                tipo,
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                payload);
    }
}
