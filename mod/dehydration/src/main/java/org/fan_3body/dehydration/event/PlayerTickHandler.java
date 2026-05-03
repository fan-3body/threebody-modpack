package org.fan_3body.dehydration.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.fan_3body.dehydration.Dehydration;
import org.fan_3body.dehydration.capability.DehydrationProvider;
import org.fan_3body.dehydration.dehydration.DehydrationAttributeModifiers;
import org.fan_3body.dehydration.dehydration.DehydrationStateMachine;
import org.fan_3body.dehydration.integration.LsoIntegration;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Dehydration.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerTickHandler {
    private static final int EFFECT_DURATION_TICKS = 20 * 8;
    private static final int EFFECT_REFRESH_THRESHOLD_TICKS = 20 * 3;
    private static final int THIRST_FULL_EXIT_GRACE_TICKS = 20 * 2;
    private static final double DEHYDRATED_MAX_HEALTH = 10.0D;
    private static final UUID MAX_HEALTH_CAP_ID = UUID.fromString("75314c58-8e3f-4029-a9ca-d0eafcc8cccb");
    private static final ResourceKey<DamageType> LSO_DEHYDRATION_DAMAGE = lsoDamageType("dehydration");
    private static final ResourceKey<DamageType> LSO_HYPERTHERMIA_DAMAGE = lsoDamageType("hyperthermia");

    private PlayerTickHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(DehydrationProvider.CAPABILITY).ifPresent(cap -> {
            if (!cap.isDehydrated()) {
                restoreDehydrationEffects(player);
                DehydrationAttributeModifiers.remove(player);
                return;
            }

            if (shouldExitDehydration(player)) {
                DehydrationStateMachine.forceExit(player);
                restoreDehydrationEffects(player);
                DehydrationAttributeModifiers.remove(player);
                LsoIntegration.removeTemperatureDamageImmunity(player);
                return;
            }

            applyDehydrationEffects(player);
            DehydrationStateMachine.tick(player);
            if (!cap.isDehydrated()) {
                restoreDehydrationEffects(player);
                LsoIntegration.removeTemperatureDamageImmunity(player);
            }
        });
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isLsoDehydrationDamage(event.getSource())) {
            return;
        }

        player.getCapability(DehydrationProvider.CAPABILITY).ifPresent(cap -> {
            if (cap.isDehydrated()) {
                event.setCanceled(true);
            }
        });
    }
    private static boolean shouldExitDehydration(ServerPlayer player) {
        if (!LsoIntegration.hasHeatStroke(player)) {
            return true;
        }
        return DehydrationStateMachine.getTicksActive(player) > THIRST_FULL_EXIT_GRACE_TICKS && LsoIntegration.isThirstFull(player);
    }


    private static boolean isLsoDehydrationDamage(DamageSource source) {
        return source.is(LSO_DEHYDRATION_DAMAGE) || source.is(LSO_HYPERTHERMIA_DAMAGE);
    }

    private static ResourceKey<DamageType> lsoDamageType(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(LsoIntegration.LSO_MODID, path));
    }
    private static void applyDehydrationEffects(ServerPlayer player) {
        applyEffectIfNeeded(player, MobEffects.MOVEMENT_SLOWDOWN, 0, true, true);
        applyEffectIfNeeded(player, MobEffects.DIG_SLOWDOWN, 0, true, true);
        applyEffectIfNeeded(player, MobEffects.DAMAGE_RESISTANCE, 0, false, false);
        applyMaxHealthCap(player);
        LsoIntegration.clearThirst(player);
        LsoIntegration.applyTemperatureDamageImmunity(player);
    }

    private static void applyEffectIfNeeded(ServerPlayer player, MobEffect effect, int amplifier, boolean visible, boolean showIcon) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() >= amplifier && current.getDuration() > EFFECT_REFRESH_THRESHOLD_TICKS) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier, false, visible, showIcon));
    }

    private static void applyMaxHealthCap(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        AttributeModifier current = maxHealth.getModifier(MAX_HEALTH_CAP_ID);
        double modifierAmount = DEHYDRATED_MAX_HEALTH - maxHealth.getBaseValue();
        if (current == null || Double.compare(current.getAmount(), modifierAmount) != 0) {
            if (current != null) {
                maxHealth.removeModifier(MAX_HEALTH_CAP_ID);
            }
            maxHealth.addTransientModifier(new AttributeModifier(
                    MAX_HEALTH_CAP_ID,
                    "Dehydration max health cap",
                    modifierAmount,
                    AttributeModifier.Operation.ADDITION
            ));
        }

        if (player.getHealth() > DEHYDRATED_MAX_HEALTH) {
            player.setHealth((float) DEHYDRATED_MAX_HEALTH);
        }
    }

    private static void removeDehydrationHealthCap(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(MAX_HEALTH_CAP_ID) != null) {
            maxHealth.removeModifier(MAX_HEALTH_CAP_ID);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void restoreDehydrationEffects(ServerPlayer player) {
        removeDehydrationHealthCap(player);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}
