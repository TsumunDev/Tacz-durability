package com.tsumundev.gundurability.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Profils de difficulté pour TACZ Durability.
 * Chaque profil préconfigure les valeurs de gameplay pour une expérience cohérente.
 */
public enum DifficultyProfile {
    EASY(
        "easy",
        Component.literal("Facile").withStyle(ChatFormatting.GREEN),
        50,      // jamChance (plus bas = plus d'enrayages, donc 50 = facile)
        0.5,     // durabilityMultiplier
        false,   // gunsBreak
        false,   // gunPartsSystem
        1.0,     // heatMultiplier
        true     // showImmersiveMessages
    ),
    NORMAL(
        "normal",
        Component.literal("Normal").withStyle(ChatFormatting.WHITE),
        15,      // jamChance
        1.0,     // durabilityMultiplier
        false,   // gunsBreak
        false,   // gunPartsSystem
        1.0,     // heatMultiplier
        false    // showImmersiveMessages
    ),
    HARDCORE(
        "hardcore",
        Component.literal("Hardcore").withStyle(ChatFormatting.RED),
        5,       // jamChance (très difficile)
        2.0,     // durabilityMultiplier (les armes s'usent 2x plus vite)
        true,    // gunsBreak
        false,   // gunPartsSystem
        1.2,     // heatMultiplier
        true     // showImmersiveMessages
    ),
    DAYZ(
        "dayz",
        Component.literal("DayZ").withStyle(ChatFormatting.DARK_RED),
        10,      // jamChance
        1.5,     // durabilityMultiplier
        true,    // gunsBreak
        false,   // gunPartsSystem
        1.5,     // heatMultiplier
        true     // showImmersiveMessages
    ),
    REALISM(
        "realism",
        Component.literal("Réalisme").withStyle(ChatFormatting.GOLD),
        8,       // jamChance
        1.2,     // durabilityMultiplier
        true,    // gunsBreak
        true,    // gunPartsSystem
        1.3,     // heatMultiplier
        true     // showImmersiveMessages
    ),
    APOCALYPSE(
        "apocalypse",
        Component.literal("Apocalypse").withStyle(ChatFormatting.DARK_PURPLE),
        3,       // jamChance (extrêmement difficile)
        2.5,     // durabilityMultiplier
        true,    // gunsBreak
        true,    // gunPartsSystem
        1.8,     // heatMultiplier
        true     // showImmersiveMessages
    );

    private final String id;
    private final Component displayName;
    private final int jamChance;
    private final double durabilityMultiplier;
    private final boolean gunsBreak;
    private final boolean gunPartsSystem;
    private final double heatMultiplier;
    private final boolean showImmersiveMessages;

    DifficultyProfile(String id, Component displayName, int jamChance, double durabilityMultiplier,
                      boolean gunsBreak, boolean gunPartsSystem, double heatMultiplier,
                      boolean showImmersiveMessages) {
        this.id = id;
        this.displayName = displayName;
        this.jamChance = jamChance;
        this.durabilityMultiplier = durabilityMultiplier;
        this.gunsBreak = gunsBreak;
        this.gunPartsSystem = gunPartsSystem;
        this.heatMultiplier = heatMultiplier;
        this.showImmersiveMessages = showImmersiveMessages;
    }

    public String id() { return id; }
    public Component displayName() { return displayName; }
    public int jamChance() { return jamChance; }
    public double durabilityMultiplier() { return durabilityMultiplier; }
    public boolean gunsBreak() { return gunsBreak; }
    public boolean gunPartsSystem() { return gunPartsSystem; }
    public double heatMultiplier() { return heatMultiplier; }
    public boolean showImmersiveMessages() { return showImmersiveMessages; }

    /**
     * Applique ce profil à la configuration.
     */
    public void applyToConfig(Consumer<ConfigOverride> overrideFunc) {
        ConfigOverride override = new ConfigOverride(
            jamChance,
            durabilityMultiplier,
            gunsBreak,
            showImmersiveMessages
        );
        overrideFunc.accept(override);
    }

    /**
     * Récupère un profil par son ID.
     */
    public static DifficultyProfile fromId(String id) {
        for (DifficultyProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id)) {
                return profile;
            }
        }
        return NORMAL;
    }

    /**
     * Classe pour contenir les valeurs de override de config.
     */
    public record ConfigOverride(
        int jamChance,
        double durabilityMultiplier,
        boolean gunsBreak,
        boolean showImmersiveMessages
    ) {}
}
