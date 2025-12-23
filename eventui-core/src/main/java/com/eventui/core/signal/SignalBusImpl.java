package com.eventui.core.signal;

import com.eventui.common.contract.signal.GameSignal;
import com.eventui.common.contract.signal.SignalBus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implementación thread-safe del bus de señales del juego.
 */
public class SignalBusImpl implements SignalBus {
    // Listeners específicos por tipo
    private final Map<Class<? extends GameSignal>, List<Consumer<? extends GameSignal>>> typedListeners;

    // Listeners para todas las señales
    private final List<Consumer<GameSignal>> globalListeners;

    public SignalBusImpl() {
        this.typedListeners = new ConcurrentHashMap<>();
        this.globalListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameSignal> void subscribe(Class<T> signalType, Consumer<T> handler) {
        typedListeners.computeIfAbsent(signalType, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<GameSignal>) handler);
    }

    @Override
    public void subscribeAll(Consumer<GameSignal> handler) {
        if (!globalListeners.contains(handler)) {
            globalListeners.add(handler);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameSignal> void unsubscribe(Class<T> signalType, Consumer<T> handler) {
        List<Consumer<? extends GameSignal>> handlers = typedListeners.get(signalType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void emit(GameSignal signal) {
        // Notificar listeners específicos del tipo
        Class<? extends GameSignal> signalClass = signal.getClass();
        List<Consumer<? extends GameSignal>> handlers = typedListeners.get(signalClass);
        if (handlers != null) {
            for (Consumer<? extends GameSignal> handler : handlers) {
                try {
                    ((Consumer<GameSignal>) handler).accept(signal);
                } catch (Exception e) {
                    System.err.println("Error processing signal: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Notificar listeners globales
        for (Consumer<GameSignal> handler : globalListeners) {
            try {
                handler.accept(signal);
            } catch (Exception e) {
                System.err.println("Error processing signal (global): " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clear() {
        typedListeners.clear();
        globalListeners.clear();
    }
}
