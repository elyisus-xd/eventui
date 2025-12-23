package com.eventui.core.event;

import com.eventui.common.contract.mission.MissionEvent;
import com.eventui.common.contract.mission.MissionEventBus;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implementación thread-safe del bus de eventos de misiones.
 */
public class EventBusImpl implements MissionEventBus {
    private final List<Consumer<MissionEvent>> listeners;

    public EventBusImpl() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void subscribe(Consumer<MissionEvent> listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void unsubscribe(Consumer<MissionEvent> listener) {
        listeners.remove(listener);
    }

    @Override
    public void publish(MissionEvent event) {
        for (Consumer<MissionEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Log pero no detener la propagación
                System.err.println("Error processing mission event: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clear() {
        listeners.clear();
    }

    /**
     * @return Cantidad de listeners suscritos
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
