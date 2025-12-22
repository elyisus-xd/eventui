package com.eventui.common.contract.viewmodel;

import com.eventui.common.dto.DataSnapshot;
import java.util.Optional;

/**
 * Interfaz que el ViewModel implementa para proveer datos a la UI.
 * El ViewModel actúa como adaptador entre el Core y la UI,
 * traduciendo objetos del dominio a DataSnapshots genéricos.
 */
public interface DataProvider {

    /**
     * Resuelve un data key a un snapshot de datos.
     * Ejemplos de data keys:
     * - "missions.active" → lista de misiones activas
     * - "mission.kill_zombies.progress" → progreso de una misión específica
     * - "player.stats" → estadísticas del jugador
     *
     * @param dataKey Clave lógica del dato
     * @return Snapshot con los datos, o empty si la clave no existe
     */
    Optional<DataSnapshot> resolveDataKey(String dataKey);

    /**
     * Registra un data key que el ViewModel puede resolver.
     *
     * @param dataKey Clave a registrar
     * @param resolver Función que genera el snapshot cuando se solicita
     */
    void registerDataKey(String dataKey, DataKeyResolver resolver);
}

/**
 * Función que resuelve un data key a datos concretos.
 */
@FunctionalInterface
interface DataKeyResolver {
    /**
     * @return Mapa con los datos a incluir en el snapshot
     */
    java.util.Map<String, Object> resolve();
}
