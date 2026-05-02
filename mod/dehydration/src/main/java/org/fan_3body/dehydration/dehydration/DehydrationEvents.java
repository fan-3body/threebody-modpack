package org.fan_3body.dehydration.dehydration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.fan_3body.dehydration.Dehydration;
import org.fan_3body.dehydration.network.DehydrationNetwork;

@Mod.EventBusSubscriber(modid = Dehydration.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DehydrationEvents {
    private DehydrationEvents() {
    }

    @SubscribeEvent
    public static void attachPlayerCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            org.fan_3body.dehydration.capability.DehydrationProvider provider = new org.fan_3body.dehydration.capability.DehydrationProvider();
            event.addCapability(org.fan_3body.dehydration.capability.DehydrationProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(org.fan_3body.dehydration.capability.DehydrationProvider.CAPABILITY).ifPresent(oldState ->
                event.getEntity().getCapability(org.fan_3body.dehydration.capability.DehydrationProvider.CAPABILITY).ifPresent(newState ->
                        newState.deserializeNBT(oldState.serializeNBT())));
        event.getOriginal().invalidateCaps();
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DehydrationNetwork.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DehydrationStateMachine.forceExit(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DehydrationNetwork.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DehydrationNetwork.syncToClient(serverPlayer);
        }
    }
}
