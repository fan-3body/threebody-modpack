package org.fan_3body.dehydration.dehydration;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.fan_3body.dehydration.config.DehydrationConfig;

import java.util.UUID;

public final class DehydrationAttributeModifiers {
    private static final UUID MOVEMENT_SPEED_ID = UUID.fromString("6d0ddfd7-7f2c-47b8-a8f7-6d0fe0346f71");
    private static final UUID ATTACK_DAMAGE_ID = UUID.fromString("ce4d6cf9-cfb1-4e84-80d2-f024c73016e4");
    private static final UUID MAX_HEALTH_ID = UUID.fromString("03c77a67-1b0d-4825-8d16-ef6ed1d5f6b0");

    private DehydrationAttributeModifiers() {
    }

    public static void apply(Player player) {
        applyMultiplier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_ID,
                "Dehydration movement speed penalty", DehydrationConfig.movementSpeedMultiplier - 1.0D);
        applyMultiplier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID,
                "Dehydration attack damage penalty", DehydrationConfig.attackDamageMultiplier - 1.0D);
        applyFlat(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID,
                "Dehydration max health penalty", -DehydrationConfig.maxHealthPenalty);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void remove(Player player) {
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_SPEED_ID);
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_ID);
        removeModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_ID);
    }

    private static void applyMultiplier(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null || instance.getModifier(id) != null || amount == 0.0D) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void applyFlat(AttributeInstance instance, UUID id, String name, double amount) {
        if (instance == null || instance.getModifier(id) != null || amount == 0.0D) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void removeModifier(AttributeInstance instance, UUID id) {
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }
}
