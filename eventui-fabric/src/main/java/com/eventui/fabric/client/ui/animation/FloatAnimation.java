package com.eventui.fabric.client.ui.animation;

import java.util.function.Consumer;

/**
 * Anima un valor float desde 'from' hasta 'to'.*
 * Es la animación más versátil del sistema, útil para:
 * - Alpha (transparencia): 0.0 → 1.0
 * - Posiciones: 100.0 → 200.0
 * - Escalas: 1.0 → 1.5
 * - Cualquier valor numérico*
 * Ejemplo de uso:
 * <pre>
 * // Fade-in de un elemento
 * FloatAnimation fadeIn = new FloatAnimation(
 *     0f, 1f, 300, Easing.EASE_OUT_CUBIC,
 *     alpha -> this.elementAlpha = alpha
 * );
 * fadeIn.start();
 *
 * // En tick():
 * fadeIn.tick();
 *
 * // En render():
 * renderElementWithAlpha(elementAlpha);
 * </pre>
 */
public class FloatAnimation extends Animation {

    private final float from;          // Valor inicial
    private final float to;            // Valor final
    private final Consumer<Float> onUpdate; // Callback con el valor actual
    private float currentValue;        // Valor actual de la animación

    /**
     * Crea una nueva animación de float.
     *
     * @param from Valor inicial
     * @param to Valor final
     * @param durationMs Duración en milisegundos
     * @param easing Función de easing
     * @param onUpdate Callback que recibe el valor actualizado en cada tick
     */
    public FloatAnimation(float from, float to, long durationMs,
                          Easing easing, Consumer<Float> onUpdate) {
        super(durationMs, easing);

        if (onUpdate == null) {
            throw new IllegalArgumentException("onUpdate callback cannot be null");
        }

        this.from = from;
        this.to = to;
        this.onUpdate = onUpdate;
        this.currentValue = from;
    }

    @Override
    protected void update(float progress) {
        // Interpolación lineal con easing ya aplicado
        currentValue = from + (to - from) * progress;

        // Notificar al callback
        onUpdate.accept(currentValue);
    }

    /**
     * @return El valor actual de la animación
     */
    public float getCurrentValue() {
        return currentValue;
    }

    /**
     * @return El valor inicial
     */
    public float getFrom() {
        return from;
    }

    /**
     * @return El valor final
     */
    public float getTo() {
        return to;
    }

    /**
     * Constructor conveniente con easing por defecto (EASE_OUT_CUBIC).
     *
     * @param from Valor inicial
     * @param to Valor final
     * @param durationMs Duración en milisegundos
     * @param onUpdate Callback con el valor actualizado
     * @return Nueva animación configurada
     */
    public static FloatAnimation create(float from, float to, long durationMs,
                                        Consumer<Float> onUpdate) {
        return new FloatAnimation(from, to, durationMs, Easing.EASE_OUT_CUBIC, onUpdate);
    }

    /**
     * Constructor conveniente para fade-in (0 → 1).
     *
     * @param durationMs Duración en milisegundos
     * @param onUpdate Callback con el valor de alpha
     * @return Animación de fade-in configurada
     */
    public static FloatAnimation fadeIn(long durationMs, Consumer<Float> onUpdate) {
        return new FloatAnimation(0f, 1f, durationMs, Easing.EASE_OUT_CUBIC, onUpdate);
    }

    /**
     * Constructor conveniente para fade-out (1 → 0).
     *
     * @param durationMs Duración en milisegundos
     * @param onUpdate Callback con el valor de alpha
     * @return Animación de fade-out configurada
     */
    public static FloatAnimation fadeOut(long durationMs, Consumer<Float> onUpdate) {
        return new FloatAnimation(1f, 0f, durationMs, Easing.EASE_OUT_CUBIC, onUpdate);
    }
}
