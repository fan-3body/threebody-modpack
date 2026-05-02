package org.fan_3body.dehydration.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.fan_3body.dehydration.Dehydration;

import javax.annotation.Nullable;

public final class DehydrationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<DehydrationCap> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation ID = new ResourceLocation(Dehydration.MODID, "dehydration");

    private final DehydrationCap cap = new DehydrationCap();
    private final LazyOptional<DehydrationCap> optional = LazyOptional.of(() -> cap);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return cap.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        cap.deserializeNBT(tag);
    }
}
