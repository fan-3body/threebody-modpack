package org.fan_3body.dehydration.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.fan_3body.dehydration.Dehydration;
import org.fan_3body.dehydration.config.DehydrationConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LsoIntegration {
    public static final String LSO_MODID = "legendarysurvivaloverhaul";
    private static final String[] THIRST_BRIDGE_CLASSES = {
            "sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil",
            "sfiomn.legendarysurvivaloverhaul.common.integration.ThirstUtil",
            "sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstCapability",
            "com.stereowalker.survive.api.thirst.ThirstUtil"
    };
    private static final String[] THIRST_DRAIN_METHODS = {
            "drainThirst",
            "addExhaustion",
            "addThirstExhaustion",
            "consumeThirst",
            "dehydrate"
    };

    private static Method cachedStaticThirstMethod;
    private static boolean searchedStaticThirstMethod;
    private static Capability<?> cachedThirstCapability;
    private static boolean searchedThirstCapability;
    private static Method cachedAddTemperatureImmunityMethod;
    private static Object[] cachedTemperatureImmunityValues;
    private static boolean searchedTemperatureImmunityMethod;
    private static final ResourceLocation TEMPERATURE_IMMUNITY_EFFECT_ID = new ResourceLocation(LSO_MODID, "temperature_immunity");

    private LsoIntegration() {
    }

    public static boolean isLsoLoaded() {
        return ModList.get().isLoaded(LSO_MODID);
    }

    public static boolean hasHeatStroke(ServerPlayer player) {
        for (MobEffectInstance instance : player.getActiveEffects()) {
            ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
            if (isConfiguredHeatStroke(effectId) || looksLikeHeatStroke(effectId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean drainThirst(ServerPlayer player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (tryStaticThirstMethod(player, amount)) {
            return true;
        }
        if (tryCapabilityReflection(player, amount)) {
            return true;
        }
        Dehydration.LOGGER.debug("No compatible LSO thirst API/capability was found for {}", player.getGameProfile().getName());
        return false;
    }

    public static boolean clearThirst(ServerPlayer player) {
        if (player == null || !isLsoLoaded()) {
            return false;
        }
        Capability<?> capability = findThirstCapability();
        if (capability == null) {
            return false;
        }

        AtomicBoolean cleared = new AtomicBoolean(false);
        LazyOptional<?> optional = player.getCapability((Capability) capability);
        optional.ifPresent(thirst -> {
            boolean hydrationSet = invokeIfPresent(thirst, "setHydrationLevel", int.class, 0);
            boolean saturationSet = invokeIfPresent(thirst, "setSaturation", float.class, 0.0F);
            boolean exhaustionSet = invokeIfPresent(thirst, "setExhaustion", float.class, 0.0F);
            invokeIfPresent(thirst, "setThirstDamageCounter", int.class, 0);
            invokeIfPresent(thirst, "setThirstDamageTickTimer", int.class, 0);
            invokeIfPresent(thirst, "setDirty");
            cleared.set(hydrationSet || saturationSet || exhaustionSet);
        });
        return cleared.get();
    }

    public static boolean isThirstFull(ServerPlayer player) {
        if (player == null || !isLsoLoaded()) {
            return false;
        }
        Capability<?> capability = findThirstCapability();
        if (capability == null) {
            return false;
        }

        AtomicBoolean full = new AtomicBoolean(false);
        LazyOptional<?> optional = player.getCapability((Capability) capability);
        optional.ifPresent(thirst -> {
            Optional<Boolean> directResult = invokeBoolean(thirst, "isHydrationLevelAtMax");
            if (directResult.isPresent()) {
                full.set(directResult.get());
                return;
            }

            Optional<Number> hydration = invokeNumber(thirst, "getHydrationLevel");
            Optional<Number> maxHydration = readStaticNumber(
                    "sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstCapability",
                    "MAX_HYDRATION"
            );
            if (hydration.isPresent() && maxHydration.isPresent()) {
                full.set(hydration.get().intValue() >= maxHydration.get().intValue());
            }
        });
        return full.get();
    }

    public static boolean applyTemperatureDamageImmunity(ServerPlayer player) {
        if (player == null || !isLsoLoaded()) {
            return false;
        }

        boolean reflected = tryAddTemperatureImmunity(player);
        MobEffect temperatureImmunity = ForgeRegistries.MOB_EFFECTS.getValue(TEMPERATURE_IMMUNITY_EFFECT_ID);
        if (temperatureImmunity != null) {
            MobEffectInstance current = player.getEffect(temperatureImmunity);
            if (current == null || current.getDuration() < 60) {
                player.addEffect(new MobEffectInstance(temperatureImmunity, 20 * 8, 0, false, false, false));
            }
            return true;
        }
        return reflected;
    }

    public static void removeTemperatureDamageImmunity(ServerPlayer player) {
        if (player == null || !isLsoLoaded()) {
            return;
        }
        MobEffect temperatureImmunity = ForgeRegistries.MOB_EFFECTS.getValue(TEMPERATURE_IMMUNITY_EFFECT_ID);
        if (temperatureImmunity != null) {
            player.removeEffect(temperatureImmunity);
        }
        tryRemoveTemperatureImmunity(player);
    }

    private static boolean isConfiguredHeatStroke(ResourceLocation effectId) {
        if (effectId == null) {
            return false;
        }
        for (String configured : DehydrationConfig.heatStrokeEffects) {
            ResourceLocation configuredId = ResourceLocation.tryParse(configured);
            if (effectId.equals(configuredId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeHeatStroke(ResourceLocation effectId) {
        if (effectId == null) {
            return false;
        }
        String namespace = effectId.getNamespace().toLowerCase(Locale.ROOT);
        String path = effectId.getPath().toLowerCase(Locale.ROOT);
        return namespace.contains("survival") && (path.equals("heat_stroke") || path.equals("heatstroke"));
    }

    private static boolean tryStaticThirstMethod(ServerPlayer player, double amount) {
        Method method = findStaticThirstMethod();
        if (method == null) {
            return false;
        }
        try {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2) {
                method.invoke(null, player, convertNumber(amount, parameterTypes[1]));
                return true;
            }
            if (parameterTypes.length == 1) {
                method.invoke(null, player);
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
            Dehydration.LOGGER.debug("Reflected LSO thirst method failed: {}", method, exception);
        }
        return false;
    }

    private static Method findStaticThirstMethod() {
        if (searchedStaticThirstMethod) {
            return cachedStaticThirstMethod;
        }
        searchedStaticThirstMethod = true;
        for (String className : THIRST_BRIDGE_CLASSES) {
            try {
                Class<?> bridge = Class.forName(className);
                for (Method method : bridge.getMethods()) {
                    if (!Modifier.isStatic(method.getModifiers()) || !isNamed(method, THIRST_DRAIN_METHODS)) {
                        continue;
                    }
                    Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length >= 1 && parameters[0].isAssignableFrom(ServerPlayer.class)
                            && (parameters.length == 1 || isNumber(parameters[1]))) {
                        cachedStaticThirstMethod = method;
                        Dehydration.LOGGER.info("Using reflected LSO thirst bridge method {}", method);
                        return cachedStaticThirstMethod;
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // Optional integration: LSO versions use different package names.
            }
        }
        return null;
    }

    private static Capability<?> findThirstCapability() {
        if (searchedThirstCapability) {
            return cachedThirstCapability;
        }
        searchedThirstCapability = true;
        try {
            Class<?> provider = Class.forName("sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstProvider");
            Field field = provider.getField("THIRST_CAPABILITY");
            Object value = field.get(null);
            if (value instanceof Capability<?> capability) {
                cachedThirstCapability = capability;
                return cachedThirstCapability;
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            Dehydration.LOGGER.debug("Could not resolve LSO thirst capability by reflection.", exception);
        }
        return null;
    }

    private static boolean tryAddTemperatureImmunity(ServerPlayer player) {
        if (!findTemperatureImmunityReflection()) {
            return false;
        }
        boolean applied = false;
        for (Object immunity : cachedTemperatureImmunityValues) {
            try {
                cachedAddTemperatureImmunityMethod.invoke(null, player, immunity);
                applied = true;
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
                Dehydration.LOGGER.debug("Reflected LSO temperature immunity call failed.", exception);
            }
        }
        return applied;
    }

    private static boolean tryRemoveTemperatureImmunity(ServerPlayer player) {
        if (!findTemperatureImmunityReflection()) {
            return false;
        }
        try {
            Class<?> util = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil");
            Class<?> immunityEnum = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureImmunityEnum");
            Method removeMethod = util.getMethod("removeImmunity", net.minecraft.world.entity.player.Player.class, immunityEnum);
            boolean removed = false;
            for (Object immunity : cachedTemperatureImmunityValues) {
                removeMethod.invoke(null, player, immunity);
                removed = true;
            }
            return removed;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
            Dehydration.LOGGER.debug("Reflected LSO temperature immunity removal failed.", exception);
            return false;
        }
    }

    private static boolean findTemperatureImmunityReflection() {
        if (searchedTemperatureImmunityMethod) {
            return cachedAddTemperatureImmunityMethod != null && cachedTemperatureImmunityValues != null;
        }
        searchedTemperatureImmunityMethod = true;
        try {
            Class<?> util = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil");
            Class<?> immunityEnum = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureImmunityEnum");
            cachedAddTemperatureImmunityMethod = util.getMethod("addImmunity", net.minecraft.world.entity.player.Player.class, immunityEnum);
            Object values = immunityEnum.getMethod("values").invoke(null);
            if (values instanceof Object[] array) {
                cachedTemperatureImmunityValues = array;
            }
            return cachedAddTemperatureImmunityMethod != null && cachedTemperatureImmunityValues != null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Dehydration.LOGGER.debug("Could not resolve LSO temperature immunity API by reflection.", exception);
            return false;
        }
    }

    private static boolean tryCapabilityReflection(ServerPlayer player, double amount) {
        if (tryDrainObject(player, player, amount)) {
            return true;
        }
        Optional<Object> lsoCap = findLsoCapabilityFromPlayer(player);
        return lsoCap.filter(capability -> tryDrainObject(capability, player, amount)).isPresent();
    }

    private static Optional<Object> findLsoCapabilityFromPlayer(ServerPlayer player) {
        for (Method method : player.getClass().getMethods()) {
            if (!method.getName().toLowerCase(Locale.ROOT).contains("thirst") || method.getParameterCount() != 0) {
                continue;
            }
            try {
                return Optional.ofNullable(method.invoke(player));
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
                // Keep probing; the integration is intentionally best-effort.
            }
        }
        return Optional.empty();
    }

    private static boolean tryDrainObject(Object target, ServerPlayer player, double amount) {
        if (target == null) {
            return false;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!isNamed(method, THIRST_DRAIN_METHODS)) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            try {
                if (parameters.length == 1 && isNumber(parameters[0])) {
                    method.invoke(target, convertNumber(amount, parameters[0]));
                    return true;
                }
                if (parameters.length == 2 && parameters[0].isAssignableFrom(ServerPlayer.class) && isNumber(parameters[1])) {
                    method.invoke(target, player, convertNumber(amount, parameters[1]));
                    return true;
                }
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
                Dehydration.LOGGER.debug("Reflected LSO thirst capability method failed: {}", method, exception);
            }
        }
        return false;
    }

    private static boolean invokeIfPresent(Object target, String methodName, Class<?> parameterType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean invokeIfPresent(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.invoke(target);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static Optional<Boolean> invokeBoolean(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Boolean value ? Optional.of(value) : Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Number> invokeNumber(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number value ? Optional.of(value) : Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Number> readStaticNumber(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getField(fieldName);
            Object value = field.get(null);
            return value instanceof Number number ? Optional.of(number) : Optional.empty();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isNamed(Method method, String[] names) {
        for (String name : names) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNumber(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class;
    }

    private static Object convertNumber(double amount, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return Math.max(1, (int) Math.round(amount));
        }
        if (type == float.class || type == Float.class) {
            return (float) amount;
        }
        return amount;
    }
}
