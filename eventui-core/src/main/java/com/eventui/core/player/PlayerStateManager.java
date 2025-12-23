package com.eventui.core.player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el estado de misiones de todos los jugadores.
 */
public class PlayerStateManager {
    private final Map<UUID, PlayerMissionData> playerData;

    public PlayerStateManager() {
        this.playerData = new ConcurrentHashMap<>();
    }

    /**
     * Obtiene o crea los datos de un jugador.
     */
    public PlayerMissionData getOrCreate(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerMissionData::new);
    }

    /**
     * Obtiene los datos de un jugador si existen.
     */
    public Optional<PlayerMissionData> get(UUID playerId) {
        return Optional.ofNullable(playerData.get(playerId));
    }

    /**
     * Elimina los datos de un jugador (cuando se desconecta).
     */
    public void remove(UUID playerId) {
        playerData.remove(playerId);
    }

    /**
     * @return true si el jugador tiene datos cargados
     */
    public boolean hasData(UUID playerId) {
        return playerData.containsKey(playerId);
    }

    /**
     * @return Cantidad de jugadores con datos cargados
     */
    public int getLoadedPlayerCount() {
        return playerData.size();
    }

    /**
     * Limpia todos los datos (útil para testing).
     */
    public void clear() {
        playerData.clear();
    }
}
