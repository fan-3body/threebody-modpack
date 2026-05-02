package org.fan_3body.dehydration.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.fan_3body.dehydration.capability.DehydrationProvider;

public final class ClientDehydrationSync {
    private ClientDehydrationSync() {
    }

    public static void apply(boolean dehydrated) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.getCapability(DehydrationProvider.CAPABILITY).ifPresent(cap -> cap.setDehydrated(dehydrated));
    }
}
