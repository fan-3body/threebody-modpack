package org.fan_3body.dehydration.dehydration;

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
    public static final Capability<IDehydrationState> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation ID = new ResourceLocation(Dehydration.MODID, "dehydration_state");

    private final DehydrationState state = new DehydrationState();
    private final LazyOptional<IDehydrationState> optional = LazyOptional.of(() -> state);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return state.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        state.deserializeNBT(tag);
    }
}
