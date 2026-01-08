package com.eventui.fabric.client.ui.animation;

import java.util.function.Consumer;

/**
 * Anima un color ARGB (Alpha, Red, Green, Blue) de un valor a otro.*
 * Útil para:
 * - Transiciones de color de fondo
 * - Cambios de color de texto
 * - Efectos de highlight
 ** Los colores se interpolan canal por canal (A, R, G, B).*
 * Ejemplo de uso:
 * <pre>
 * // Transición de rojo a verde
 * ColorAnimation colorTransition = new ColorAnimation(
 *     0xFFFF0000, // Rojo opaco
 *     0xFF00FF00, // Verde opaco
 *     500,
 *     Easing.EASE_IN_OUT_CUBIC,
 *     color -> this.backgroundColor = color
 * );
 * animationManager.play(colorTransition);
 * </pre>
 */
public class ColorAnimation extends Animation {

    private final int fromColor;       // Color inicial (ARGB)
    private final int toColor;         // Color final (ARGB)
    private final Consumer<Integer> onUpdate; // Callback con color actual
    private int currentColor;          // Color actual

    /**
     * Crea una nueva animación de color.
     *
     * @param fromColor Color inicial en formato ARGB (0xAARRGGBB)
     * @param toColor Color final en formato ARGB (0xAARRGGBB)
     * @param durationMs Duración en milisegundos
     * @param easing Función de easing
     * @param onUpdate Callback que recibe el color interpolado
     */
    public ColorAnimation(int fromColor, int toColor, long durationMs,
                          Easing easing, Consumer<Integer> onUpdate) {
        super(durationMs, easing);

        if (onUpdate == null) {
            throw new IllegalArgumentException("onUpdate callback cannot be null");
        }

        this.fromColor = fromColor;
        this.toColor = toColor;
        this.onUpdate = onUpdate;
        this.currentColor = fromColor;
    }

    @Override
    protected void update(float progress) {
        // Extraer canales del color inicial
        int fromA = (fromColor >> 24) & 0xFF;
        int fromR = (fromColor >> 16) & 0xFF;
        int fromG = (fromColor >> 8) & 0xFF;
        int fromB = fromColor & 0xFF;

        // Extraer canales del color final
        int toA = (toColor >> 24) & 0xFF;
        int toR = (toColor >> 16) & 0xFF;
        int toG = (toColor >> 8) & 0xFF;
        int toB = toColor & 0xFF;

        // Interpolar cada canal
        int currentA = (int) (fromA + (toA - fromA) * progress);
        int currentR = (int) (fromR + (toR - fromR) * progress);
        int currentG = (int) (fromG + (toG - fromG) * progress);
        int currentB = (int) (fromB + (toB - fromB) * progress);

        // Reconstruir color ARGB
        currentColor = (currentA << 24) | (currentR << 16) | (currentG << 8) | currentB;

        // Notificar callback
        onUpdate.accept(currentColor);
    }

    /**
     * @return El color actual de la animación (ARGB)
     */
    public int getCurrentColor() {
        return currentColor;
    }

    /**
     * @return El color inicial (ARGB)
     */
    public int getFromColor() {
        return fromColor;
    }

    /**
     * @return El color final (ARGB)
     */
    public int getToColor() {
        return toColor;
    }

    /**
     * Constructor conveniente con easing por defecto.
     *
     * @param fromColor Color inicial (ARGB)
     * @param toColor Color final (ARGB)
     * @param durationMs Duración en milisegundos
     * @param onUpdate Callback con color actual
     * @return Nueva animación configurada
     */
    public static ColorAnimation create(int fromColor, int toColor, long durationMs,
                                        Consumer<Integer> onUpdate) {
        return new ColorAnimation(fromColor, toColor, durationMs,
                Easing.EASE_OUT_CUBIC, onUpdate);
    }

    /**
     * Crea una animación de fade-in de un color.
     * Anima el alpha de 0 a 255, manteniendo RGB constante.
     *
     * @param color Color base (se ignorará el alpha)
     * @param durationMs Duración
     * @param onUpdate Callback
     * @return Animación de fade-in configurada
     */
    public static ColorAnimation fadeInColor(int color, long durationMs,
                                             Consumer<Integer> onUpdate) {
        int rgb = color & 0x00FFFFFF; // Extraer solo RGB
        int transparentColor = rgb; // Alpha = 0
        int opaqueColor = 0xFF000000 | rgb; // Alpha = 255

        return new ColorAnimation(transparentColor, opaqueColor, durationMs,
                Easing.EASE_OUT_CUBIC, onUpdate);
    }

    /**
     * Crea una animación de fade-out de un color.
     * Anima el alpha de 255 a 0, manteniendo RGB constante.
     *
     * @param color Color base (se ignorará el alpha)
     * @param durationMs Duración
     * @param onUpdate Callback
     * @return Animación de fade-out configurada
     */
    public static ColorAnimation fadeOutColor(int color, long durationMs,
                                              Consumer<Integer> onUpdate) {
        int rgb = color & 0x00FFFFFF; // Extraer solo RGB
        int opaqueColor = 0xFF000000 | rgb; // Alpha = 255
        int transparentColor = rgb; // Alpha = 0

        return new ColorAnimation(opaqueColor, transparentColor, durationMs,
                Easing.EASE_OUT_CUBIC, onUpdate);
    }
}
