package com.corrinedev.gundurability.util;

import com.corrinedev.gundurability.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class GunNBTUtil {

    public static final String KEY_DURABILITY = "Durability";
    public static final String KEY_GUN_ID = "GunId";
    public static final String KEY_JAMMED = "Jammed";
    public static final String KEY_FIRE_MODE = "GunFireMode";
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
}
