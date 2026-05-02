package org.fan_3body.dehydration.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.fan_3body.dehydration.Dehydration;

import java.util.List;

@Mod.EventBusSubscriber(modid = Dehydration.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DehydrationConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEAT_STROKE_EFFECTS = BUILDER
            .comment("Mob effects that count as Legendary Survival Overhaul heat stroke.")
            .defineListAllowEmpty("heatStrokeEffects",
                    List.of("legendarysurvivaloverhaul:heat_stroke", "legendarysurvivaloverhaul:heatstroke"),
                    DehydrationConfig::isResourceLocation);

    private static final ForgeConfigSpec.IntValue MAX_DURATION_TICKS = BUILDER
            .comment("Maximum dehydration mode duration in ticks after K is pressed.")
            .defineInRange("maxDurationTicks", 20 * 90, 20, 20 * 60 * 30);

    private static final ForgeConfigSpec.IntValue THIRST_DRAIN_INTERVAL_TICKS = BUILDER
            .comment("How often dehydration mode attempts to drain LSO thirst.")
            .defineInRange("thirstDrainIntervalTicks", 20 * 5, 20, 20 * 60);

    private static final ForgeConfigSpec.DoubleValue THIRST_DRAIN_AMOUNT = BUILDER
            .comment("Amount passed to reflected LSO thirst drain calls when available.")
            .defineInRange("thirstDrainAmount", 1.0D, 0.0D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue MOVEMENT_SPEED_MULTIPLIER = BUILDER
            .comment("Movement speed multiplier while dehydrated. 0.85 means -15%.")
            .defineInRange("movementSpeedMultiplier", 0.85D, 0.05D, 2.0D);

    private static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE_MULTIPLIER = BUILDER
            .comment("Attack damage multiplier while dehydrated. 0.80 means -20%.")
            .defineInRange("attackDamageMultiplier", 0.80D, 0.05D, 2.0D);

    private static final ForgeConfigSpec.DoubleValue MAX_HEALTH_PENALTY = BUILDER
            .comment("Flat max health penalty while dehydrated.")
            .defineInRange("maxHealthPenalty", 4.0D, 0.0D, 20.0D);

    private static final ForgeConfigSpec.BooleanValue REQUIRE_HEAT_STROKE_TO_ENTER = BUILDER
            .comment("If true, the server only allows K to enter dehydration mode while heat stroke is active.")
            .define("requireHeatStrokeToEnter", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static List<? extends String> heatStrokeEffects = List.of("legendarysurvivaloverhaul:heat_stroke", "legendarysurvivaloverhaul:heatstroke");
    public static int maxDurationTicks = 20 * 90;
    public static int thirstDrainIntervalTicks = 20 * 5;
    public static double thirstDrainAmount = 1.0D;
    public static double movementSpeedMultiplier = 0.85D;
    public static double attackDamageMultiplier = 0.80D;
    public static double maxHealthPenalty = 4.0D;
    public static boolean requireHeatStrokeToEnter = true;

    private DehydrationConfig() {
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String text && ResourceLocation.isValidResourceLocation(text);
    }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent event) {
        heatStrokeEffects = HEAT_STROKE_EFFECTS.get();
        maxDurationTicks = MAX_DURATION_TICKS.get();
        thirstDrainIntervalTicks = THIRST_DRAIN_INTERVAL_TICKS.get();
        thirstDrainAmount = THIRST_DRAIN_AMOUNT.get();
        movementSpeedMultiplier = MOVEMENT_SPEED_MULTIPLIER.get();
        attackDamageMultiplier = ATTACK_DAMAGE_MULTIPLIER.get();
        maxHealthPenalty = MAX_HEALTH_PENALTY.get();
        requireHeatStrokeToEnter = REQUIRE_HEAT_STROKE_TO_ENTER.get();
    }
}
