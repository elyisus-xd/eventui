package com.eventui.fabric.client.ui.animation;

/**
 * Funciones de interpolación (easing) para animaciones suaves.
 *
 * Basado en: https://easings.net/
 *
 * Uso:
 * - LINEAR: Sin aceleración, velocidad constante
 * - EASE_OUT_*: Empieza rápido, desacelera al final (más común para UIs)
 * - EASE_IN_*: Empieza lento, acelera al final
 * - EASE_IN_OUT_*: Aceleración al inicio y desaceleración al final
 */
@FunctionalInterface
public interface Easing {

    /**
     * Aplica la función de easing al progreso lineal.
     *
     * @param t Progreso lineal (0.0 = inicio, 1.0 = final)
     * @return Progreso transformado (0.0 a 1.0)
     */
    float apply(float t);

    // ========== BÁSICAS ==========

    /**
     * Sin aceleración, velocidad constante.
     */
    Easing LINEAR = t -> t;

    // ========== QUADRATIC (Cuadrático - suave) ==========

    /**
     * Aceleración cuadrática desde cero velocidad.
     */
    Easing EASE_IN_QUAD = t -> t * t;

    /**
     * Desaceleración cuadrática hasta cero velocidad.
     * ⭐ Recomendado para: hover, tooltips, elementos pequeños.
     */
    Easing EASE_OUT_QUAD = t -> t * (2 - t);

    /**
     * Aceleración hasta la mitad, luego desaceleración.
     */
    Easing EASE_IN_OUT_QUAD = t ->
            t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;

    // ========== CUBIC (Cúbico - más pronunciado) ==========

    /**
     * Aceleración cúbica desde cero velocidad.
     */
    Easing EASE_IN_CUBIC = t -> t * t * t;

    /**
     * Desaceleración cúbica hasta cero velocidad.
     * ⭐ Recomendado para: scroll, transiciones de pantalla, fade-in.
     */
    Easing EASE_OUT_CUBIC = t -> {
        float f = t - 1;
        return f * f * f + 1;
    };

    /**
     * Aceleración cúbica hasta la mitad, luego desaceleración.
     */
    Easing EASE_IN_OUT_CUBIC = t ->
            t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;

    // ========== EXPONENTIAL (Exponencial - muy dramático) ==========

    /**
     * Desaceleración exponencial hasta cero velocidad.
     * ⭐ Recomendado para: animaciones dramáticas, efectos especiales.
     */
    Easing EASE_OUT_EXPO = t ->
            t == 1.0f ? 1.0f : 1 - (float) Math.pow(2, -10 * t);

    /**
     * Aceleración exponencial desde cero velocidad.
     */
    Easing EASE_IN_EXPO = t ->
            t == 0.0f ? 0.0f : (float) Math.pow(2, 10 * (t - 1));

    // ========== ELASTIC (Elástico - efecto de rebote) ==========

    /**
     * Desaceleración con rebote elástico al final.
     * ⭐ Recomendado para: elementos lúdicos, notificaciones, logros.
     */
    Easing EASE_OUT_ELASTIC = t -> {
        if (t == 0 || t == 1) return t;

        float p = 0.3f;
        return (float) (Math.pow(2, -10 * t) *
                Math.sin((t - p / 4) * (2 * Math.PI) / p) + 1);
    };

    // ========== BACK (Retroceso - overshoot) ==========

    /**
     * Desacelera y sobrepasa ligeramente el objetivo antes de volver.
     * ⭐ Recomendado para: elementos que "aterrizan", stagger effects.
     */
    Easing EASE_OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    };

    /**
     * Retrocede ligeramente antes de acelerar hacia el objetivo.
     */
    Easing EASE_IN_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    };

    // ========== BOUNCE (Rebote físico) ==========

    /**
     * Rebote físico al final de la animación.
     * ⭐ Recomendado para: elementos que "caen", efectos cómicos.
     */
    Easing EASE_OUT_BOUNCE = t -> {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5 / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    };
}
