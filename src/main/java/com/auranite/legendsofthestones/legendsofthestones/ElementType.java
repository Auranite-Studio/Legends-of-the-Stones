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

    public static ElementType fromVanillaDamageType(String damageTypeId) {
        if (damageTypeId == null) return PHYSICAL; // По умолчанию физический

        String id = damageTypeId.contains(":") ? damageTypeId.split(":")[1] : damageTypeId;

        return switch (id) {

            case "arrow", "player_attack", "mob_attack", "mob_projectile",
                 "fall", "anvil", "falling_block", "cactus", "sweet_berry_bush",
                 "fly_into_wall", "dragon_breath", "wither_skull", "trident" -> PHYSICAL;

            case "in_fire", "on_fire", "lava", "hot_floor" -> FIRE;

            case "drown" -> WATER; // freeze тоже считаем водой/льдом

            case "generic" -> WIND;

            case "stalagmite" -> EARTH;

            case "lightning_bolt" -> ELECTRIC;

            case "freeze" -> ICE;

            case "indirect_magic","magic", "thorns", "sonic_boom" -> SOURCE;

            case "poison", "wither" -> NATURAL;

            case "out_of_world", "generic_kill" -> QUANTUM;

            default -> PHYSICAL;
        };
    }

    @Override
    public String toString() {
        return name();
    }
}