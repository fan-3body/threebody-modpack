package org.fan_3body.dehydration.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.fan_3body.dehydration.Dehydration;
import org.fan_3body.dehydration.capability.DehydrationProvider;

public final class DehydrationNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;
    private static int packetId;

    private DehydrationNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Dehydration.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        channel.messageBuilder(DehydratePacket.class, packetId++)
                .encoder(DehydratePacket::encode)
                .decoder(DehydratePacket::decode)
                .consumerMainThread(DehydratePacket::handle)
                .add();
        channel.messageBuilder(SyncDehydrationPacket.class, packetId++)
                .encoder(SyncDehydrationPacket::encode)
                .decoder(SyncDehydrationPacket::decode)
                .consumerMainThread(SyncDehydrationPacket::handle)
                .add();
    }

    public static void sendDehydrateRequestToServer() {
        if (channel != null) {
            channel.sendToServer(DehydratePacket.INSTANCE);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (channel == null || player == null) {
            return;
        }
        player.getCapability(DehydrationProvider.CAPABILITY).ifPresent(cap ->
                channel.send(PacketDistributor.PLAYER.with(() -> player), new SyncDehydrationPacket(cap.isDehydrated())));
    }
}
