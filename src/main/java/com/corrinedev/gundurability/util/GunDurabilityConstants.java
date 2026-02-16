package com.corrinedev.gundurability.util;

public final class GunDurabilityConstants {

    private GunDurabilityConstants() {
    }

    public static final int DEFAULT_MAX_DURABILITY = 2000;
    public static final int DEFAULT_JAM_CHANCE = 15;
    public static final int DEFAULT_INACCURACY_RATE = 500;

    public static final String BIOME_DESERT = "minecraft:desert";
    public static final String BIOME_RIVER = "minecraft:river";
    public static final String BIOME_PLAINS = "minecraft:plains";

    public static final float BIOME_MODIFIER_DESERT = 1.5f;
    public static final float BIOME_MODIFIER_RIVER = 2.0f;
    public static final float BIOME_MODIFIER_PLAINS = 0.8f;

    public static final int BURST_MODE_MULTIPLIER = 3;
    public static final int SINGLE_SHOT_MULTIPLIER = 1;

    public static final int DEFAULT_UNJAM_TIME_TICKS = 100;

    public static final int COLOR_GREEN = -13382656;
    public static final int COLOR_YELLOW = -154;
    public static final int COLOR_ORANGE = -26317;
    public static final int COLOR_RED = -39322;

    public static final double DURABILITY_THRESHOLD_HIGH = 75.0;
    public static final double DURABILITY_THRESHOLD_MEDIUM = 50.0;
    public static final double DURABILITY_THRESHOLD_LOW = 25.0;

    public static final int OVERLAY_TEXT_OFFSET_X = 97;
    public static final int OVERLAY_TEXT_OFFSET_Y = 16;
    public static final int OVERLAY_ICON_OFFSET_X = 115;
    public static final int OVERLAY_ICON_OFFSET_Y = 25;
    public static final int OVERLAY_ICON_SIZE = 16;

    public static final int REPAIR_GUI_WIDTH = 176;
    public static final int REPAIR_GUI_HEIGHT = 166;
    public static final int BUTTON_OFFSET_X = 34;
    public static final int BUTTON_OFFSET_Y = 171;
    public static final int BUTTON_WIDTH = 114;
    public static final int BUTTON_HEIGHT = 20;

    public static final String UNJAMMABLE_DB_SHORT = "tacz:db_short";
    public static final String UNJAMMABLE_DB_LONG = "tacz:db_long";
}
