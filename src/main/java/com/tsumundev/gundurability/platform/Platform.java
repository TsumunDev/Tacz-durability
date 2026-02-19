package com.tsumundev.gundurability.platform;

public class Platform {
    private static final boolean IS_NEOFORGE;
    private static final IPlatformHelper HELPER;

    static {
        boolean neo;
        try {
            Class.forName("net.neoforged.fml.ModLoader");
            neo = true;
        } catch (ClassNotFoundException e) {
            neo = false;
        }
        IS_NEOFORGE = neo;
        HELPER = IS_NEOFORGE ? new NeoForgePlatformHelper() : new ForgePlatformHelper();
    }

    public static IPlatformHelper getPlatform() {
        return HELPER;
    }

    public static String getPlatformName() {
        return HELPER.getPlatformName();
    }

    public static boolean isModLoaded(String modId) {
        return HELPER.isModLoaded(modId);
    }

    public static boolean isDevelopment() {
        return HELPER.isDevelopmentEnvironment();
    }

    public static boolean isNeoForge() {
        return IS_NEOFORGE;
    }

    private static class ForgePlatformHelper implements IPlatformHelper {
        @Override
        public String getPlatformName() {
            return "Forge";
        }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                Class<?> modList = Class.forName("net.minecraftforge.fml.ModList");
                Object list = modList.getMethod("get").invoke(null);
                return (Boolean) modList.getMethod("isLoaded", String.class).invoke(list, modId);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean isDevelopmentEnvironment() {
            try {
                Class<?> fmlLoader = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
                return !(Boolean) fmlLoader.getMethod("isProduction").invoke(null);
            } catch (Exception e) {
                return true;
            }
        }
    }

    private static class NeoForgePlatformHelper implements IPlatformHelper {
        @Override
        public String getPlatformName() {
            return "NeoForge";
        }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                Class<?> modList = Class.forName("net.neoforged.fml.ModList");
                Object list = modList.getMethod("get").invoke(null);
                return (Boolean) modList.getMethod("isLoaded", String.class).invoke(list, modId);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean isDevelopmentEnvironment() {
            try {
                Class<?> fmlLoader = Class.forName("net.neoforged.fml.loading.FMLLoader");
                return !(Boolean) fmlLoader.getMethod("isProduction").invoke(null);
            } catch (Exception e) {
                return true;
            }
        }
    }
}
