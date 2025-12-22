package com.eventui.common.contract.ui;

/**
 * Contexto de renderizado que contiene información sobre
 * el estado actual del renderizado (posición, escala, viewport, etc.)
 *
 * Esta interfaz será implementada por el módulo Fabric.
 */
public interface RenderContext {

    /**
     * @return Ancho del viewport en píxeles
     */
    int getWidth();

    /**
     * @return Alto del viewport en píxeles
     */
    int getHeight();

    /**
     * @return Factor de escala de la UI (para HiDPI)
     */
    float getScale();

    /**
     * @return Delta time desde el último frame (para animaciones)
     */
    float getDeltaTime();

    /**
     * @return Mouse X absoluto en píxeles
     */
    int getMouseX();

    /**
     * @return Mouse Y absoluto en píxeles
     */
    int getMouseY();
}
