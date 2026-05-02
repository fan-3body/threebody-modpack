package org.fan_3body.dehydration.dehydration;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IDehydrationState {
    boolean isActive();

    int getTicksActive();

    int getRemainingTicks();

    void enter(int durationTicks);

    void tick();

    void exit();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
