package org.fan_3body.dehydration.dehydration;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.fan_3body.dehydration.config.DehydrationConfig;
import org.fan_3body.dehydration.integration.LsoIntegration;
import org.fan_3body.dehydration.network.DehydrationNetwork;

public final class DehydrationStateMachine {
    private static final String TICKS_ACTIVE_KEY = "DehydrationTicksActive";
    private static final String REMAINING_TICKS_KEY = "DehydrationRemainingTicks";

    private DehydrationStateMachine() {
    }

    public static void requestEnter(ServerPlayer player) {
        player.getCapability(org.fan_3body.dehydration.capability.DehydrationProvider.CAPABILITY).ifPresent(state -> {
            if (state.isDehydrated()) {
                exit(player, state, true);
                return;
            }
            if (DehydrationConfig.requireHeatStrokeToEnter && !LsoIntegration.hasHeatStroke(player)) {
                player.displayClientMessage(Component.translatable("message.dehydration.requires_heat_stroke"), true);
                return;
            }
            enter(player, state);
        });
    }

    public static void tick(ServerPlayer player) {
        player.getCapability(org.fan_3body.dehydration.capability.DehydrationProvider.CAPABILITY).ifPresent(state -> {
            if (!state.isDehydrated()) {
                DehydrationAttributeModifiers.remove(player);
                return;
            }

            int ticksActive = player.getPersistentData().getInt(TICKS_ACTIVE_KEY);
            int remainingTicks = player.getPersistentData().getInt(REMAINING_TICKS_KEY);

            ticksActive++;
            remainingTicks--;
            player.getPersistentData().putInt(TICKS_ACTIVE_KEY, ticksActive);
            player.getPersistentData().putInt(REMAINING_TICKS_KEY, remainingTicks);

            if (remainingTicks <= 0) {
                exit(player, state, false);
            }
        });
    }

    public static void forceExit(ServerPlayer player) {
        player.getCapability(org.fan_3body.dehydration.capability.DehydrationProvider.CAPABILITY).ifPresent(state -> exit(player, state, false));
    }

    public static int getTicksActive(ServerPlayer player) {
        return player.getPersistentData().getInt(TICKS_ACTIVE_KEY);
    }

    private static void enter(ServerPlayer player, org.fan_3body.dehydration.capability.DehydrationCap state) {
        state.setDehydrated(true);
        player.getPersistentData().putInt(TICKS_ACTIVE_KEY, 0);
        player.getPersistentData().putInt(REMAINING_TICKS_KEY, DehydrationConfig.maxDurationTicks);
        LsoIntegration.clearThirst(player);
        DehydrationNetwork.syncToClient(player);
        player.displayClientMessage(Component.translatable("message.dehydration.enter"), true);
    }

    private static void exit(ServerPlayer player, org.fan_3body.dehydration.capability.DehydrationCap state, boolean toggledByPlayer) {
        state.setDehydrated(false);
        player.getPersistentData().remove(TICKS_ACTIVE_KEY);
        player.getPersistentData().remove(REMAINING_TICKS_KEY);
        DehydrationAttributeModifiers.remove(player);
        DehydrationNetwork.syncToClient(player);
        if (toggledByPlayer) {
            player.displayClientMessage(Component.translatable("message.dehydration.exit"), true);
        }
    }
}
