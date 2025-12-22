package com.eventui.common.contract.ui;

import com.eventui.common.dto.DataSnapshot;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Contrato entre la UI y el ViewModel.
 * La UI usa esta interfaz para suscribirse a datos y enviar acciones.
 *
 * La UI NUNCA debe importar clases del Core directamente.
 */
public interface UIContext {

    /**
     * Suscribe un componente de UI para recibir actualizaciones de datos.
     *
     * @param dataKey Identificador lógico del dato (ej: "missions.active", "progress.kill_zombies")
     * @param callback Función que se llama cuando los datos cambian
     */
    void subscribe(String dataKey, Consumer<DataSnapshot> callback);

    /**
     * Cancela la suscripción de un componente.
     *
     * @param dataKey Identificador del dato
     */
    void unsubscribe(String dataKey);

    /**
     * Obtiene un snapshot actual de los datos sin suscribirse.
     *
     * @param dataKey Identificador del dato
     * @return Snapshot inmutable de los datos, o null si no existe
     */
    DataSnapshot getData(String dataKey);

    /**
     * Envía una acción del usuario al ViewModel/Core.
     *
     * @param actionId Identificador de la acción (ej: "accept_mission", "abandon_mission")
     * @param params Parámetros de la acción (ej: {"missionId": "kill_zombies"})
     */
    void sendAction(String actionId, Map<String, Object> params);
}
