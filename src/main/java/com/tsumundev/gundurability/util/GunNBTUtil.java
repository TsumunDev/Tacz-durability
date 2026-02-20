package com.tsumundev.gundurability.util;

import com.tsumundev.gundurability.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class GunNBTUtil {

    public static final String KEY_DURABILITY = "Durability";
    public static final String KEY_GUN_ID = "GunId";
    public static final String KEY_JAMMED = "Jammed";
    public static final String KEY_FIRE_MODE = "GunFireMode";
    public static final String KEY_WETNESS = "GunWetness";
    public static final String KEY_LAST_RAIN_TICK = "LastRainTick";
    public static final String KEY_UNJAM_PROGRESS = "UnjamProgress";
    public static final String KEY_UNJAM_REQUIRED = "UnjamRequired";
    public static final String FIRE_MODE_BURST = "BURST";
    public static final String FIRE_MODE_AUTO = "AUTO";

    private GunNBTUtil() {
    }

    public static CompoundTag getTag(ItemStack stack) {
        return stack.getOrCreateTag();
    }

    public static boolean hasDurability(ItemStack stack) {
        return getTag(stack).contains(KEY_DURABILITY);
    }

    public static int getDurability(ItemStack stack) {
        return getTag(stack).getInt(KEY_DURABILITY);
    }

    public static void setDurability(ItemStack stack, int durability) {
        getTag(stack).putInt(KEY_DURABILITY, durability);
    }

    public static int getMaxDurability(ItemStack stack) {
        String gunId = getGunId(stack);
        if (gunId == null || gunId.isEmpty()) {
            return Config.MAXDURABILITY.get();
        }
        return Config.getDurability(gunId);
    }

    public static double getDurabilityPercent(ItemStack stack) {
        int max = getMaxDurability(stack);
        if (max <= 0) {
            return 0.0;
        }
        return (double) getDurability(stack) / max;
    }

    public static String getGunId(ItemStack stack) {
        return getTag(stack).getString(KEY_GUN_ID);
    }

    public static void setGunId(ItemStack stack, String gunId) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(KEY_DURABILITY)) {
            int maxDurability = (gunId != null && !gunId.isEmpty())
                ? Config.getDurability(gunId)
                : Config.MAXDURABILITY.get();
            tag.putInt(KEY_DURABILITY, maxDurability);
        }
        if (gunId != null) {
            tag.putString(KEY_GUN_ID, gunId);
        }
    }

    public static boolean isJammed(ItemStack stack) {
        return getTag(stack).getBoolean(KEY_JAMMED);
    }

    public static void setJammed(ItemStack stack, boolean jammed) {
        getTag(stack).putBoolean(KEY_JAMMED, jammed);
    }

    public static void unjam(ItemStack stack) {
        setJammed(stack, false);
    }

    // ===== Système de checkup multiple pour désenrayage =====

    public static int getUnjamProgress(ItemStack stack) {
        return getTag(stack).getInt(KEY_UNJAM_PROGRESS);
    }

    public static void setUnjamProgress(ItemStack stack, int progress) {
        getTag(stack).putInt(KEY_UNJAM_PROGRESS, Math.max(0, progress));
    }

    public static int getRequiredUnjam(ItemStack stack) {
        return getTag(stack).getInt(KEY_UNJAM_REQUIRED);
    }

    public static void setRequiredUnjam(ItemStack stack, int required) {
        getTag(stack).putInt(KEY_UNJAM_REQUIRED, Math.max(1, required));
    }

    public static void incrementUnjamProgress(ItemStack stack) {
        int current = getUnjamProgress(stack);
        int required = getRequiredUnjam(stack);
        setUnjamProgress(stack, current + 1);
        if (current + 1 >= required) {
            unjam(stack);
            setUnjamProgress(stack, 0);
            setRequiredUnjam(stack, 1);
        }
    }

    public static boolean needsMoreUnjam(ItemStack stack) {
        return isJammed(stack) && getUnjamProgress(stack) < getRequiredUnjam(stack);
    }

    public static void setupUnjamRequirements(ItemStack stack) {
        double durabilityPercent = getDurabilityPercent(stack);
        int required;

        // Plus l'arme est usée, plus il faut de checkups
        if (durabilityPercent > 0.7) {
            required = 2;  // Bon état: 2 checkups
        } else if (durabilityPercent > 0.4) {
            required = 3;  // Usée: 3 checkups
        } else if (durabilityPercent > 0.2) {
            required = 4;  // Très usée: 4 checkups
        } else {
            required = 5;  // Critique: 5 checkups
        }

        setRequiredUnjam(stack, required);
        setUnjamProgress(stack, 0);
    }

    public static String getFireMode(ItemStack stack) {
        return getTag(stack).getString(KEY_FIRE_MODE);
    }

    public static boolean isAutoMode(ItemStack stack) {
        return FIRE_MODE_AUTO.equals(getTag(stack).getString(KEY_FIRE_MODE));
    }

    public static boolean isBurstMode(ItemStack stack) {
        return FIRE_MODE_BURST.equals(getTag(stack).getString(KEY_FIRE_MODE));
    }

    public static boolean isSemiAutoMode(ItemStack stack) {
        String mode = getTag(stack).getString(KEY_FIRE_MODE);
        return mode.isEmpty() || (!FIRE_MODE_AUTO.equals(mode) && !FIRE_MODE_BURST.equals(mode));
    }

    public static void repair(ItemStack stack, int amount) {
        int current = getDurability(stack);
        int max = getMaxDurability(stack);
        setDurability(stack, Math.min(max, current + amount));
    }

    public static void damage(ItemStack stack, int amount) {
        int current = getDurability(stack);
        setDurability(stack, Math.max(0, current - amount));
    }

    public static boolean isBroken(ItemStack stack) {
        return getDurability(stack) <= 0;
    }

    // ===== Système d'humidité =====

    public static double getWetness(ItemStack stack) {
        return getTag(stack).getDouble(KEY_WETNESS);
    }

    public static void setWetness(ItemStack stack, double wetness) {
        getTag(stack).putDouble(KEY_WETNESS, Math.max(0.0, Math.min(1.0, wetness)));
    }

    public static void addWetness(ItemStack stack, double amount) {
        setWetness(stack, getWetness(stack) + amount);
    }

    public static boolean isWet(ItemStack stack) {
        return getWetness(stack) > 0.1;
    }

    public static boolean isVeryWet(ItemStack stack) {
        return getWetness(stack) > 0.5;
    }

    public static boolean isSoaked(ItemStack stack) {
        return getWetness(stack) > 0.8;
    }

    public static long getLastRainTick(ItemStack stack) {
        return getTag(stack).getLong(KEY_LAST_RAIN_TICK);
    }

    public static void setLastRainTick(ItemStack stack, long tick) {
        getTag(stack).putLong(KEY_LAST_RAIN_TICK, tick);
    }

    public static String getWetnessState(ItemStack stack) {
        double wetness = getWetness(stack);
        if (wetness <= 0.05) return "Sèche";
        if (wetness <= 0.2) return "Légèrement humide";
        if (wetness <= 0.5) return "Humide";
        if (wetness <= 0.8) return "Très humide";
        return "Détrempée";
    }
}
