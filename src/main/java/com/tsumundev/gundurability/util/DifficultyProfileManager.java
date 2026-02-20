package com.tsumundev.gundurability.util;

import com.tsumundev.gundurability.config.DifficultyProfile;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Gestionnaire du profil de difficulté actif.
 * Stocké par joueur pour permettre des profils différents en multijoueur.
 */
public class DifficultyProfileManager {

    private static final String STORAGE_KEY = "TACZDifficultyProfile";

    // Profil par défaut (pour les nouveaux joueurs ou singleplayer)
    private static DifficultyProfile defaultProfile = DifficultyProfile.NORMAL;

    // Profils par joueur (UUID -> Profile)
    private static final java.util.Map<UUID, DifficultyProfile> playerProfiles = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Définit le profil de difficulté pour un joueur.
     */
    public static void setProfile(UUID playerId, DifficultyProfile profile) {
        playerProfiles.put(playerId, profile);
    }

    /**
     * Récupère le profil de difficulté pour un joueur.
     */
    public static DifficultyProfile getProfile(UUID playerId) {
        return playerProfiles.getOrDefault(playerId, defaultProfile);
    }

    /**
     * Récupère le profil de difficulté actuel (pour le contexte actuel).
     */
    public static DifficultyProfile getCurrentProfile() {
        return defaultProfile;
    }

    /**
     * Définit le profil par défaut.
     */
    public static void setDefaultProfile(DifficultyProfile profile) {
        defaultProfile = profile;
    }

    /**
     * Réinitialise le profil d'un joueur au profil par défaut.
     */
    public static void resetProfile(UUID playerId) {
        playerProfiles.remove(playerId);
    }

    /**
     * Récupère tous les profils disponibles.
     */
    public static DifficultyProfile[] getAllProfiles() {
        return DifficultyProfile.values();
    }

    /**
     * Récupère le nom d'affichage du profil actuel.
     */
    public static Component getCurrentProfileName() {
        return defaultProfile.displayName();
    }
}
