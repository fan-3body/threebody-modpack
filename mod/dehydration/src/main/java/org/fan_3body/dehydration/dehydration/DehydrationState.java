package org.fan_3body.dehydration.dehydration;

import net.minecraft.nbt.CompoundTag;

public final class DehydrationState implements IDehydrationState {
    private boolean active;
    private int ticksActive;
    private int remainingTicks;

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public int getTicksActive() {
        return ticksActive;
    }

    @Override
    public int getRemainingTicks() {
        return remainingTicks;
    }

    @Override
    public void enter(int durationTicks) {
        active = true;
        ticksActive = 0;
        remainingTicks = Math.max(1, durationTicks);
    }

    @Override
    public void tick() {
        if (!active) {
            return;
        }
        ticksActive++;
        remainingTicks--;
        if (remainingTicks <= 0) {
            exit();
        }
    }

    @Override
    public void exit() {
        active = false;
        ticksActive = 0;
        remainingTicks = 0;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", active);
        tag.putInt("TicksActive", ticksActive);
        tag.putInt("RemainingTicks", remainingTicks);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        active = tag.getBoolean("Active");
        ticksActive = Math.max(0, tag.getInt("TicksActive"));
        remainingTicks = Math.max(0, tag.getInt("RemainingTicks"));
        if (remainingTicks <= 0) {
            active = false;
            ticksActive = 0;
        }
    }
}
