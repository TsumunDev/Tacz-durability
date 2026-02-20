package com.tsumundev.gundurability.config;

import com.tsumundev.gundurability.config.DurabilityItemHolder;
import com.tsumundev.gundurability.util.GunTypeInfo;
import com.tsumundev.gundurability.util.GunTypeCache;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration principale du mod avec ForgeConfigSpec (TOML).
 * Supporte le reload à chaud via /taczd reload.
 */
public class Config {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Config valeurs
    public static final ForgeConfigSpec.ConfigValue<Integer> MAXDURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> INACCURACYRATE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> GUNSBREAK;
    public static final ForgeConfigSpec.ConfigValue<Integer> JAMCHANCE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> DEBUG;

    // Système de durabilité par type d'arme
    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_GUN_TYPE_CONFIG;

    // Multiplicateurs de risque d'enrayage selon le mode de tir
    public static final ForgeConfigSpec.ConfigValue<Double> JAM_MULTIPLIER_AUTO;
    public static final ForgeConfigSpec.ConfigValue<Double> JAM_MULTIPLIER_BURST;
    public static final ForgeConfigSpec.ConfigValue<Double> JAM_MULTIPLIER_SEMI;

    // Messages immersifs
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_IMMERSIVE_MESSAGES;

    // Liste des armes non-enrayables
    public static final ForgeConfigSpec.ConfigValue<List<?>> GUN_LIST;

    // Liste des modificateurs de durabilité (format: gunId,maxDurability,jamMultiplier,jamTime)
    public static final ForgeConfigSpec.ConfigValue<List<?>> DURABILITY_LIST;

    // ===== GUN TYPE CONFIG VALUES =====
    // Valeurs par défaut
    public static final ForgeConfigSpec.ConfigValue<Integer> DEFAULT_MAX_DURABILITY;

    // Multiplicateurs d'enrayage
    public static final ForgeConfigSpec.ConfigValue<Double> BASE_JAM_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> LOW_DURABILITY_JAM_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> CRITICAL_DURABILITY_JAM_MULTIPLIER;

    // Multiplicateurs de biome
    public static final ForgeConfigSpec.ConfigValue<Double> DESERT_JAM_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> RIVER_JAM_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> SNOW_JAM_MULTIPLIER;
    public static final ForgeConfigSpec.ConfigValue<Double> RAIN_JAM_MULTIPLIER;

    // Durabilités par type d'arme
    public static final ForgeConfigSpec.ConfigValue<Integer> PISTOL_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> SMG_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> RIFLE_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> SNIPER_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> SHOTGUN_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> LMG_DURABILITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> DMR_DURABILITY;

    // Items de durabilité (pour compatibilité)
    public static final List<DurabilityItemHolder> ITEMS = List.of(
        DurabilityItemHolder.AK_BARREL,
        DurabilityItemHolder.GUN_BARREL,
        DurabilityItemHolder.GUN_BOLT,
        DurabilityItemHolder.RECOIL_SPRING,
        DurabilityItemHolder.BRASS_BRUSH,
        DurabilityItemHolder.WD40
    );

    // Creative tab location
    public static final ForgeConfigSpec.ConfigValue<String> TABLOCATION;

    // Biome modifiers (pour compatibilité - utilisent GunTypeConfig à la place)
    public static final List<BiomeModifier> BIOMEMODIFIERS = List.of();

    // OPTIMISATION: Cache LRU pour les lookups de type d'arme (évite de reparser à chaque tir)
    private static final GunTypeCache GUN_TYPE_CACHE = new GunTypeCache();

    // Cache pour les lookups O(1) - modificateurs de durabilité personnalisés
    private static final Map<String, DurabilityModifier> DURABILITY_CACHE = new ConcurrentHashMap<>();

    static {
        BUILDER.comment("=====================================================================").push("Gun Durability Configuration (DayZ Style)");

        MAXDURABILITY = BUILDER
                .comment("Durabilité maximale par défaut des armes (si useGunTypeConfig est désactivé)")
                .defineInRange("maxDurability", 2000, 100, 100000);

        INACCURACYRATE = BUILDER
                .comment("Taux d'imprécision quand l'arme est usée")
                .defineInRange("inaccuracyRate", 500, 0, 10000);

        GUNSBREAK = BUILDER
                .comment("Les armes peuvent-elles se casser définitivement ?")
                .define("doGunsBreak", false);

        JAMCHANCE = BUILDER
                .comment("Chance de base d'enrayage (plus bas = plus d'enrayages)")
                .defineInRange("jamChance", 15, 1, 1000);

        USE_GUN_TYPE_CONFIG = BUILDER
                .comment("Activer le système de durabilité par type d'arme (DayZ style)")
                .define("useGunTypeConfig", true);

        BUILDER.push("Jam Multipliers by Fire Mode");

        JAM_MULTIPLIER_AUTO = BUILDER
                .comment("Multiplicateur de risque d'enrayage en mode automatique")
                .defineInRange("auto", 3.0, 1.0, 100.0);

        JAM_MULTIPLIER_BURST = BUILDER
                .comment("Multiplicateur de risque d'enrayage en mode rafale")
                .defineInRange("burst", 2.5, 1.0, 100.0);

        JAM_MULTIPLIER_SEMI = BUILDER
                .comment("Multiplicateur de risque d'enrayage en mode semi-automatique")
                .defineInRange("semi", 1.0, 1.0, 100.0);

        BUILDER.pop();

        SHOW_IMMERSIVE_MESSAGES = BUILDER
                .comment("Afficher les messages immersifs (Enrayée, Réparé, Cassé)")
                .define("showImmersiveMessages", false);

        GUN_LIST = BUILDER
                .comment("Liste des armes qui ne peuvent pas s'enrayer")
                .defineList("unjammableList", () -> new ArrayList<String>(List.of("tacz:db_short", "tacz:db_long")), o -> true);

        DURABILITY_LIST = BUILDER
                .comment("Liste des durabilités personnalisées. Format: gunId,maxDurability,jamMultiplier,jamTime")
                .defineList("durabilityList", () -> new ArrayList<String>(), o -> true);

        TABLOCATION = BUILDER
                .comment("Item pour l'onglet créatif")
                .define("creativeTab", "gundurability:brass_brush");

        DEBUG = BUILDER
                .comment("Activer le mode debug")
                .define("debug", false);

        // ===== GUN TYPE CONFIG SECTION =====
        BUILDER.push("Gun Type Durability");

        DEFAULT_MAX_DURABILITY = BUILDER
                .comment("Durabilité maximale par défaut pour les types d'arme")
                .defineInRange("defaultMaxDurability", 1000, 100, 100000);

        BUILDER.push("Jam Multipliers");

        BASE_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur de base pour les enrayages")
                .defineInRange("jamBaseMultiplier", 0.8, 0.1, 10.0);

        LOW_DURABILITY_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur quand durabilité < 25%")
                .defineInRange("jamLowDurabilityMultiplier", 3.0, 1.0, 20.0);

        CRITICAL_DURABILITY_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur quand durabilité < 10%")
                .defineInRange("jamCriticalDurabilityMultiplier", 6.0, 1.0, 50.0);

        BUILDER.pop();

        BUILDER.push("Biome Modifiers");

        DESERT_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur d'enrayage dans le désert (sable)")
                .defineInRange("desert", 2.0, 1.0, 20.0);

        RIVER_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur d'enrayage dans l'eau")
                .defineInRange("river", 4.0, 1.0, 50.0);

        RAIN_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur d'enrayage quand il pleut")
                .defineInRange("rain", 1.3, 1.0, 10.0);

        SNOW_JAM_MULTIPLIER = BUILDER
                .comment("Multiplicateur d'enrayage dans la neige")
                .defineInRange("snow", 1.8, 1.0, 20.0);

        BUILDER.pop();

        BUILDER.push("Durability by Gun Type");

        PISTOL_DURABILITY = BUILDER
                .comment("Pistolets (Glock, 1911, etc.)")
                .defineInRange("pistol", 800, 100, 10000);

        SMG_DURABILITY = BUILDER
                .comment("SMG (MP5, Uzi, etc.)")
                .defineInRange("smg", 1000, 100, 10000);

        RIFLE_DURABILITY = BUILDER
                .comment("Fusils d'assaut (AK, M4, etc.)")
                .defineInRange("rifle", 1200, 100, 10000);

        SNIPER_DURABILITY = BUILDER
                .comment("Snipers (mécanismes précis = fragiles)")
                .defineInRange("sniper", 600, 100, 10000);

        SHOTGUN_DURABILITY = BUILDER
                .comment("Fusils à pompe (stress mécanique élevé mais plus solides)")
                .defineInRange("shotgun", 600, 100, 10000);

        LMG_DURABILITY = BUILDER
                .comment("Mitrailleuses (très robustes)")
                .defineInRange("lmg", 2500, 100, 20000);

        DMR_DURABILITY = BUILDER
                .comment("DMR (entre rifle et sniper)")
                .defineInRange("dmr", 1400, 100, 10000);

        BUILDER.pop();
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Reconstruit le cache des modificateurs de durabilité.
     * OPTIMISÉ: vide aussi le cache LRU des types d'armes.
     */
    public static void refreshCache() {
        DURABILITY_CACHE.clear();
        GUN_TYPE_CACHE.clearCache();  // Vider le cache LRU aussi

        for (Object entryObj : DURABILITY_LIST.get()) {
            try {
                String entry = entryObj.toString();
                String[] parts = entry.split(",");
                if (parts.length >= 2) {
                    String gunId = parts[0].trim();
                    int maxDur = Integer.parseInt(parts[1].trim());
                    double jamMult = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 1.0;
                    int jamTime = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 100;
                    DURABILITY_CACHE.put(gunId, new DurabilityModifier(gunId, maxDur, (float) jamMult, jamTime));
                }
            } catch (Exception e) {
                // Ignorer les entrées invalides
            }
        }
    }

    /**
     * Récupère la durabilité maximale pour une arme.
     * OPTIMISÉ: utilise le cache LRU pour éviter de reparser le gunId à chaque tir.
     */
    public static int getDurability(String gunId) {
        if (gunId == null || gunId.isEmpty()) {
            return MAXDURABILITY.get();
        }

        // Vérifier le cache personnalisé d'abord
        DurabilityModifier modifier = DURABILITY_CACHE.get(gunId);
        if (modifier != null) {
            return modifier.maxDurability();
        }

        // OPTIMISATION: Vérifier le cache LRU pour les types d'armes
        GunTypeInfo cachedInfo = GUN_TYPE_CACHE.getValid(gunId);
        if (cachedInfo != null) {
            return cachedInfo.maxDurability();
        }

        // Si le système de type est activé, calculer et mettre en cache
        if (USE_GUN_TYPE_CONFIG.get()) {
            String gunType = extractGunTypeOptimized(gunId);
            String caliber = extractCaliberOptimized(gunId);
            int durability = getDurabilityForGun(gunId, gunType, caliber);

            // Mettre en cache LRU pour les prochains appels
            GUN_TYPE_CACHE.put(gunId, new GunTypeInfo(gunType, caliber, durability));

            return durability;
        }

        return MAXDURABILITY.get();
    }

    /**
     * Récupère le modificateur de durabilité pour une arme.
     */
    public static DurabilityModifier getDurabilityModifier(String gunId) {
        if (gunId == null || gunId.isEmpty()) {
            return null;
        }
        return DURABILITY_CACHE.get(gunId);
    }

    /**
     * Vérifie si une arme est dans la liste des armes non-enrayables.
     */
    public static boolean isUnjammable(String gunId) {
        if (gunId == null || gunId.isEmpty()) {
            return false;
        }
        for (Object entry : GUN_LIST.get()) {
            if (gunId.equals(entry.toString())) {
                return true;
            }
        }
        return false;
    }

    private static String extractGunType(String gunId) {
        if (gunId == null) return null;
        String lower = gunId.toLowerCase();
        if (lower.contains("glock") || lower.contains("1911") || lower.contains("p226") || lower.contains("usp")) return "pistol";
        else if (lower.contains("mp5") || lower.contains("uzi") || lower.contains("p90") || lower.contains("vector")) return "smg";
        else if (lower.contains("ak") || lower.contains("m4") || lower.contains("m16") || lower.contains("scar")) return "rifle";
        else if (lower.contains("awp") || lower.contains("m700") || lower.contains("m24")) return "sniper";
        else if (lower.contains("m870") || lower.contains("spas") || lower.contains("benelli")) return "shotgun";
        else if (lower.contains("m249") || lower.contains("mg") || lower.contains("pk")) return "lmg";
        else if (lower.contains("dmr") || lower.contains("sr") || lower.contains("hk417")) return "dmr";
        return null;
    }

    private static String extractCaliber(String gunId) {
        if (gunId == null) return null;
        String lower = gunId.toLowerCase();
        if (lower.contains("9mm") || lower.contains("9_")) return "9mm";
        if (lower.contains("45") || lower.contains("acp")) return "45acp";
        if (lower.contains("762") || lower.contains("7.62")) return "762x39";
        if (lower.contains("556") || lower.contains("5.56")) return "556x45";
        if (lower.contains("308") || lower.contains("7.62") && lower.contains("nato")) return "762x51";
        if (lower.contains("50") || lower.contains("bmg")) return "50bmg";
        if (lower.contains("12") && lower.contains("gauge")) return "12gauge";
        return null;
    }

    /**
     * OPTIMISÉ: Extract gun type sans allocation de String (toLowerCase).
     * Utilise regionMatches pour la comparaison insensible à la casse.
     */
    private static String extractGunTypeOptimized(String gunId) {
        if (gunId == null) return null;

        // Shotguns (check first - common names)
        if (containsIgnoreCase(gunId, "m870") || containsIgnoreCase(gunId, "spas") ||
            containsIgnoreCase(gunId, "benelli") || containsIgnoreCase(gunId, "db_") ||
            containsIgnoreCase(gunId, "double") || containsIgnoreCase(gunId, "sawed") ||
            containsIgnoreCase(gunId, "remington") || containsIgnoreCase(gunId, "shotgun") ||
            containsIgnoreCase(gunId, "dp12") || containsIgnoreCase(gunId, "short") ||
            containsIgnoreCase(gunId, "long")) return "shotgun";

        // Pistols
        if (containsIgnoreCase(gunId, "glock") || containsIgnoreCase(gunId, "1911") ||
            containsIgnoreCase(gunId, "p226") || containsIgnoreCase(gunId, "usp") ||
            containsIgnoreCase(gunId, "deagle") || containsIgnoreCase(gunId, "beretta") ||
            containsIgnoreCase(gunId, "pistol")) return "pistol";

        // SMG
        if (containsIgnoreCase(gunId, "mp5") || containsIgnoreCase(gunId, "uzi") ||
            containsIgnoreCase(gunId, "p90") || containsIgnoreCase(gunId, "vector") ||
            containsIgnoreCase(gunId, "mp7") || containsIgnoreCase(gunId, "mp9") ||
            containsIgnoreCase(gunId, "mac") || containsIgnoreCase(gunId, "smg")) return "smg";

        // Rifles
        if (containsIgnoreCase(gunId, "ak") || containsIgnoreCase(gunId, "m4") ||
            containsIgnoreCase(gunId, "m16") || containsIgnoreCase(gunId, "scar") ||
            containsIgnoreCase(gunId, "aug") || containsIgnoreCase(gunId, "famas") ||
            containsIgnoreCase(gunId, "galil") || containsIgnoreCase(gunId, "qbz")) return "rifle";

        // Snipers
        if (containsIgnoreCase(gunId, "awp") || containsIgnoreCase(gunId, "m700") ||
            containsIgnoreCase(gunId, "m24") || containsIgnoreCase(gunId, "m98") ||
            containsIgnoreCase(gunId, "svd") || containsIgnoreCase(gunId, "kar98") ||
            containsIgnoreCase(gunId, "mosin")) return "sniper";

        // LMG
        if (containsIgnoreCase(gunId, "m249") || containsIgnoreCase(gunId, "mg") ||
            containsIgnoreCase(gunId, "pk") || containsIgnoreCase(gunId, "rpk") ||
            containsIgnoreCase(gunId, "negev") || containsIgnoreCase(gunId, "lmg")) return "lmg";

        // DMR
        if (containsIgnoreCase(gunId, "dmr") || containsIgnoreCase(gunId, "sr") ||
            containsIgnoreCase(gunId, "hk417") || containsIgnoreCase(gunId, "sr25") ||
            containsIgnoreCase(gunId, "mk14") || containsIgnoreCase(gunId, "g3")) return "dmr";

        return null;
    }

    /**
     * OPTIMISÉ: Extract caliber sans allocation de String (toLowerCase).
     */
    private static String extractCaliberOptimized(String gunId) {
        if (gunId == null) return null;

        // Chercher les motifs de caliber sans toLowerCase complet
        if (containsIgnoreCase(gunId, "9mm") || containsIgnoreCase(gunId, "9_")) return "9mm";
        if (containsIgnoreCase(gunId, "45") && containsIgnoreCase(gunId, "acp")) return "45acp";
        if (containsIgnoreCase(gunId, "762") || containsIgnoreCase(gunId, "7.62")) {
            if (containsIgnoreCase(gunId, "nato")) return "762x51";
            return "762x39";
        }
        if (containsIgnoreCase(gunId, "556") || containsIgnoreCase(gunId, "5.56")) return "556x45";
        if (containsIgnoreCase(gunId, "308")) return "762x51";
        if (containsIgnoreCase(gunId, "50") && containsIgnoreCase(gunId, "bmg")) return "50bmg";
        if (containsIgnoreCase(gunId, "12") && containsIgnoreCase(gunId, "gauge")) return "12gauge";

        return null;
    }

    /**
     * OPTIMISÉ: Vérifie si une chaîne contient une sous-chaîne sans créer de nouvelle String.
     * Équivalent case-insensitive de String.contains() sans allocation.
     */
    private static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;
        int strLen = str.length();
        int searchLen = searchStr.length();
        if (strLen < searchLen) return false;

        // Chercher la première correspondance de caractère
        char firstChar = searchStr.charAt(0);
        for (int i = 0; i <= strLen - searchLen; i++) {
            // Comparaison case-insensitive du premier caractère
            if (Character.toLowerCase(str.charAt(i)) == Character.toLowerCase(firstChar)) {
                // Vérifier si le reste correspond
                boolean match = true;
                for (int j = 1; j < searchLen; j++) {
                    if (Character.toLowerCase(str.charAt(i + j)) !=
                        Character.toLowerCase(searchStr.charAt(j))) {
                        match = false;
                        break;
                    }
                }
                if (match) return true;
            }
        }
        return false;
    }

    /**
     * Classe pour les modificateurs de biome (compatibilité).
     */
    public static class BiomeModifier {
        private final String biomeName;
        private final double multiplier;

        public BiomeModifier(String biomeName, double multiplier) {
            this.biomeName = biomeName;
            this.multiplier = multiplier;
        }

        public String biomeName() { return biomeName; }
        public double multiplier() { return multiplier; }
    }

    /**
     * Classe de compatibilité pour les classes qui utilisent l'ancien système.
     */
    public record DurabilityModifier(String gunId, int maxDurability, float jamMultiplier, int jamTime) {
        // Record avec mêmes champs que l'ancienne classe
    }

    // ===== Méthodes de GunTypeConfig (intégrées) =====

    /**
     * Récupère la durabilité pour une arme donnée.
     */
    public static int getDurabilityForGun(String gunId, String gunType, String caliber) {
        // Vérifier d'abord si c'est une arme spécifique connue
        Integer specific = getSpecificDurability(gunId);
        if (specific != null) return specific;

        // Ensuite par type
        if (gunType != null) {
            return switch (gunType.toLowerCase()) {
                case "pistol" -> PISTOL_DURABILITY.get();
                case "smg" -> SMG_DURABILITY.get();
                case "rifle" -> RIFLE_DURABILITY.get();
                case "sniper" -> SNIPER_DURABILITY.get();
                case "shotgun" -> SHOTGUN_DURABILITY.get();
                case "lmg" -> LMG_DURABILITY.get();
                case "dmr" -> DMR_DURABILITY.get();
                default -> DEFAULT_MAX_DURABILITY.get();
            };
        }

        return DEFAULT_MAX_DURABILITY.get();
    }

    /**
     * Récupère la durabilité pour des armes spécifiques (override).
     */
    private static Integer getSpecificDurability(String gunId) {
        if (gunId == null) return null;

        // ===== SHOTGUNS =====
        // Double barrel - extremely simple, very durable
        if (gunId.contains("db_") || gunId.contains("double_barrel") || gunId.contains("db700") ||
            gunId.contains("one_barrel")) return 800;
        // Sawed off - stress on frame
        if (gunId.contains("sawed") || gunId.contains("sawn")) return 500;
        // Remington 870 - workhorse
        if (gunId.contains("m870") || gunId.contains("remington")) return 700;
        // SPAS-12 / SPR-15 - complex but sturdy
        if (gunId.contains("spas") || gunId.contains("spr_")) return 650;
        // Benelli - modern, reliable
        if (gunId.contains("benelli")) return 750;
        // Winchester 1894 / 1887 / 1873 - lever action shotguns/rifles
        if (gunId.contains("win189") || gunId.contains("win18") || gunId.contains("m1887") ||
            gunId.contains("win187")) return 750;
        // M1887_HC - heavy coach variant
        if (gunId.contains("m1887_hc") || gunId.contains("m1887hc")) return 850;

        // ===== PISTOLS =====
        // Glock - very reliable pistol
        if (gunId.contains("glock")) return 900;
        // 1911 classic
        if (gunId.contains("1911") || gunId.contains("m1911") || gunId.contains("m45a1") ||
            gunId.contains("sti2011")) return 750;
        // Deagle - heavy but reliable
        if (gunId.contains("deagle")) return 1000;
        // Beretta / M92FS
        if (gunId.contains("beretta") || gunId.contains("m92fs") || gunId.contains("92fs")) return 850;
        // Sig Sauer
        if (gunId.contains("p226") || gunId.contains("p320") || gunId.contains("sig")) return 850;
        // USP / HK45
        if (gunId.contains("usp") || gunId.contains("hk45") || gunId.contains("g40")) return 850;
        // Hi-Power / Browning
        if (gunId.contains("hipower") || gunId.contains("browning") || gunId.contains("b92")) return 800;
        // Five-seveN / FN57
        if (gunId.contains("five") || gunId.contains("fn57")) return 800;
        // Makarov / small pistols
        if (gunId.contains("makarov") || gunId.contains("mp18") || gunId.contains("ppk")) return 700;
        // Revolvers
        if (gunId.contains("python") || gunId.contains("colt_python") || gunId.contains("sw_") ||
            gunId.contains("m1879") || gunId.contains("coltm1851") || gunId.contains("webley") ||
            gunId.contains("nagant") || gunId.contains("luger") || gunId.contains("tt33")) return 900;
        // Tec-9 / MAC-10 - cheap SMGs/pistols
        if (gunId.contains("tec_9") || gunId.contains("tec9") || gunId.contains("mac10")) return 600;

        // ===== SMG =====
        // MP5 - reliable SMG
        if (gunId.contains("mp5")) return 900;
        // Uzi - very reliable
        if (gunId.contains("uzi")) return 1000;
        // P90 - unique design
        if (gunId.contains("p90")) return 1100;
        // MP7 / MP9 / Vector - modern SMGs
        if (gunId.contains("mp7") || gunId.contains("mp9") || gunId.contains("vector")) return 950;
        // M1A1 SMG / Thompson
        if (gunId.contains("m1a1_smg") || gunId.contains("thompson")) return 1000;
        // UDP-9 / Skorpion
        if (gunId.contains("udp") || gunId.contains("skorpion")) return 850;
        // M1A1 / M1 Carbine
        if (gunId.contains("m1a1") && !gunId.contains("smg")) return 1100;
        // PP-2000 / Kedr
        if (gunId.contains("pp2000") || gunId.contains("kedr") || gunId.contains("bizon")) return 900;

        // ===== RIFLES =====
        // AK series - very reliable
        if (gunId.contains("ak47") || gunId.contains("ak74") || gunId.contains("akm") ||
            gunId.contains("ak12") || gunId.contains("ak_")) return 1600;
        // M4/M16 series - decent
        if (gunId.contains("m4a1") || gunId.contains("mk18")) return 1100;
        if (gunId.contains("m16")) return 1300;
        if (gunId.contains("scar")) return 1400;
        // HK416 / HK417
        if (gunId.contains("hk416") || gunId.contains("hk417")) return 1300;
        // AUG / FAMAS / G36
        if (gunId.contains("aug") || gunId.contains("famas") || gunId.contains("g36")) return 1200;
        // Galil / QBZ
        if (gunId.contains("galil") || gunId.contains("qbz")) return 1350;
        // FN FAL
        if (gunId.contains("fal") || gunId.contains("fn_fal")) return 1400;
        // SKS - very reliable
        if (gunId.contains("sks")) return 1500;
        // M1 Garand / M14 / MK47
        if (gunId.contains("garand") || gunId.contains("m1garand") || gunId.contains("mk47")) return 1400;
        // MDR / MDX series
        if (gunId.contains("mdx") || gunId.contains("mdr")) return 1200;
        // SKW8
        if (gunId.contains("skw")) return 1100;

        // ===== SNIPERS =====
        // AWP - precision rifle, fragile
        if (gunId.contains("awp")) return 500;
        // M700 / M24 - bolt actions
        if (gunId.contains("m700") || gunId.contains("m24")) return 600;
        // M700 Swift
        if (gunId.contains("swift")) return 550;
        // Kar98k / Mosin / Type 99 - old bolt actions
        if (gunId.contains("kar98") || gunId.contains("mosin") || gunId.contains("type99") ||
            gunId.contains("krag")) return 700;
        // Lee-Enfield / SMLE
        if (gunId.contains("smle")) return 750;
        // M1903 / Gew98 / Gras / Lebel - vintage rifles
        if (gunId.contains("m1903") || gunId.contains("gew98") || gunId.contains("gras") ||
            gunId.contains("lebel")) return 650;
        // SVD / Dragunov
        if (gunId.contains("svd")) return 1200;
        // MRAD ELR - precision
        if (gunId.contains("mrad")) return 550;
        // M82A2 - heavy sniper
        if (gunId.contains("m82")) return 800;
        // AR50 / Anzio 20mm - anti-materiel
        if (gunId.contains("ar50") || gunId.contains("anzio")) return 700;
        // Win1894 as sniper (lever action)
        if (gunId.contains("win1894") && gunId.contains("sniper")) return 750;

        // ===== LMG / MACHINE GUNS =====
        // M249 SAW - belt fed, sturdy
        if (gunId.contains("m249")) return 3000;
        // PKM / PKP - machine gun
        if (gunId.contains("pk")) return 3500;
        // RPK - AK variant LMG
        if (gunId.contains("rpk")) return 2500;
        // M60 / M134 / DP28 / MG42
        if (gunId.contains("m60") || gunId.contains("minigun") || gunId.contains("dp28") ||
            gunId.contains("mg14") || gunId.contains("madsen")) return 3000;
        // MG34 / MG42
        if (gunId.contains("mg34") || gunId.contains("mg42")) return 2800;
        // Negev / NGSW
        if (gunId.contains("negev") || gunId.contains("ngsw")) return 3200;

        // ===== DMR =====
        // SR25 / MK14 / G3 / HK417 DMRs
        if (gunId.contains("sr25") || gunId.contains("mk14") || gunId.contains("g3")) return 1400;
        // SCAR MK20
        if (gunId.contains("mk20")) return 1500;

        // ===== SPECIAL / OTHER =====
        // Grenade launchers
        if (gunId.contains("mgl") || gunId.contains("grenade") || gunId.contains("rpg")) return 1200;
        // Flare gun
        if (gunId.contains("flare")) return 500;
        // Boombox / meme weapons
        if (gunId.contains("boombox") || gunId.contains("nitemare")) return 400;
        // Diddy / joke weapons
        if (gunId.contains("diddy") || gunId.contains("omen")) return 600;

        return null;
    }

    /**
     * Calcule le multiplicateur de probabilité d'enrayage selon la durabilité actuelle.
     */
    public static double calculateJamChanceMultiplier(int currentDurability, int maxDurability) {
        double percent = (double) currentDurability / maxDurability;

        if (percent < 0.10) {
            return CRITICAL_DURABILITY_JAM_MULTIPLIER.get();
        }
        if (percent < 0.25) {
            return LOW_DURABILITY_JAM_MULTIPLIER.get();
        }

        double durabilityFactor = 1.0 + (1.0 - percent);
        return BASE_JAM_MULTIPLIER.get() * durabilityFactor;
    }

    /**
     * Récupère le multiplicateur de biome pour l'enrayage.
     */
    public static double getBiomeJamMultiplier(String biomeId) {
        if (biomeId == null) return 1.0;

        String biomeLower = biomeId.toLowerCase();

        // Desert biomes
        if (biomeLower.contains("desert")) {
            return DESERT_JAM_MULTIPLIER.get();
        }

        // Water/ocean biomes - includes all ocean variants and coastal areas
        if (biomeLower.contains("ocean") || biomeLower.contains("deep") ||
            biomeLower.contains("river") || biomeLower.contains("lake") ||
            biomeLower.contains("beach") || biomeLower.contains("shore")) {
            return RIVER_JAM_MULTIPLIER.get();
        }

        // Snow/ice biomes
        if (biomeLower.contains("snow") || biomeLower.contains("ice") || biomeLower.contains("frozen")) {
            return SNOW_JAM_MULTIPLIER.get();
        }

        // Swamp biomes - also high humidity
        if (biomeLower.contains("swamp") || biomeLower.contains("marsh")) {
            return RIVER_JAM_MULTIPLIER.get() * 0.8;
        }

        return 1.0;
    }
}
