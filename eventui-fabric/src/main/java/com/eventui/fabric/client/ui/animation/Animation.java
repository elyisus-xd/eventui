package com.eventui.fabric.client.ui.animation;

/**
 * Clase base abstracta para todas las animaciones en EventUI.
 * Ciclo de vida:
 * 1. Crear animación
 * 2. Llamar a start()
 * 3. Llamar a tick() en cada frame
 * 4. Cuando termine, isFinished() retorna true
 * Ejemplo de uso:
 * <pre>
 * Animation fadeIn = new FloatAnimation(0f, 1f, 300, Easing.EASE_OUT_CUBIC,
 *     alpha -> this.screenAlpha = alpha);
 * fadeIn.start();
 *
 * // En tu método tick() o render():
 * fadeIn.tick();
 * </pre>
 */
public abstract class Animation {

    protected final long duration; // Duración en milisegundos
    protected long startTime;      // Timestamp de inicio
    protected boolean started;     // Si la animación ha comenzado
    protected boolean finished;    // Si la animación ha terminado
    protected Easing easing;       // Función de interpolación

    /**
     * Crea una nueva animación.
     *
     * @param durationMs Duración en milisegundos
     * @param easing Función de easing a aplicar
     */
    public Animation(long durationMs, Easing easing) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("Duration must be positive, got: " + durationMs);
        }
        if (easing == null) {
            throw new IllegalArgumentException("Easing cannot be null");
        }

        this.duration = durationMs;
        this.easing = easing;
        this.started = false;
        this.finished = false;
    }

    /**
     * Inicia la animación.
     * Registra el timestamp actual como punto de inicio.
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.started = true;
        this.finished = false;
    }

    /**
     * Actualiza la animación.
     * Debe llamarse en cada tick/frame.
     */
    public void tick() {
        if (!started || finished) {
            return;
        }

        // Calcular progreso lineal (0.0 a 1.0)
        long elapsed = System.currentTimeMillis() - startTime;
        float linearProgress = Math.min(1.0f, (float) elapsed / duration);

        // Aplicar función de easing
        float easedProgress = easing.apply(linearProgress);

        // Delegar actualización a subclases
        update(easedProgress);

        // Marcar como terminada si llegamos al final
        if (linearProgress >= 1.0f) {
            finish();
        }
    }

    /**
     * Método abstracto implementado por subclases.
     * Aquí se actualiza el valor específico que se está animando.
     *
     * @param progress Progreso con easing aplicado (0.0 a 1.0)
     */
    protected abstract void update(float progress);

    /**
     * Finaliza la animación y llama al callback onComplete.
     */
    protected void finish() {
        this.finished = true;
        onComplete();
    }

    /**
     * Callback opcional llamado cuando la animación termina.
     * Override en subclases si necesitas ejecutar código al completar.
     */
    protected void onComplete() {
        // Por defecto no hace nada
    }

    /**
     * @return true si la animación ha terminado
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * @return true si la animación ha comenzado
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Resetea la animación para poder reutilizarla.
     * Útil para animaciones cíclicas.
     */
    public void reset() {
        this.started = false;
        this.finished = false;
    }

    /**
     * @return Duración de la animación en milisegundos
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return Progreso actual de la animación (0.0 a 1.0)
     */
    public float getProgress() {
        if (!started) return 0.0f;
        if (finished) return 1.0f;

        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, (float) elapsed / duration);
    }
}
