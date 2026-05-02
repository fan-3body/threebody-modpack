package org.fan_3body.dehydration.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.fan_3body.dehydration.dehydration.DehydrationStateMachine;

import java.util.function.Supplier;

public final class DehydratePacket {
    public static final DehydratePacket INSTANCE = new DehydratePacket();

    private DehydratePacket() {
    }

    public static void encode(DehydratePacket packet, FriendlyByteBuf buffer) {
    }

    public static DehydratePacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(DehydratePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null && !sender.isSpectator()) {
                DehydrationStateMachine.requestEnter(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
