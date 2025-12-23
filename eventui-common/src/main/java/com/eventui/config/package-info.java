/**
 * Paquete de configuración y esquemas de EventUI.
 *
 * <h2>Propósito</h2>
 * Este paquete define la estructura de los archivos de configuración JSON
 * que los creadores de contenido usarán para definir eventos y UIs.
 *
 * <h2>Archivos de configuración</h2>
 * <ul>
 *   <li><b>events/*.json</b> - Definiciones de eventos y objetivos</li>
 *   <li><b>ui/*.json</b> - Configuraciones de interfaces gráficas</li>
 * </ul>
 *
 * <h2>Flujo de trabajo</h2>
 * <ol>
 *   <li>Creador escribe archivos JSON siguiendo los esquemas</li>
 *   <li>PLUGIN lee y parsea estos archivos al iniciar</li>
 *   <li>PLUGIN valida que cumplan con el esquema</li>
 *   <li>PLUGIN expone los datos al MOD via EventBridge</li>
 *   <li>MOD renderiza la UI según la configuración</li>
 * </ol>
 *
 * <h2>FASE 1: Alcance mínimo</h2>
 * <ul>
 *   <li>Un solo archivo de evento</li>
 *   <li>Un solo archivo de UI</li>
 *   <li>Solo elementos IMAGE y BUTTON</li>
 *   <li>Sin persistencia de progreso (reinicia al reconectar)</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.eventui.config;
