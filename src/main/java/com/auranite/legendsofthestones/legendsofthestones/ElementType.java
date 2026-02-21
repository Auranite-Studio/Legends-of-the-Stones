package com.auranite.legendsofthestones.legendsofthestones;

import java.util.Arrays;
import java.util.Optional;

public enum ElementType {
    FIRE("fire_dmg"),
    PHYSICAL("physical_dmg"),
    WIND ("wind_dmg"),
    EARTH ("earth_dmg"),
    WATER ("water_dmg"),
     ICE ("ice_dmg"),
    ELECTRIC("electric_dmg"),
    SOURCE("source_dmg"),
    NATURAL("natural_dmg"),
    QUANTUM("quantum_dmg");

    private final String damageTypeId;

    ElementType(String damageTypeId) {
        this.damageTypeId = damageTypeId;
    }

    public String getDamageTypeId() {
        return damageTypeId;
    }

    public String getFullDamageTypeId() {
        return "power:" + damageTypeId;
    }

    public static Optional<ElementType> fromDamageTypeId(String id) {
        String cleanId = id.contains(":") ? id.substring(id.indexOf(":") + 1) : id;

        return Arrays.stream(values())
                .filter(type -> type.getDamageTypeId().equals(cleanId))
                .findFirst();
    }

    @Override
    public String toString() {
        return name();
    }
}