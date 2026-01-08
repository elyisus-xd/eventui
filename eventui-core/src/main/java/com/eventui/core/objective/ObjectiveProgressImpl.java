package com.eventui.core.objective;

import com.eventui.api.objective.ObjectiveProgress;

/**
 * Implementación mutable de ObjectiveProgress.
 *
 * ARQUITECTURA:
 * - Esta clase SÍ es mutable (el progreso cambia)
 * - El PLUGIN gestiona instancias de esta clase
 * - Thread-safe para actualizaciones concurrentes
 */
public class ObjectiveProgressImpl implements ObjectiveProgress {

    private final String objectiveId;
    private final int targetAmount;
    private volatile int currentAmount;
    private volatile boolean completed;

    public ObjectiveProgressImpl(String objectiveId, int targetAmount) {
        this.objectiveId = objectiveId;
        this.targetAmount = targetAmount;
        this.currentAmount = 0;
        this.completed = false;
    }

    public ObjectiveProgressImpl(String objectiveId, int targetAmount, int currentAmount) {
        this.objectiveId = objectiveId;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.completed = currentAmount >= targetAmount;
    }

    @Override
    public String getObjectiveId() {
        return objectiveId;
    }

    @Override
    public int getCurrentAmount() {
        return currentAmount;
    }

    @Override
    public int getTargetAmount() {
        return targetAmount;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Incrementa el progreso en la cantidad especificada.
     *
     * @param amount Cantidad a incrementar
     * @return true si el objetivo se completó con este incremento
     */
    public synchronized boolean increment(int amount) {
        if (completed) {
            return false; // Ya completado
        }

        currentAmount += amount;

        if (currentAmount >= targetAmount) {
            currentAmount = targetAmount; // Clamp al máximo
            completed = true;
            return true;
        }

        return false;
    }

    /**
     * Establece el progreso a una cantidad específica.
     * ✅ Usado para COLLECT_ITEM (inventario) y otros objetivos que requieren seteo directo.
     *
     * @param amount Nueva cantidad
     */
    public synchronized void setProgress(int amount) {
        this.currentAmount = Math.max(0, Math.min(amount, targetAmount));
        this.completed = this.currentAmount >= targetAmount;
    }

    /**
     * Resetea el progreso a 0.
     */
    public synchronized void reset() {
        this.currentAmount = 0;
        this.completed = false;
    }
}
