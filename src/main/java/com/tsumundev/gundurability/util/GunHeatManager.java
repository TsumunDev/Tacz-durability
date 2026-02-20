package com.tsumundev.gundurability.util;

import com.tsumundev.gundurability.config.DifficultyProfile;
import com.tsumundev.gundurability.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de la chaleur des armes.
 *
 * Système de surchauffe:
 * - 0-50%: fonctionnement normal
 * - 50-75%: usure x1.5
 * - 75-90%: usure x2, risque d'enrayage
 * - 90-100%: usure x3, arme peut se casser
 *
 * La chaleur se dissipe progressivement quand l'arme n'est pas tirée.
 */
public class GunHeatManager {

    private static final String HEAT_TAG = "GunHeat";
    private static final String LAST_FIRE_TAG = "LastFireTick";
    private static final String OVERHEATED_TAG = "Overheated";

    // Cache pour éviter les accès NBT trop fréquents
    private static final Map<String, HeatData> HEAT_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 5000; // 5 secondes

    /**
     * Ajoute de la chaleur à une arme après un tir.
     * @return La nouvelle température (0.0 à 1.0)
     */
    public static double addHeat(ItemStack gunStack, Player player, boolean isAuto, boolean hasHeavyBarrel, boolean hasSilencer) {
        String gunId = GunNBTUtil.getGunId(gunStack);
        String cacheKey = getCacheKey(player, gunStack);

        HeatData data = getCachedHeatData(gunStack, cacheKey);

        // Calculer l'augmentation de chaleur
        double heatIncrease = calculateHeatIncrease(isAuto, hasHeavyBarrel, hasSilencer);

        // Appliquer le multiplicateur de profil de difficulté
        DifficultyProfile profile = DifficultyProfileManager.getCurrentProfile();
        heatIncrease *= profile.heatMultiplier();

        // Ajouter la chaleur
        data.heat = Math.min(1.0, data.heat + heatIncrease);
        data.lastFireTick = player.level().getGameTime();
        data.overheated = data.heat >= 0.9;

        // Mettre à jour le cache et le NBT
        HEAT_CACHE.put(cacheKey, data);
        saveHeatData(gunStack, data);

        return data.heat;
    }

    /**
     * Met à jour la chaleur d'une arme (dissipation).
     * Doit être appelé chaque tick sur l'arme tenue.
     */
    public static void updateHeat(ItemStack gunStack, Player player) {
        String cacheKey = getCacheKey(player, gunStack);
        HeatData data = getCachedHeatData(gunStack, cacheKey);

        if (data.heat <= 0) {
            return;
        }

        long currentTick = player.level().getGameTime();
        long ticksSinceFire = currentTick - data.lastFireTick;

        // La chaleur se dissipe progressivement
        // Taux de dissipation de base: 0.5% par tick (sans toucher l'arme)
        double dissipationRate = 0.005;

        // Bonus de dissipation si l'arme n'est pas tirée depuis un moment
        if (ticksSinceFire > 20) { // 1 seconde
            dissipationRate *= 2.0;
        }
        if (ticksSinceFire > 100) { // 5 secondes
            dissipationRate *= 2.0;
        }

        // Refroidissement accéléré si le joueur est dans un biome froid
        String biomeId = player.level().getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.location().toString()).orElse("");
        if (biomeId.contains("snow") || biomeId.contains("ice") || biomeId.contains("frozen")) {
            dissipationRate *= 1.5;
        }

        // Chaleur supplémentaire si le biome est chaud (désert)
        if (biomeId.contains("desert")) {
            dissipationRate *= 0.7;
        }

        data.heat = Math.max(0, data.heat - dissipationRate);

        // Reset overheated status si la chaleur baisse
        if (data.heat < 0.5) {
            data.overheated = false;
        }

        HEAT_CACHE.put(cacheKey, data);
        saveHeatData(gunStack, data);
    }

    /**
     * Récupère la température actuelle d'une arme (0.0 à 1.0).
     */
    public static double getTemperature(ItemStack gunStack) {
        HeatData data = getHeatData(gunStack);
        return data.heat;
    }

    /**
     * Vérifie si l'arme est surchauffée.
     */
    public static boolean isOverheated(ItemStack gunStack) {
        HeatData data = getHeatData(gunStack);
        return data.overheated || data.heat >= 0.9;
    }

    /**
     * Récupère le multiplicateur d'usure selon la température.
     */
    public static double getHeatWearMultiplier(ItemStack gunStack) {
        double heat = getTemperature(gunStack);

        if (heat < 0.5) {
            return 1.0;
        } else if (heat < 0.75) {
            return 1.5;
        } else if (heat < 0.9) {
            return 2.0;
        } else {
            return 3.0;
        }
    }

    /**
     * Récupère le niveau de chaleur pour l'affichage (0 à 4).
     */
    public static int getHeatLevel(ItemStack gunStack) {
        double heat = getTemperature(gunStack);
        if (heat < 0.25) return 0;
        if (heat < 0.5) return 1;
        if (heat < 0.75) return 2;
        if (heat < 0.9) return 3;
        return 4;
    }

    /**
     * Refroidit immédiatement une arme (pour les kits de nettoyage, etc.).
     */
    public static void coolDown(ItemStack gunStack, double amount) {
        HeatData data = getHeatData(gunStack);
        data.heat = Math.max(0, data.heat - amount);
        data.overheated = false;
        saveHeatData(gunStack, data);
    }

    // ===== Méthodes privées =====

    private static HeatData getCachedHeatData(ItemStack gunStack, String cacheKey) {
        HeatData cached = HEAT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        return getHeatData(gunStack);
    }

    private static HeatData getHeatData(ItemStack gunStack) {
        CompoundTag tag = gunStack.getOrCreateTag();

        HeatData data = new HeatData();
        data.heat = tag.getDouble(HEAT_TAG);
        data.lastFireTick = tag.getLong(LAST_FIRE_TAG);
        data.overheated = tag.getBoolean(OVERHEATED_TAG);

        return data;
    }

    private static void saveHeatData(ItemStack gunStack, HeatData data) {
        CompoundTag tag = gunStack.getOrCreateTag();
        tag.putDouble(HEAT_TAG, data.heat);
        tag.putLong(LAST_FIRE_TAG, data.lastFireTick);
        tag.putBoolean(OVERHEATED_TAG, data.overheated);
    }

    private static String getCacheKey(Player player, ItemStack gunStack) {
        return player.getUUID().toString() + gunStack.getDescriptionId();
    }

    private static double calculateHeatIncrease(boolean isAuto, boolean hasHeavyBarrel, boolean hasSilencer) {
        double baseHeat = isAuto ? 0.03 : 0.01;

        if (hasHeavyBarrel) {
            baseHeat *= 0.7; // -30% de chaleur
        }

        if (hasSilencer) {
            baseHeat *= 1.3; // +30% de chaleur (backpressure)
        }

        return baseHeat;
    }

    /**
     * Classe pour stocker les données de chaleur.
     */
    private static class HeatData {
        double heat = 0.0;
        long lastFireTick = 0;
        boolean overheated = false;
    }
}
