package com.eventui.config;

/**
 * Documentación del esquema de archivos de configuración JSON.*
 * ARQUITECTURA:
 * - Esta clase NO contiene lógica, solo documentación
 * - Define la estructura esperada de los archivos JSON
 * - El PLUGIN es responsable de parsear estos archivos
 * - Sirve como referencia para creadores de contenido*
 * UBICACIÓN DE ARCHIVOS:
 * - plugins/EventUI/events/*.json (definiciones de eventos)
 * - plugins/EventUI/ui/*.json (configuraciones de UI)
 */
public final class ConfigSchema {

    private ConfigSchema() {
        // Clase de documentación, no instanciable
    }

    /**
     * ESQUEMA: event_definition.json*
     * Define un evento completo con sus objetivos.*
     * Ejemplo:
     * {
     *   "id": "tutorial_mining",
     *   "display_name": "§6Tutorial de Minería",
     *   "description": "Aprende a minar tus primeros recursos",
     *   "objectives": [
     *     {
     *       "id": "mine_stone",
     *       "type": "MINE_BLOCK",
     *       "description": "Mina 10 bloques de piedra",
     *       "target_amount": 10,
     *       "parameters": {
     *         "block_id": "minecraft:stone"
     *       },
     *       "ui_resources": {
     *         "icon": "textures/objectives/stone.png"
     *       }
     *     },
     *     {
     *       "id": "mine_coal",
     *       "type": "MINE_BLOCK",
     *       "description": "Mina 5 carbones",
     *       "target_amount": 5,
     *       "parameters": {
     *         "block_id": "minecraft:coal_ore"
     *       }
     *     }
     *   ],
     *   "ui_resources": {
     *     "icon": "textures/events/mining_icon.png",
     *     "background": "textures/events/mining_bg.png"
     *   },
     *   "metadata": {
     *     "category": "tutorial",
     *     "repeatable": "false"
     *   }
     * }
     */
    public static final String EVENT_DEFINITION_SCHEMA = """
        {
          "id": "string (required, unique)",
          "display_name": "string (required)",
          "description": "string (required)",
          "objectives": [
            {
              "id": "string (required, unique within event)",
              "type": "enum: MINE_BLOCK | KILL_ENTITY | CRAFT_ITEM | COLLECT_ITEM | REACH_LOCATION | INTERACT | CUSTOM",
              "description": "string (required)",
              "target_amount": "number (required)",
              "parameters": "object (optional, type-specific)",
              "ui_resources": "object (optional)",
              "optional": "boolean (optional, default: false)"
            }
          ],
          "ui_resources": "object (optional)",
          "metadata": "object (optional)"
        }
        """;

    /**
     * ESQUEMA: ui_config.json*
     * Define la interfaz gráfica de un evento.*
     * Ejemplo FASE 1 (una pantalla simple):
     * {
     *   "id": "tutorial_mining_ui",
     *   "title": "Tutorial de Minería",
     *   "screen_width": 256,
     *   "screen_height": 200,
     *   "associated_event_id": "tutorial_mining",
     *   "screen_properties": {
     *     "background_color": "#2C2C2C",
     *     "blur_background": "true"
     *   },
     *   "root_elements": [
     *     {
     *       "id": "background",
     *       "type": "IMAGE",
     *       "x": 0,
     *       "y": 0,
     *       "width": 256,
     *       "height": 200,
     *       "properties": {
     *         "texture": "eventui:textures/gui/event_background.png"
     *       },
     *       "z_index": 0
     *     },
     *     {
     *       "id": "title_text",
     *       "type": "TEXT",
     *       "x": 128,
     *       "y": 20,
     *       "width": 200,
     *       "height": 20,
     *       "properties": {
     *         "content": "§6§lTutorial de Minería",
     *         "color": "#FFFFFF",
     *         "shadow": "true",
     *         "align": "center"
     *       },
     *       "z_index": 10
     *     },
     *     {
     *       "id": "close_button",
     *       "type": "BUTTON",
     *       "x": 220,
     *       "y": 10,
     *       "width": 20,
     *       "height": 20,
     *       "properties": {
     *         "texture": "eventui:textures/gui/button_close.png",
     *         "hover_texture": "eventui:textures/gui/button_close_hover.png",
     *         "action": "close_screen"
     *       },
     *       "z_index": 100
     *     }
     *   ]
     * }
     */
    public static final String UI_CONFIG_SCHEMA = """
        {
          "id": "string (required, unique)",
          "title": "string (optional)",
          "screen_width": "number (optional, default: 176)",
          "screen_height": "number (optional, default: 166)",
          "associated_event_id": "string (optional, links to event)",
          "screen_properties": "object (optional)",
          "root_elements": [
            {
              "id": "string (required, unique within UI)",
              "type": "enum: IMAGE | BUTTON | TEXT | PROGRESS_BAR | LIST | PANEL",
              "x": "number (required)",
              "y": "number (required)",
              "width": "number (required)",
              "height": "number (required)",
              "properties": "object (type-specific)",
              "children": "array (optional, for PANEL type)",
              "visible": "boolean (optional, default: true)",
              "z_index": "number (optional, default: 0)"
            }
          ]
        }
        """;

    /**
     * PROPIEDADES ESPECÍFICAS POR TIPO DE ELEMENTO UI
     */
    public static final String UI_ELEMENT_PROPERTIES = """
        IMAGE:
          - texture: "string (resource location)"
          - uv_x: "number (optional, texture offset X)"
          - uv_y: "number (optional, texture offset Y)"
          - texture_width: "number (optional, for sprite sheets)"
          - texture_height: "number (optional, for sprite sheets)"
        
        BUTTON:
          - texture: "string (normal state)"
          - hover_texture: "string (hover state)"
          - pressed_texture: "string (optional, pressed state)"
          - disabled_texture: "string (optional, disabled state)"
          - action: "string (button action: close_screen, open_event, etc.)"
          - tooltip: "string (optional, hover tooltip)"
        
        TEXT:
          - content: "string (text to display, supports § color codes)"
          - color: "string (hex color, optional)"
          - shadow: "boolean (optional, default: true)"
          - align: "string (left | center | right, optional)"
          - font_size: "number (optional, scale factor)"
        
        PROGRESS_BAR:
          - bar_texture: "string (filled portion)"
          - bg_texture: "string (background/empty portion)"
          - direction: "string (horizontal | vertical)"
          - data_source: "string (event_progress | objective_progress)"
          - objective_id: "string (optional, if tracking specific objective)"
        
        PANEL:
          - background_texture: "string (optional)"
          - border_size: "number (optional)"
          - padding: "number (optional, inner spacing)"
        
        LIST:
          - item_height: "number (height of each item)"
          - scrollable: "boolean (optional, default: true)"
          - scroll_bar_texture: "string (optional)"
          - data_source: "string (objectives_list | events_list)"
        """;

    /**
     * PARÁMETROS ESPECÍFICOS POR TIPO DE OBJETIVO
     */
    public static final String OBJECTIVE_PARAMETERS = """
        MINE_BLOCK:
          - block_id: "string (required, minecraft:stone)"
          - silk_touch: "boolean (optional, must use silk touch)"
        
        KILL_ENTITY:
          - entity_type: "string (required, minecraft:zombie)"
          - min_distance: "number (optional, minimum distance from spawn)"
          - with_weapon: "string (optional, specific weapon ID)"
        
        CRAFT_ITEM:
          - item_id: "string (required, minecraft:diamond_sword)"
          - count: "number (optional, items per craft counts as X)"
        
        COLLECT_ITEM:
          - item_id: "string (required)"
          - check_inventory: "boolean (optional, default: true)"
        
        REACH_LOCATION:
          - x: "number (required)"
          - y: "number (optional, any Y if not specified)"
          - z: "number (required)"
          - radius: "number (optional, default: 5)"
          - dimension: "string (optional, minecraft:overworld)"
        
        INTERACT:
          - target_type: "string (block | entity)"
          - target_id: "string (minecraft:villager)"
          - interaction_type: "string (optional, right_click | left_click)"
        
        CUSTOM:
          - custom_handler: "string (identifier for custom logic)"
          - (any additional custom parameters)
        """;
}
