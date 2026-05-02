package org.fan_3body.dehydration.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.fan_3body.dehydration.Dehydration;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Dehydration.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {
    private static final String CATEGORY = "key.categories.dehydration";

    public static final KeyMapping DEHYDRATE_KEY = new KeyMapping(
            "key.dehydration.trigger",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DEHYDRATE_KEY);
    }
}
