package com.tsumundev.gundurability.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class GunTypeCache extends LinkedHashMap<String, GunTypeInfo> {
    private static final int MAX_SIZE = 500;
    private static final long serialVersionUID = 1L;

    public GunTypeCache() {
        super(MAX_SIZE, 0.75f, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, GunTypeInfo> eldest) {
        if (size() > MAX_SIZE) {
            return true;
        }
        return false;
    }

    public GunTypeInfo getValid(String key) {
        GunTypeInfo info = get(key);
        if (info != null && !info.isValid()) {
            remove(key);
            return null;
        }
        return info;
    }

    public void clearCache() {
        clear();
    }
}
