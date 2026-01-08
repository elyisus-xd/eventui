package com.eventui.core.tracking;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventState;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.core.EventUIPlugin;
import com.eventui.core.bridge.PluginBridgeMessage;
import com.eventui.core.event.EventDefinitionImpl;
import com.eventui.core.event.EventProgressImpl;
import com.eventui.core.objective.ObjectiveProgressImpl;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ObjectiveTracker - Rastreador de progreso de objetivos
 * ═══════════════════════════════════════════════════════════════════════════

 * RESPONSABILIDAD:
 * - Escucha eventos del juego (romper bloques, matar entidades, etc.)
 * - Incrementa el progreso de objetivos cuando el jugador realiza acciones
 * - Detecta cuando un objetivo se completa
 * - Completa el evento cuando TODOS los objetivos están completados
 * - Entrega recompensas y muestra feedback épico

 * FLUJO DE EJECUCIÓN:
 * 1. Jugador rompe un bloque → onBlockBreak() se dispara
 * 2. Busca todos los eventos IN_PROGRESS del jugador
 * 3. Para cada evento, verifica si tiene un objetivo tipo MINE_BLOCK
 * 4. Si el bloque roto coincide con el requerido, incrementa el progreso
 * 5. Notifica al cliente vía PluginEventBridge
 * 6. Si el objetivo se completa, verifica si todos los objetivos están listos
 * 7. Si sí, marca el evento como COMPLETED y entrega recompensas

 * COLABORADORES:
 * - EventStorage: Para obtener y guardar el progreso
 * - PluginEventBridge: Para notificar al cliente (EventScreen en Fabric)
 * - RewardManager: Para entregar XP e items

 * LIMITACIÓN IMPORTANTE:
 * Este código asume que todos los objetivos de un evento son PARALELOS
 * (se pueden completar en cualquier orden). NO soporta objetivos secuenciales
 * donde debes completar el objetivo 1 antes de que se active el objetivo 2.

 * ═══════════════════════════════════════════════════════════════════════════
 */
public class ObjectiveTracker implements Listener {

    private static final Logger LOGGER = Logger.getLogger(ObjectiveTracker.class.getName());

    private final EventUIPlugin plugin;

    // ✅ NUEVO: Índice de eventos activos por jugador
    // UUID del jugador → Set de IDs de eventos IN_PROGRESS
    private final Map<UUID, Set<String>> activeEventsByPlayer = new ConcurrentHashMap<>();

    // ✅ NUEVO: Índice de eventos por tipo de objetivo
    // ObjectiveType → Set de IDs de eventos que contienen ese tipo
    private final Map<ObjectiveType, Set<String>> eventsByObjectiveType = new ConcurrentHashMap<>();

    public ObjectiveTracker(EventUIPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Maneja el evento de Bukkit cuando un jugador rompe un bloque.

     * LLAMADO POR: Bukkit automáticamente cuando BlockBreakEvent se dispara

     * FLUJO:
     * 1. Obtiene el jugador y el tipo de bloque roto
     * 2. Itera sobre TODOS los eventos registrados en el sistema
     * 3. Para cada evento, verifica si el jugador tiene progreso
     * 4. Si está IN_PROGRESS, busca objetivos tipo MINE_BLOCK
     * 5. Si el bloque coincide, incrementa el progreso
     * 6. Notifica al cliente y verifica completación
     */

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockTypeString = event.getBlock().getType().getKey().toString();

        // ✅ OPTIMIZACIÓN 1: Obtener solo eventos activos del jugador con objetivos MINE_BLOCK o BREAK_WITH_TOOL
        Set<String> relevantMineEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.MINE_BLOCK);
        Set<String> relevantToolEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.BREAK_WITH_TOOL);

        // Si no hay eventos relevantes, salir inmediatamente
        if (relevantMineEvents.isEmpty() && relevantToolEvents.isEmpty()) {
            return;
        }

        // Obtener herramienta una sola vez (para BREAK_WITH_TOOL)
        String toolType = null;
        if (!relevantToolEvents.isEmpty()) {
            org.bukkit.inventory.ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() != org.bukkit.Material.AIR) {
                toolType = itemInHand.getType().getKey().toString();
            }
        }

        // ✅ OPTIMIZACIÓN 2: Solo procesar eventos relevantes
        String finalToolType = toolType;

        // Procesar MINE_BLOCK
        relevantMineEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.MINE_BLOCK) {
                    String requiredBlock = objective.getParameters().get("block_id");

                    if (blockTypeString.equals(requiredBlock)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§aEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });

        // Procesar BREAK_WITH_TOOL
        if (finalToolType != null) {
            relevantToolEvents.forEach(eventId -> {
                var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
                if (eventDefOpt.isEmpty()) return;

                var eventDef = eventDefOpt.get();
                var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                if (progressOpt.isEmpty()) return;

                EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

                eventDef.getObjectives().forEach(objective -> {
                    if (objective.getType() == ObjectiveType.BREAK_WITH_TOOL) {
                        String requiredTool = objective.getParameters().get("tool_type");
                        String requiredBlock = objective.getParameters().get("block_id");

                        if (finalToolType.equals(requiredTool)) {
                            boolean blockMatches = (requiredBlock == null) || blockTypeString.equals(requiredBlock);

                            if (blockMatches) {
                                ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                                if (objProgress != null && !objProgress.isCompleted()) {
                                    boolean completed = objProgress.increment(1);

                                    player.sendMessage("§bEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                            objProgress.getTargetAmount() + " - " + objective.getDescription());

                                    plugin.getEventBridge().notifyProgressUpdate(
                                            player.getUniqueId(), eventDef.getId(), objective.getId(),
                                            objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                            objective.getDescription()
                                    );

                                    if (completed) {
                                        player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                        checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                                    }
                                }
                            }
                        }
                    }
                });
            });
        }
    }



    /**
     * Maneja el evento cuando un jugador coloca un bloque.*
     * FLUJO:
     * 1. Obtiene el tipo de bloque colocado
     * 2. Busca eventos IN_PROGRESS con objetivos PLACE_BLOCK
     * 3. Si el bloque coincide, incrementa progreso
     */
    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo PLACE_BLOCK
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.PLACE_BLOCK);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.PLACE_BLOCK) {
                    String requiredBlock = objective.getParameters().get("block_id");

                    if (blockType.equals(requiredBlock)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§aEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }


    /**
     * Maneja el evento cuando un jugador mata una entidad.*
     * FLUJO:
     * 1. Verifica que sea un jugador quien mató (no caída, lava, etc.)
     * 2. Obtiene el tipo de entidad muerta
     * 3. Busca eventos IN_PROGRESS del jugador con objetivos KILL_ENTITY
     * 4. Si el tipo coincide, incrementa progreso
     * 5. Verifica completación y entrega recompensas si aplica
     */
    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // Verificar que el killer sea un jugador
        if (!(event.getEntity().getKiller() instanceof Player player)) {
            return;
        }

        String entityType = event.getEntity().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo KILL_ENTITY
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.KILL_ENTITY);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.KILL_ENTITY) {
                    String requiredEntity = objective.getParameters().get("entity_type");

                    if (entityType.equals(requiredEntity)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§cEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }/**
     * Maneja el evento cuando un jugador craftea un item.*
     * IMPORTANTE: Detecta crafting en mesa de crafteo Y en inventario 2x2
     */
    @EventHandler
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        org.bukkit.inventory.ItemStack craftedItem = event.getRecipe().getResult();
        String itemType = craftedItem.getType().getKey().toString();

        int amount = event.isShiftClick() ?
                calculateMaxCraftAmount(event.getInventory(), event.getRecipe()) :
                craftedItem.getAmount();

        // ✅ OPTIMIZADO: Solo eventos con objetivo CRAFT_ITEM
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.CRAFT_ITEM);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.CRAFT_ITEM) {
                    String requiredItem = objective.getParameters().get("item_id");

                    if (itemType.equals(requiredItem)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(amount);

                            player.sendMessage("§eEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Calcula la cantidad máxima de items que se pueden craftear con shift-click
     */
    private int calculateMaxCraftAmount(org.bukkit.inventory.CraftingInventory inventory,
                                        org.bukkit.inventory.Recipe recipe) {
        // Por defecto, asumimos 1 (implementación simple)
        // Una implementación completa debería verificar todos los ingredientes
        return 1;
    }
    /**
     * Verifica objetivos tipo REACH_LOCATION para un jugador.
     * LLAMADO POR: Task periódico cada 1 segundo
     * FLUJO:
     * 1. Obtiene la ubicación actual del jugador
     * 2. Para cada objetivo REACH_LOCATION activo, calcula distancia
     * 3. Si está dentro del radio, completa el objetivo
     */
    public void checkReachLocationObjectives(Player player) {
        plugin.getStorage().getAllEventDefinitions().values().forEach(eventDef -> {

            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventDef.getId());

            if (progressOpt.isEmpty()) {
                return;
            }

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            if (progress.getState() != EventState.IN_PROGRESS) {
                return;
            }

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.REACH_LOCATION) {

                    ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                    if (objProgress != null && !objProgress.isCompleted()) {

                        // Parámetros esperados: "x", "y", "z", "radius", "world"
                        String xStr = objective.getParameters().get("x");
                        String yStr = objective.getParameters().get("y");
                        String zStr = objective.getParameters().get("z");
                        String radiusStr = objective.getParameters().get("radius");
                        String worldName = objective.getParameters().get("world");

                        if (xStr != null && yStr != null && zStr != null && radiusStr != null) {

                            try {
                                double targetX = Double.parseDouble(xStr);
                                double targetY = Double.parseDouble(yStr);
                                double targetZ = Double.parseDouble(zStr);
                                double radius = Double.parseDouble(radiusStr);

                                // Verificar mundo si está especificado
                                if (worldName != null && !player.getWorld().getName().equals(worldName)) {
                                    return; // Jugador en mundo diferente
                                }

                                // Calcular distancia 3D
                                org.bukkit.Location playerLoc = player.getLocation();
                                double distance = Math.sqrt(
                                        Math.pow(playerLoc.getX() - targetX, 2) +
                                                Math.pow(playerLoc.getY() - targetY, 2) +
                                                Math.pow(playerLoc.getZ() - targetZ, 2)
                                );

                                // Verificar si está dentro del radio
                                if (distance <= radius) {

                                    // Completar objetivo (usa setProgress con targetAmount para marcar completo)
                                    objProgress.setProgress(objProgress.getTargetAmount());

                                    // Feedback
                                    player.sendMessage("§aEventUI: ¡Has llegado al destino!");
                                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

                                    // Notificar al cliente
                                    plugin.getEventBridge().notifyProgressUpdate(
                                            player.getUniqueId(),
                                            eventDef.getId(),
                                            objective.getId(),
                                            objProgress.getCurrentAmount(),
                                            objProgress.getTargetAmount(),
                                            objective.getDescription()
                                    );

                                    // Verificar completación del evento
                                    checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                                }

                            } catch (NumberFormatException e) {
                                LOGGER.warning("Invalid coordinates for REACH_LOCATION: " + objective.getId());
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Verifica objetivos tipo VISIT_DIMENSION para un jugador.*
     * LLAMADO POR: Task periódico cada 2 segundos*
     * FLUJO:
     * 1. Obtiene la dimensión actual del jugador
     * 2. Para cada objetivo VISIT_DIMENSION activo, compara dimensiones
     * 3. Si coincide, completa el objetivo
     */
    /**
     * Maneja el evento cuando un jugador cambia de mundo/dimensión.
     * OPTIMIZADO: Solo se dispara cuando cambia, no cada 2 segundos.
     */
    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newDimension = getDimensionName(player.getWorld().getEnvironment());

        // ✅ OPTIMIZADO: Solo eventos con objetivo VISIT_DIMENSION
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.VISIT_DIMENSION);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.VISIT_DIMENSION) {
                    String requiredDimension = objective.getParameters().get("dimension");

                    if (newDimension.equalsIgnoreCase(requiredDimension)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            objProgress.setProgress(objProgress.getTargetAmount());

                            String dimensionName = formatDimensionName(requiredDimension);
                            player.sendMessage("§aEventUI: ¡Has visitado " + dimensionName + "!");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                        }
                    }
                }
            });
        });
    }


    /**
     * Verifica objetivos tipo REACH_LEVEL para un jugador.
     * LLAMADO POR: Task periódico cada 2 segundos
     * FLUJO:
     * 1. Obtiene el nivel actual del jugador
     * 2. Para cada objetivo REACH_LEVEL activo, compara niveles
     * 3. Si alcanzó o superó el nivel, completa el objetivo
     */
    @EventHandler
    public void onPlayerLevelChange(org.bukkit.event.player.PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();

        // ✅ OPTIMIZADO: Solo eventos con objetivo REACH_LEVEL
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.REACH_LEVEL);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.REACH_LEVEL) {
                    String levelStr = objective.getParameters().get("level");

                    if (levelStr != null) {
                        try {
                            int requiredLevel = Integer.parseInt(levelStr);

                            if (newLevel >= requiredLevel) {
                                ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                                if (objProgress != null && !objProgress.isCompleted()) {
                                    objProgress.setProgress(objProgress.getTargetAmount());


                                    plugin.getEventBridge().notifyProgressUpdate(
                                            player.getUniqueId(), eventDef.getId(), objective.getId(),
                                            objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                            objective.getDescription()
                                    );

                                    checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                                }
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Invalid level for REACH_LEVEL: " + objective.getId());
                        }
                    }
                }
            });
        });
    }

    /**
     * Verifica objetivos tipo VISIT_BIOME para un jugador.*
     * LLAMADO POR: Task periódico cada 2 segundos*
     * FLUJO:
     * 1. Obtiene el bioma actual del jugador
     * 2. Para cada objetivo VISIT_BIOME activo, compara biomas
     * 3. Si coincide, completa el objetivo
     */
    public void checkVisitBiomeObjectives(Player player) {
        plugin.getStorage().getAllEventDefinitions().values().forEach(eventDef -> {

            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventDef.getId());

            if (progressOpt.isEmpty()) {
                return;
            }

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            if (progress.getState() != EventState.IN_PROGRESS) {
                return;
            }

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.VISIT_BIOME) {

                    ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                    if (objProgress != null && !objProgress.isCompleted()) {

                        // Parámetro esperado: "biome": "minecraft:desert"
                        String requiredBiome = objective.getParameters().get("biome");

                        if (requiredBiome != null) {

                            // Obtener bioma actual del jugador
                            String currentBiome = player.getLocation().getBlock().getBiome().getKey().toString();

                            if (currentBiome.equals(requiredBiome)) {

                                // Completar objetivo
                                objProgress.setProgress(objProgress.getTargetAmount());


                                // Notificar al cliente
                                plugin.getEventBridge().notifyProgressUpdate(
                                        player.getUniqueId(),
                                        eventDef.getId(),
                                        objective.getId(),
                                        objProgress.getCurrentAmount(),
                                        objProgress.getTargetAmount(),
                                        objective.getDescription()
                                );

                                // Verificar completación del evento
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Formatea el nombre de bioma para mostrar al jugador.
     */
    private String formatBiomeName(String biome) {
        // Remover namespace si existe
        String name = biome.contains(":") ? biome.split(":")[1] : biome;

        // Convertir snake_case a Title Case
        String[] words = name.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase());
        }

        return formatted.toString();
    }


    /**
     * Convierte Environment de Bukkit a nombre de dimensión.
     */
    private String getDimensionName(org.bukkit.World.Environment environment) {
        return switch (environment) {
            case NETHER -> "nether";
            case THE_END -> "the_end";
            default -> "overworld";
        };
    }

    /**
     * Formatea el nombre de dimensión para mostrar al jugador.
     */
    private String formatDimensionName(String dimension) {
        return switch (dimension.toLowerCase()) {
            case "nether" -> "§cthe Nether";
            case "the_end" -> "§dthe End";
            default -> "§athe Overworld";
        };
    }


    /**
     * Notifica al cliente (Fabric) que un evento cambió de estado.

     * LLAMADO DESDE: onBlockBreak() cuando el evento se completa

     * FLUJO DE COMUNICACIÓN:
     * [ObjectiveTracker] → [PluginEventBridge] → [PluginNetworkHandler]
     * → [Paper Plugin Messaging Channel] → [Fabric NetworkHandler]
     * → [ClientEventBridge] → [EventViewModel] → [EventScreen]
     * RESULTADO:
     * La UI del cliente actualiza el evento de "IN PROGRESS" a "COMPLETED"
     * y lo mueve a la sección correspondiente.
     *
     * @param playerId UUID del jugador
     * @param eventId ID del evento (ej. "tutorial-mining")
     * @param newState Nuevo estado (COMPLETED, FAILED, etc.)
     */
    private void notifyStateChange(UUID playerId, String eventId, EventState newState) {
        Map<String, String> payload = Map.of(
                "event_id", eventId,
                "new_state", newState.name()
        );

        BridgeMessage message = new PluginBridgeMessage(
                MessageType.EVENT_STATE_CHANGED,
                payload,
                playerId
        );

        // Enviar mensaje al cliente vía networking
        plugin.getEventBridge().sendMessage(message);

        LOGGER.info("Notified state change: " + eventId + " → " + newState);
    }
    /**
     * Verifica objetivos tipo COLLECT_ITEM para un jugador.
     * LLAMADO POR: Task periódico cada 2 segundos
     * FLUJO:
     * 1. Escanea el inventario del jugador
     * 2. Para cada objetivo COLLECT_ITEM activo, cuenta items
     * 3. Actualiza el progreso si cambió
     * 4. Verifica completación
     */
    public void checkCollectObjectives(Player player) {
        // ✅ OPTIMIZADO: Solo eventos activos con objetivo COLLECT_ITEM
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.COLLECT_ITEM);

        if (relevantEvents.isEmpty()) {
            return; // No hay eventos con COLLECT_ITEM activos
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.COLLECT_ITEM) {
                    String requiredItem = objective.getParameters().get("item_id");

                    if (requiredItem != null) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            int currentAmount = countItemsInInventory(player, requiredItem);
                            int previousAmount = objProgress.getCurrentAmount();
                            boolean wasCompleted = objProgress.isCompleted();

                            objProgress.setProgress(currentAmount);
                            boolean isNowCompleted = objProgress.isCompleted();

                            if (currentAmount != previousAmount) {
                                plugin.getEventBridge().notifyProgressUpdate(
                                        player.getUniqueId(), eventDef.getId(), objective.getId(),
                                        objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                        objective.getDescription()
                                );
                            }

                            if (!wasCompleted && isNowCompleted) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }
    /**
     * Maneja el evento cuando un jugador interactúa con un bloque.*
     * EJEMPLOS:
     * - Click derecho en crafting table
     * - Click derecho en furnace
     * - Click derecho en chest
     */
    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        String blockType = event.getClickedBlock().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo INTERACT
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.INTERACT);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.INTERACT) {
                    String targetType = objective.getParameters().get("target_type");
                    String targetId = objective.getParameters().get("target_id");

                    if ("block".equals(targetType) && blockType.equals(targetId)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§bEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador interactúa con una entidad.
     * EJEMPLOS:
     * - Click derecho en aldeano (trading)
     * - Click derecho en vaca (ordeñar)
     * - Click derecho en sheep (esquilar)
     */
    @EventHandler
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        String entityType = event.getRightClicked().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo INTERACT
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.INTERACT);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.INTERACT) {
                    String targetType = objective.getParameters().get("target_type");
                    String targetId = objective.getParameters().get("target_id");

                    if ("entity".equals(targetType) && entityType.equals(targetId)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§bEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador domestica una entidad.*
     * FLUJO:
     * 1. Detecta cuando se domestica un animal (lobo, gato, loro, caballo)
     * 2. Busca eventos IN_PROGRESS con objetivos TAME_ENTITY
     * 3. Si la entidad coincide, incrementa progreso
     */
    @EventHandler
    public void onEntityTame(org.bukkit.event.entity.EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }

        String entityType = event.getEntity().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo TAME_ENTITY
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.TAME_ENTITY);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.TAME_ENTITY) {
                    String requiredEntity = objective.getParameters().get("entity_type");

                    if (entityType.equals(requiredEntity)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§6EventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador reproduce animales.*
     * FLUJO:
     * 1. Detecta cuando dos animales se reproducen
     * 2. Verifica que el jugador fue quien inició la reproducción
     * 3. Busca eventos IN_PROGRESS con objetivos BREED_ENTITY
     * 4. Si la entidad coincide, incrementa progreso
     */
    @EventHandler
    public void onEntityBreed(org.bukkit.event.entity.EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) {
            return;
        }

        String entityType = event.getEntity().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo BREED_ENTITY
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.BREED_ENTITY);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.BREED_ENTITY) {
                    String requiredEntity = objective.getParameters().get("entity_type");

                    if (entityType.equals(requiredEntity)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§dEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador obtiene un item fundido de un horno.*
     * FLUJO:
     * 1. Detecta cuando se extrae un item fundido (FurnaceExtractEvent)
     * 2. Busca eventos IN_PROGRESS con objetivos SMELT_ITEM
     * 3. Si el item coincide, incrementa progreso por la cantidad extraída
     */
    @EventHandler
    public void onFurnaceExtract(org.bukkit.event.inventory.FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItemType().getKey().toString();
        int amount = event.getItemAmount();

        // ✅ OPTIMIZADO: Solo eventos con objetivo SMELT_ITEM
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.SMELT_ITEM);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.SMELT_ITEM) {
                    String requiredItem = objective.getParameters().get("item_id");

                    if (itemType.equals(requiredItem)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(amount);

                            player.sendMessage("§6EventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador consume un item (come/bebe).*
     * FLUJO:
     * 1. Detecta cuando se consume un item (PlayerItemConsumeEvent)
     * 2. Busca eventos IN_PROGRESS con objetivos CONSUME_ITEM
     * 3. Si el item coincide, incrementa progreso
     */
    @EventHandler
    public void onItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItem().getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo CONSUME_ITEM
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.CONSUME_ITEM);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.CONSUME_ITEM) {
                    String requiredItem = objective.getParameters().get("item_id");

                    if (itemType.equals(requiredItem)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§aEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador prepara una poción.*
     * FLUJO:
     * 1. Detecta cuando se completa el preparado de una poción (BrewEvent)
     * 2. ESPERA 1 tick para que las pociones se transformen
     * 3. Verifica el tipo de poción resultante
     * 4. Busca eventos IN_PROGRESS con objetivos BREW_POTION
     * 5. Si la poción coincide, incrementa progreso
     */
    @EventHandler
    public void onPotionBrew(org.bukkit.event.inventory.BrewEvent event) {
        org.bukkit.block.Block block = event.getBlock();

        Player player = null;
        double closestDistance = 10.0;

        for (org.bukkit.entity.Entity entity : block.getWorld().getNearbyEntities(
                block.getLocation(), closestDistance, closestDistance, closestDistance)) {
            if (entity instanceof Player) {
                player = (Player) entity;
                break;
            }
        }

        if (player == null) {
            return;
        }

        // ✅ OPTIMIZADO: Solo eventos con objetivo BREW_POTION
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.BREW_POTION);

        if (relevantEvents.isEmpty()) {
            return;
        }

        Player finalPlayer = player;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!(block.getState() instanceof org.bukkit.block.BrewingStand brewingStand)) {
                return;
            }

            org.bukkit.inventory.BrewerInventory inventory = brewingStand.getInventory();

            for (int slot = 0; slot < 3; slot++) {
                org.bukkit.inventory.ItemStack item = inventory.getItem(slot);

                if (item != null && item.getType() == org.bukkit.Material.POTION) {
                    org.bukkit.inventory.meta.PotionMeta meta =
                            (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();

                    if (meta != null && meta.getBasePotionType() != null) {
                        String potionType = meta.getBasePotionType().name().toLowerCase();

                        relevantEvents.forEach(eventId -> {
                            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
                            if (eventDefOpt.isEmpty()) return;

                            var eventDef = eventDefOpt.get();
                            var progressOpt = plugin.getStorage().getProgress(finalPlayer.getUniqueId(), eventId);
                            if (progressOpt.isEmpty()) return;

                            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

                            eventDef.getObjectives().forEach(objective -> {
                                if (objective.getType() == ObjectiveType.BREW_POTION) {
                                    String requiredPotion = objective.getParameters().get("potion_type");

                                    if (potionType.equalsIgnoreCase(requiredPotion)) {
                                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                                        if (objProgress != null && !objProgress.isCompleted()) {
                                            boolean completed = objProgress.increment(1);

                                            finalPlayer.sendMessage("§dEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                                            plugin.getEventBridge().notifyProgressUpdate(
                                                    finalPlayer.getUniqueId(), eventDef.getId(), objective.getId(),
                                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                                    objective.getDescription()
                                            );

                                            if (completed) {
                                                finalPlayer.sendMessage("§6EventUI: ¡Objetivo completado!");
                                                checkEventCompletion(finalPlayer, (EventDefinitionImpl) eventDef, progress);
                                            }
                                        }
                                    }
                                }
                            });
                        });
                    }
                }
            }
        }, 1L);
    }

    /**
     * Maneja el evento cuando un jugador hace daño a una entidad.
     * FLUJO:
     * 1. Detecta cuando un jugador ataca una entidad (EntityDamageByEntityEvent)
     * 2. Busca eventos IN_PROGRESS con objetivos DAMAGE_ENTITY
     * 3. Si la entidad coincide, incrementa progreso según el daño
     */
    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        String entityType = event.getEntity().getType().getKey().toString();
        double damage = event.getFinalDamage();

        // ✅ OPTIMIZADO: Solo eventos con objetivo DAMAGE_ENTITY
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.DAMAGE_ENTITY);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.DAMAGE_ENTITY) {
                    String requiredEntity = objective.getParameters().get("entity_type");

                    if (entityType.equals(requiredEntity)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            int damageAmount = (int) Math.ceil(damage);
                            boolean completed = objProgress.increment(damageAmount);

                            player.sendMessage("§cEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador encanta un item.*
     * FLUJO:
     * 1. Detecta cuando se encanta un item (EnchantItemEvent)
     * 2. Busca eventos IN_PROGRESS con objetivos ENCHANT_ITEM
     * 3. Incrementa progreso por cada encantamiento
     */
    @EventHandler
    public void onEnchantItem(org.bukkit.event.enchantment.EnchantItemEvent event) {
        Player player = event.getEnchanter();
        org.bukkit.inventory.ItemStack enchantedItem = event.getItem();
        String itemType = enchantedItem.getType().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo ENCHANT_ITEM
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.ENCHANT_ITEM);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.ENCHANT_ITEM) {
                    ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());
                    String requiredItem = objective.getParameters().get("item_type");

                    if (requiredItem == null || itemType.equals(requiredItem)) {
                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§5EventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Maneja el evento cuando un jugador consigue un logro.*
     * FLUJO:
     * 1. Detecta cuando se desbloquea un advancement (PlayerAdvancementDoneEvent)
     * 2. Busca eventos IN_PROGRESS con objetivos UNLOCK_ADVANCEMENT
     * 3. Si el advancement coincide, completa el objetivo
     */
    @EventHandler
    public void onAdvancementDone(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().toString();

        // ✅ OPTIMIZADO: Solo eventos con objetivo UNLOCK_ADVANCEMENT
        Set<String> relevantEvents = getRelevantActiveEvents(player.getUniqueId(), ObjectiveType.UNLOCK_ADVANCEMENT);

        if (relevantEvents.isEmpty()) {
            return;
        }

        relevantEvents.forEach(eventId -> {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) return;

            var eventDef = eventDefOpt.get();
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
            if (progressOpt.isEmpty()) return;

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.UNLOCK_ADVANCEMENT) {
                    String requiredAdvancement = objective.getParameters().get("advancement_id");

                    if (advancementKey.equals(requiredAdvancement)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            objProgress.setProgress(objProgress.getTargetAmount());

                            player.sendMessage("§6EventUI: ¡Logro desbloqueado!");

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(), eventDef.getId(), objective.getId(),
                                    objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                        }
                    }
                }
            });
        });
    }


    /**
     * Maneja el evento cuando un jugador se mueve entre chunks.*
     * FLUJO:
     * 1. Detecta cuando el jugador cambia de chunk
     * 2. Verifica si hay estructuras en ese chunk
     * 3. Si coincide con un objetivo VISIT_STRUCTURE, completa el objetivo*
     * VENTAJAS:
     * - No requiere búsqueda costosa
     * - Detección instantánea al entrar
     * - Sin radius configuration
     */
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        // Solo verificar cuando cambia de chunk (optimización)
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        Player player = event.getPlayer();
        org.bukkit.Chunk chunk = event.getTo().getChunk();

        // ========== VISIT_STRUCTURE ==========
        java.util.Collection<org.bukkit.generator.structure.GeneratedStructure> structures =
                chunk.getStructures();

        if (!structures.isEmpty()) {
            Set<String> relevantStructureEvents = getRelevantActiveEvents(
                    player.getUniqueId(), ObjectiveType.VISIT_STRUCTURE);

            if (!relevantStructureEvents.isEmpty()) {
                relevantStructureEvents.forEach(eventId -> {
                    var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
                    if (eventDefOpt.isEmpty()) return;

                    var eventDef = eventDefOpt.get();
                    var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                    if (progressOpt.isEmpty()) return;

                    EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

                    eventDef.getObjectives().forEach(objective -> {
                        if (objective.getType() == ObjectiveType.VISIT_STRUCTURE) {
                            ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                            if (objProgress != null && !objProgress.isCompleted()) {
                                String requiredStructureTag = objective.getParameters().get("structure_tag");

                                if (requiredStructureTag != null) {
                                    org.bukkit.NamespacedKey requiredKey =
                                            org.bukkit.NamespacedKey.fromString(requiredStructureTag);

                                    if (requiredKey != null) {
                                        for (org.bukkit.generator.structure.GeneratedStructure genStructure : structures) {
                                            org.bukkit.generator.structure.Structure structure = genStructure.getStructure();
                                            org.bukkit.NamespacedKey structureKey = structure.getKey();

                                            if (structureKey.equals(requiredKey)) {
                                                objProgress.setProgress(objProgress.getTargetAmount());


                                                plugin.getEventBridge().notifyProgressUpdate(
                                                        player.getUniqueId(), eventDef.getId(), objective.getId(),
                                                        objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                                        objective.getDescription()
                                                );

                                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                });
            }
        }

        // ========== VISIT_BIOME ==========
        String currentBiome = player.getLocation().getBlock().getBiome().getKey().toString();

        Set<String> relevantBiomeEvents = getRelevantActiveEvents(
                player.getUniqueId(), ObjectiveType.VISIT_BIOME);

        if (!relevantBiomeEvents.isEmpty()) {
            relevantBiomeEvents.forEach(eventId -> {
                var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
                if (eventDefOpt.isEmpty()) return;

                var eventDef = eventDefOpt.get();
                var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                if (progressOpt.isEmpty()) return;

                EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

                eventDef.getObjectives().forEach(objective -> {
                    if (objective.getType() == ObjectiveType.VISIT_BIOME) {
                        String requiredBiome = objective.getParameters().get("biome");

                        if (currentBiome.equals(requiredBiome)) {
                            ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                            if (objProgress != null && !objProgress.isCompleted()) {
                                objProgress.setProgress(objProgress.getTargetAmount());



                                plugin.getEventBridge().notifyProgressUpdate(
                                        player.getUniqueId(), eventDef.getId(), objective.getId(),
                                        objProgress.getCurrentAmount(), objProgress.getTargetAmount(),
                                        objective.getDescription()
                                );

                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                });
            });
        }
    }






    /**
     * Método público para que otros sistemas puedan incrementar progreso de objetivos CUSTOM.
     * EJEMPLO DE USO:
     * EventUIPlugin.getInstance().getObjectiveTracker()
     *     .triggerCustomObjective(player, "minigame-parkour", 1);
     *
     * @param player Jugador que completó la acción
     * @param customObjectiveId ID del objetivo custom (ej: "complete-parkour")
     * @param amount Cantidad a incrementar
     */
    public void triggerCustomObjective(Player player, String customObjectiveId, int amount) {
        plugin.getStorage().getAllEventDefinitions().values().forEach(eventDef -> {

            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventDef.getId());

            if (progressOpt.isEmpty()) {
                return;
            }

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            if (progress.getState() != EventState.IN_PROGRESS) {
                return;
            }

            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.CUSTOM) {

                    // Parámetro esperado: "custom_id": identificador único
                    String objectiveCustomId = objective.getParameters().get("custom_id");

                    if (customObjectiveId.equals(objectiveCustomId)) {

                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {

                            boolean completed = objProgress.increment(amount);

                            player.sendMessage("§dEventUI: Progreso " + objProgress.getCurrentAmount() + "/" +
                                    objProgress.getTargetAmount() + " - " + objective.getDescription());

                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(),
                                    eventDef.getId(),
                                    objective.getId(),
                                    objProgress.getCurrentAmount(),
                                    objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6EventUI: ¡Objetivo completado!");
                                checkEventCompletion(player, (EventDefinitionImpl) eventDef, progress);
                            }
                        }
                    }
                }
            });
        });
    }


    /**
     * Cuenta cuántos items de un tipo específico tiene el jugador en su inventario.
     */
    private int countItemsInInventory(Player player, String itemId) {
        int total = 0;

        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().getKey().toString().equals(itemId)) {
                total += item.getAmount();
            }
        }

        return total;
    }
    /**
     * Verifica si todos los objetivos de un evento están completados.
     * Si sí, completa el evento y entrega recompensas.
     */
    // UBICACIÓN: ObjectiveTracker.java (línea ~100, aprox)
    private void checkEventCompletion(Player player, EventDefinitionImpl eventDef, EventProgressImpl progress) {

        if (progress.areAllObjectivesCompleted()) {
            progress.complete();

            // ✅ NUEVO: Desregistrar evento activo
            unregisterActiveEvent(player.getUniqueId(), eventDef.getId());

            player.sendMessage("§6§l✔ EVENT COMPLETED: " + eventDef.getDisplayName());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            // Otorgar rewards
            plugin.getRewardManager().giveRewards(player, eventDef);

            // Notificar al cliente
            plugin.getEventBridge().notifyStateChange(
                    player.getUniqueId(),
                    eventDef.getId(),
                    EventState.COMPLETED
            );
        }
    }
    /**
     * Construye un índice de qué eventos contienen qué tipos de objetivos.
     * Esto permite filtrar rápidamente qué eventos revisar según el evento de Bukkit.
     */
    public void buildObjectiveTypeIndex() {
        eventsByObjectiveType.clear();

        plugin.getStorage().getAllEventDefinitions().values().forEach(eventDef -> {
            eventDef.getObjectives().forEach(objective -> {
                eventsByObjectiveType
                        .computeIfAbsent(objective.getType(), k -> ConcurrentHashMap.newKeySet())
                        .add(eventDef.getId());
            });
        });

        LOGGER.info("Built objective type index: " + eventsByObjectiveType.size() + " types indexed");
    }

    /**
     * Registra que un jugador tiene un evento activo.
     * Llamar cuando un evento pasa a IN_PROGRESS.
     */
    public void registerActiveEvent(UUID playerId, String eventId) {
        activeEventsByPlayer
                .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(eventId);
    }
    /**
     * Inicializa el índice de eventos activos cargando todos los progresos IN_PROGRESS.
     * Llamar al cargar el plugin.
     */
    public void initializeActiveEventsIndex() {
        activeEventsByPlayer.clear();

        // Obtener todos los progresos guardados
        Map<UUID, Map<String, EventProgressImpl>> allProgress = plugin.getStorage().getAllProgress();

        // Iterar sobre todos los jugadores
        allProgress.forEach((playerId, progressMap) -> {
            // Iterar sobre todos los eventos del jugador
            progressMap.forEach((eventId, progress) -> {
                // Solo registrar si está IN_PROGRESS
                if (progress.getState() == EventState.IN_PROGRESS) {
                    registerActiveEvent(playerId, eventId);
                }
            });
        });

        LOGGER.info("Initialized active events index: " + activeEventsByPlayer.size() + " players with active events");
    }

    /**
     * Elimina un evento activo del jugador.
     * Llamar cuando un evento pasa a COMPLETED o LOCKED.
     */
    public void unregisterActiveEvent(UUID playerId, String eventId) {
        Set<String> activeEvents = activeEventsByPlayer.get(playerId);
        if (activeEvents != null) {
            activeEvents.remove(eventId);
            if (activeEvents.isEmpty()) {
                activeEventsByPlayer.remove(playerId);
            }
        }
    }

    /**
     * Obtiene los IDs de eventos activos para un jugador que contienen un tipo de objetivo específico.
     */
    public Set<String> getRelevantActiveEvents(UUID playerId, ObjectiveType objectiveType) {
        Set<String> playerActiveEvents = activeEventsByPlayer.get(playerId);
        if (playerActiveEvents == null || playerActiveEvents.isEmpty()) {
            return Set.of(); // No hay eventos activos
        }

        Set<String> eventsWithObjectiveType = eventsByObjectiveType.get(objectiveType);
        if (eventsWithObjectiveType == null || eventsWithObjectiveType.isEmpty()) {
            return Set.of(); // No hay eventos con este tipo de objetivo
        }

        // Intersección: eventos activos del jugador que contienen este tipo de objetivo
        Set<String> relevant = new HashSet<>(playerActiveEvents);
        relevant.retainAll(eventsWithObjectiveType);
        return relevant;
    }

    /**
     * Reconstruye el índice (llamar cuando se recarguen eventos).
     */
    public void rebuildIndex() {
        buildObjectiveTypeIndex();
    }

}
