package com.corrinedev.gundurability.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Configuration côté client avec ForgeConfigSpec (TOML).
 */
public class ConfigClient {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Affichage
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOWGUI;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_ICON_OVERLAY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_DURABILITY_TEXT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_DURABILITY_BAR;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOWRED;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_BRACKETS;

    // Couleurs
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_HIGH;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_MEDIUM;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_LOW;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_CRITICAL;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR_BACKGROUND;

    // Seuils de durabilité
    public static final ForgeConfigSpec.ConfigValue<Double> THRESHOLD_HIGH;
    public static final ForgeConfigSpec.ConfigValue<Double> THRESHOLD_MEDIUM;
    public static final ForgeConfigSpec.ConfigValue<Double> THRESHOLD_LOW;

    // Textes personnalisables
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_JAMMED;
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_JAM_CLEARED;
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_BROKEN;
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_DURABILITY_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<String> TEXT_PERCENT_FORMAT;

    // Position overlay
    public static final ForgeConfigSpec.ConfigValue<String> OVERLAY_POSITION;
    public static final ForgeConfigSpec.ConfigValue<Integer> OVERLAY_OFFSET_X;
    public static final ForgeConfigSpec.ConfigValue<Integer> OVERLAY_OFFSET_Y;
    public static final ForgeConfigSpec.ConfigValue<Boolean> OVERLAY_SHADOW;
    public static final ForgeConfigSpec.ConfigValue<Boolean> OVERLAY_BACKGROUND;

    static {
        BUILDER.comment("=====================================================================").push("Gun Durability Client Configuration");

        SHOWGUI = BUILDER
                .comment("Afficher l'overlay de durabilité")
                .define("showGui", false);

        SHOW_ICON_OVERLAY = BUILDER
                .comment("Afficher l'icône de durabilité")
                .define("showIconOverlay", false);

        SHOW_DURABILITY_TEXT = BUILDER
                .comment("Afficher le texte de durabilité")
                .define("showDurabilityText", false);

        SHOW_DURABILITY_BAR = BUILDER
                .comment("Afficher la barre de durabilité")
                .define("showDurabilityBar", false);

        SHOWRED = BUILDER
                .comment("Appliquer une teinte rouge sur les armes usées")
                .define("showRedTint", false);

        SHOW_BRACKETS = BUILDER
                .comment("Afficher les crochets autour du pourcentage")
                .define("showBrackets", true);

        BUILDER.push("Colors");

        COLOR_HIGH = BUILDER
                .comment("Couleur pour durabilité élevée (hex sans #)")
                .define("colorHigh", "AAAAAA");

        COLOR_MEDIUM = BUILDER
                .comment("Couleur pour durabilité moyenne")
                .define("colorMedium", "AAAAAA");

        COLOR_LOW = BUILDER
                .comment("Couleur pour durabilité faible")
                .define("colorLow", "AAAAAA");

        COLOR_CRITICAL = BUILDER
                .comment("Couleur pour durabilité critique")
                .define("colorCritical", "AAAAAA");

        COLOR_BACKGROUND = BUILDER
                .comment("Couleur de fond (hex avec alpha)")
                .define("colorBackground", "80000000");

        BUILDER.pop();

        BUILDER.push("Thresholds");

        THRESHOLD_HIGH = BUILDER
                .comment("Seuil pour durabilité élevée (%)")
                .defineInRange("thresholdHigh", 75.0, 0.0, 100.0);

        THRESHOLD_MEDIUM = BUILDER
                .comment("Seuil pour durabilité moyenne (%)")
                .defineInRange("thresholdMedium", 50.0, 0.0, 100.0);

        THRESHOLD_LOW = BUILDER
                .comment("Seuil pour durabilité faible (%)")
                .defineInRange("thresholdLow", 25.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.push("Text Messages");

        TEXT_JAMMED = BUILDER
                .comment("Message quand l'arme est enrayée")
                .define("textJammed", "Enrayée !");

        TEXT_JAM_CLEARED = BUILDER
                .comment("Message quand l'arme est désenrayée")
                .define("textJamCleared", "Arme réparée");

        TEXT_BROKEN = BUILDER
                .comment("Message quand l'arme est cassée")
                .define("textBroken", "Cassée");

        TEXT_DURABILITY_FORMAT = BUILDER
                .comment("Format du texte de durabilité")
                .define("textDurabilityFormat", "{percent}");

        TEXT_PERCENT_FORMAT = BUILDER
                .comment("Format du pourcentage")
                .define("textPercentFormat", "[{percent}%]");

        BUILDER.pop();

        BUILDER.push("Overlay Position");

        OVERLAY_POSITION = BUILDER
                .comment("Position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .define("overlayPosition", "BOTTOM_LEFT");

        OVERLAY_OFFSET_X = BUILDER
                .comment("Décalage X")
                .defineInRange("offsetX", 5, -32768, 32767);

        OVERLAY_OFFSET_Y = BUILDER
                .comment("Décalage Y")
                .defineInRange("offsetY", 5, -32768, 32767);

        OVERLAY_SHADOW = BUILDER
                .comment("Afficher l'ombre du texte")
                .define("overlayShadow", true);

        OVERLAY_BACKGROUND = BUILDER
                .comment("Afficher un fond semi-transparent")
                .define("overlayBackground", false);

        BUILDER.pop();
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Convertit une couleur hex string en int pour Minecraft.
     */
    public static int parseColor(String hex) {
        try {
            if (hex.startsWith("0x") || hex.startsWith("0X")) {
                return (int) Long.parseLong(hex.substring(2), 16);
            } else if (hex.startsWith("#")) {
                return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            } else {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            }
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    /**
     * Récupère la couleur pour un niveau de durabilité donné.
     */
    public static int getColorForDurability(double percent) {
        if (percent > THRESHOLD_HIGH.get()) {
            return parseColor(COLOR_HIGH.get());
        } else if (percent > THRESHOLD_MEDIUM.get()) {
            return parseColor(COLOR_MEDIUM.get());
        } else if (percent > THRESHOLD_LOW.get()) {
            return parseColor(COLOR_LOW.get());
        } else {
            return parseColor(COLOR_CRITICAL.get());
        }
    }

    /**
     * Formate le texte de pourcentage.
     */
    public static String formatPercentText(double percent) {
        String percentStr = String.format("%.0f", percent);
        if (SHOW_BRACKETS.get()) {
            return "[" + percentStr + "%]";
        }
        return percentStr + "%";
    }

    /**
     * Formate le texte complet de durabilité.
     */
    public static String formatDurabilityText(double percent) {
        return formatPercentText(percent);
    }

    /**
     * Enum pour les positions de l'overlay.
     */
    public enum OverlayPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        public static OverlayPosition fromString(String value) {
            if (value == null) return BOTTOM_LEFT;
            try {
                return OverlayPosition.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return BOTTOM_LEFT;
            }
        }
    }

    /**
     * Récupère la position de l'overlay.
     */
    public static OverlayPosition getOverlayPosition() {
        return OverlayPosition.fromString(OVERLAY_POSITION.get());
    }
}
