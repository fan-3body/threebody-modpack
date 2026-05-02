package org.fan_3body.dehydration.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.fan_3body.dehydration.Dehydration;

public final class LSOHelper {
    private static final String LSO_MODID = "legendarysurvivaloverhaul";
    private static final ResourceLocation HEAT_STROKE_ID = new ResourceLocation(LSO_MODID, "heat_stroke");

    private static boolean initialized;
    private static boolean lsoLoaded;
    private static MobEffect heatStrokeEffect;

    private LSOHelper() {
    }

    public static void init() {
        initialized = true;
        lsoLoaded = ModList.get().isLoaded(LSO_MODID);
        heatStrokeEffect = null;

        if (!lsoLoaded) {
            Dehydration.LOGGER.debug("Legendary Survival Overhaul is not installed; heat stroke checks are disabled.");
            return;
        }

        if (ForgeRegistries.MOB_EFFECTS == null) {
            Dehydration.LOGGER.debug("Mob effect registry is unavailable; heat stroke checks are disabled.");
            return;
        }

        heatStrokeEffect = ForgeRegistries.MOB_EFFECTS.getValue(HEAT_STROKE_ID);
        if (heatStrokeEffect == null) {
            Dehydration.LOGGER.warn("LSO is installed, but effect {} was not found.", HEAT_STROKE_ID);
        }
    }

    public static boolean hasHeatStroke(Player player) {
        if (player == null) {
            return false;
        }
        if (!initialized) {
            init();
        }
        if (!lsoLoaded || heatStrokeEffect == null) {
            return false;
        }
        return player.hasEffect(heatStrokeEffect);
    }
}
