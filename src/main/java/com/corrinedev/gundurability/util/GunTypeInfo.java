package com.corrinedev.gundurability.util;

public class GunTypeInfo {
    private final String gunType;
    private final String caliber;
    private final int maxDurability;
    private final long timestamp;

    public GunTypeInfo(String gunType, String caliber, int maxDurability) {
        this.gunType = gunType;
        this.caliber = caliber;
        this.maxDurability = maxDurability;
        this.timestamp = System.currentTimeMillis();
    }

    public String gunType() { return gunType; }
    public String caliber() { return caliber; }
    public int maxDurability() { return maxDurability; }
    public long timestamp() { return timestamp; }

    public boolean isValid() {
        return System.currentTimeMillis() - timestamp < 600_000;
    }
}
