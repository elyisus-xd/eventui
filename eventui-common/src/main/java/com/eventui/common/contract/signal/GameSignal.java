package com.eventui.common.contract.signal;

import java.util.UUID;

/**
 * Señales abstractas del juego que el Core consume.
 * El Motor de Señales traduce eventos de Minecraft a estas señales genéricas.
 * El Core NO conoce eventos de Minecraft directamente.
 */
public sealed interface GameSignal permits
        GameSignal.EntityKilled,
        GameSignal.ItemCrafted,
        GameSignal.BlockPlaced,
        GameSignal.BlockBroken,
        GameSignal.ItemPickedUp,
        GameSignal.TimePassed,
        GameSignal.LocationReached {

    /**
     * @return ID del jugador que generó la señal
     */
    UUID playerId();

    /**
     * @return Timestamp de cuando ocurrió (epoch millis)
     */
    long timestamp();

    /**
     * Jugador mató una entidad.
     */
    record EntityKilled(
            UUID playerId,
            String entityType,
            String dimension,
            long timestamp
    ) implements GameSignal {

        public EntityKilled(UUID playerId, String entityType, String dimension) {
            this(playerId, entityType, dimension, System.currentTimeMillis());
        }
    }

    /**
     * Jugador crafteó un item.
     */
    record ItemCrafted(
            UUID playerId,
            String itemId,
            int count,
            long timestamp
    ) implements GameSignal {

        public ItemCrafted(UUID playerId, String itemId, int count) {
            this(playerId, itemId, count, System.currentTimeMillis());
        }
    }

    /**
     * Jugador colocó un bloque.
     */
    record BlockPlaced(
            UUID playerId,
            String blockType,
            String dimension,
            long timestamp
    ) implements GameSignal {

        public BlockPlaced(UUID playerId, String blockType, String dimension) {
            this(playerId, blockType, dimension, System.currentTimeMillis());
        }
    }

    /**
     * Jugador rompió un bloque.
     */
    record BlockBroken(
            UUID playerId,
            String blockType,
            String dimension,
            long timestamp
    ) implements GameSignal {

        public BlockBroken(UUID playerId, String blockType, String dimension) {
            this(playerId, blockType, dimension, System.currentTimeMillis());
        }
    }

    /**
     * Jugador recogió un item.
     */
    record ItemPickedUp(
            UUID playerId,
            String itemId,
            int count,
            long timestamp
    ) implements GameSignal {

        public ItemPickedUp(UUID playerId, String itemId, int count) {
            this(playerId, itemId, count, System.currentTimeMillis());
        }
    }

    /**
     * Pasó cierto tiempo (para objetivos de duración).
     */
    record TimePassed(
            UUID playerId,
            long seconds,
            long timestamp
    ) implements GameSignal {

        public TimePassed(UUID playerId, long seconds) {
            this(playerId, seconds, System.currentTimeMillis());
        }
    }

    /**
     * Jugador llegó a una ubicación específica.
     */
    record LocationReached(
            UUID playerId,
            String locationId,
            String dimension,
            long timestamp
    ) implements GameSignal {

        public LocationReached(UUID playerId, String locationId, String dimension) {
            this(playerId, locationId, dimension, System.currentTimeMillis());
        }
    }
}
