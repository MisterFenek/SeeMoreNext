package com.seemorenext.lib.nabconfiguration;

import java.util.ArrayList;
import java.util.List;

public final class ConfigEntries {


    public static ConfigEntry<Double> doubleEntry() {
        return new ConfigEntry<>(o -> o == null ? 0.0 : ((Number) o).doubleValue());
    }

    public static ConfigEntry<Float> floatEntry() {
        return new ConfigEntry<>(o -> o == null ? 0.0f : ((Number) o).floatValue());
    }

    public static ConfigEntry<Long> longEntry() {
        return new ConfigEntry<>(o -> o == null ? 0L : ((Number) o).longValue());
    }

    public static ConfigEntry<Integer> integerEntry() {
        return integerEntry(0);
    }

    public static ConfigEntry<Integer> integerEntry(Integer defaultValue) {
        return new ConfigEntry<>(o -> o == null ? defaultValue : ((Number) o).intValue());
    }

    public static ConfigEntry<Short> shortEntry() {
        return new ConfigEntry<>(o -> o == null ? (short) 0 : ((Number) o).shortValue());
    }

    public static ConfigEntry<Byte> byteEntry() {
        return new ConfigEntry<>(o -> o == null ? (byte) 0 : ((Number) o).byteValue());
    }

    public static ConfigEntry<Boolean> booleanEntry(Boolean defaultValue) {
        return new ConfigEntry<>(o -> o == null ? defaultValue : Boolean.TRUE.equals(o));
    }

    public static <E extends Enum<E>> ConfigEntry<E> enumEntry(Class<E> e) {
        return enumEntry(e, false);
    }

    public static <E extends Enum<E>> ConfigEntry<E> enumEntry(Class<E> e, boolean caseSensitive) {
        return new ConfigEntry<>(o -> {
            if (o == null) return null;
            for (E value : e.getEnumConstants()) {
                if (caseSensitive ? value.name().equals(o.toString()) : value.name().equalsIgnoreCase(o.toString())) {
                    return value;
                }
            }
            return null;
        });
    }
    
    public static ConfigEntry<List<String>> stringListEntry() {
        return new ConfigEntry<>(o -> o == null ? new ArrayList<>() : (List<String>) o);
    }

}
