package com.eventui.common.contract.ui;


/**
 * Representa un componente de UI en el sistema declarativo.
 *
 * Los componentes se definen en JSON y el motor de renderizado
 * los instancia dinámicamente.
 */
public interface UIComponent {

    /**
     * @return ID único del componente en la definición JSON
     */
    String getId();

    /**
     * @return Tipo del componente (image, button, list, progress, etc.)
     */
    String getType();

    /**
     * Renderiza el componente en pantalla.
     *
     * @param context Contexto de renderizado con información de posición, escala, etc.
     */
    void render(RenderContext context);

    /**
     * Actualiza el estado interno del componente.
     * Llamado cuando los datos vinculados cambian.
     *
     * @param data Nuevos datos del ViewModel
     */
    void updateData(Object data);
}
