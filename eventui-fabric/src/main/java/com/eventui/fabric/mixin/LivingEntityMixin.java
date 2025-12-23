package com.eventui.fabric.mixin;

import com.eventui.common.contract.signal.GameSignal;
import com.eventui.fabric.EventUIFabricMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Verificar si fue asesinado por un jugador
        if (damageSource.getEntity() instanceof Player player) {
            // Obtener tipo de entidad
            String entityType = BuiltInRegistries.ENTITY_TYPE
                    .getKey(self.getType())
                    .toString();

            // Obtener dimensión
            ResourceKey<Level> dimension = self.level().dimension();
            String dimensionName = dimension.location().toString();

            // Crear señal
            GameSignal signal = new GameSignal.EntityKilled(
                    player.getUUID(),
                    entityType,
                    dimensionName
            );

            // Emitir al Core
            try {
                EventUIFabricMod.getCore().getSignalBus().emit(signal);

                EventUIFabricMod.LOGGER.debug(
                        "Entity killed signal: {} killed {} in {}",
                        player.getName().getString(),
                        entityType,
                        dimensionName
                );
            } catch (Exception e) {
                EventUIFabricMod.LOGGER.error("Failed to emit entity killed signal", e);
            }
        }
    }
}
