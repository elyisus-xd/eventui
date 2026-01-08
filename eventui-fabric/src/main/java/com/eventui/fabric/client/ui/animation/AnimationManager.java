package com.eventui.fabric.client.ui.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona múltiples animaciones simultáneas.
 *
 * Responsabilidades:
 * - Mantener lista de animaciones activas
 * - Actualizar todas las animaciones en cada tick
 * - Remover animaciones terminadas automáticamente
 * - Limitar número de animaciones concurrentes (performance)
 *
 * Ejemplo de uso:
 * <pre>
 * // En tu Screen:
 * private final AnimationManager animationManager = new AnimationManager();
 *
 * // Agregar animaciones:
 * animationManager.play(fadeInAnimation);
 * animationManager.play(scrollAnimation);
 *
 * // En tick():
 * animationManager.tick();
 * </pre>
 */
public class AnimationManager {

    private static final int MAX_CONCURRENT_ANIMATIONS = 100;

    private final List<Animation> activeAnimations;

    public AnimationManager() {
        this.activeAnimations = new ArrayList<>();
    }

    /**
     * Agrega y comienza una animación.
     * Si se alcanza el límite de animaciones, se remueven las más antiguas.
     *
     * @param animation Animación a reproducir
     */
    public void play(Animation animation) {
        if (animation == null) {
            throw new IllegalArgumentException("Animation cannot be null");
        }

        // Limitar animaciones concurrentes para evitar problemas de performance
        if (activeAnimations.size() >= MAX_CONCURRENT_ANIMATIONS) {
            // Remover la animación más antigua
            activeAnimations.remove(0);
        }

        // Iniciar y agregar
        animation.start();
        activeAnimations.add(animation);
    }

    /**
     * Actualiza todas las animaciones activas.
     * Debe llamarse en Screen.tick() o al inicio de Screen.render().
     */
    public void tick() {
        // Iterar en reversa para poder remover mientras iteramos
        for (int i = activeAnimations.size() - 1; i >= 0; i--) {
            Animation animation = activeAnimations.get(i);

            // Actualizar animación
            animation.tick();

            // Remover si terminó
            if (animation.isFinished()) {
                activeAnimations.remove(i);
            }
        }
    }

    /**
     * Detiene y remueve todas las animaciones activas.
     * Útil al cerrar una pantalla o resetear estado.
     */
    public void stopAll() {
        activeAnimations.clear();
    }

    /**
     * Detiene y remueve todas las animaciones que cumplan una condición.
     *
     * @param predicate Condición para remover (retorna true para remover)
     */
    public void stopIf(java.util.function.Predicate<Animation> predicate) {
        activeAnimations.removeIf(predicate);
    }

    /**
     * @return Número de animaciones actualmente activas
     */
    public int getActiveCount() {
        return activeAnimations.size();
    }

    /**
     * @return true si hay animaciones en ejecución
     */
    public boolean hasActiveAnimations() {
        return !activeAnimations.isEmpty();
    }

    /**
     * Pausa todas las animaciones.
     * Nota: Implementación básica, las animaciones seguirán "corriendo"
     * pero no se actualizarán hasta que se llame a resume().
     */
    private boolean paused = false;

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Versión modificada de tick que respeta el estado pausado.
     */
    public void tickWithPause() {
        if (paused) return;
        tick();
    }
}
