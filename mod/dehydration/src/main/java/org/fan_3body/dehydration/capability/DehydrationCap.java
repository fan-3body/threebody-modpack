package org.fan_3body.dehydration.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public final class DehydrationCap {
    private boolean dehydrated;

    public boolean isDehydrated() {
        return dehydrated;
    }

    public void setDehydrated(boolean dehydrated) {
        this.dehydrated = dehydrated;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsDehydrated", dehydrated);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        dehydrated = tag != null && tag.getBoolean("IsDehydrated");
    }
}
