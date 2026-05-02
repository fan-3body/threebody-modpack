package org.fan_3body.dehydration.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.fan_3body.dehydration.client.ClientDehydrationSync;

import java.util.function.Supplier;

public final class SyncDehydrationPacket {
    private final boolean dehydrated;

    public SyncDehydrationPacket(boolean dehydrated) {
        this.dehydrated = dehydrated;
    }

    public boolean isDehydrated() {
        return dehydrated;
    }

    public static void encode(SyncDehydrationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.dehydrated);
    }

    public static SyncDehydrationPacket decode(FriendlyByteBuf buffer) {
        return new SyncDehydrationPacket(buffer.readBoolean());
    }

    public static void handle(SyncDehydrationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientDehydrationSync.apply(packet.dehydrated)));
        context.setPacketHandled(true);
    }
}
