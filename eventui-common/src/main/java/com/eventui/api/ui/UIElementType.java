package com.eventui.api.ui;

/**
 * Tipos de elementos UI soportados por EventUI.
 * ARQUITECTURA:
 * - Define los componentes básicos de la interfaz
 * - Cada tipo se renderiza de forma diferente en el MOD
 * - Son bloques de construcción declarativos
 * - Empezamos simple: solo imágenes y botones (FASE 1)
 */
public enum UIElementType {
    /**
     * Imagen estática (fondo, decoración, iconos)
     */
    IMAGE,

    /**
     * Botón clickeable
     */
    BUTTON,

    /**
     * Texto renderizado
     */
    TEXT,

    /**
     * Barra de progreso visual
     */
    PROGRESS_BAR,

    /**
     * Lista scrolleable de elementos
     */
    LIST,

    /**
     * Panel contenedor (agrupa otros elementos)
     */
    PANEL
}
