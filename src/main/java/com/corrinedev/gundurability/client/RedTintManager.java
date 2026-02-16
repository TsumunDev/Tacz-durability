package com.corrinedev.gundurability.client;

public class RedTintManager {

    private static boolean showRedTint = false;

    public static void setShowRedTint(boolean value) {
        showRedTint = value;
    }

    public static boolean isShowRedTint() {
        return showRedTint;
    }
}
