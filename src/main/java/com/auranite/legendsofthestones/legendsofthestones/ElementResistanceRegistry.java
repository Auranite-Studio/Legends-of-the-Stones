
package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.world.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

public class ElementResistanceRegistry {

    private ElementResistanceRegistry() {}

    public static void init() {
        LegendsOfTheStones.LOGGER.info("Initializing Element Resistance Registry...");
        LegendsOfTheStones.LOGGER.info("ElementType.FIRE.getDamageTypeId() = {}", ElementType.FIRE.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.PHYSICAL.getDamageTypeId() = {}", ElementType.PHYSICAL.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.WIND.getDamageTypeId() = {}", ElementType.WIND.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.WATER.getDamageTypeId() = {}", ElementType.WATER.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.EARTH.getDamageTypeId() = {}", ElementType.EARTH.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.ICE.getDamageTypeId() = {}", ElementType.ICE.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.ELECTRIC.getDamageTypeId() = {}", ElementType.ELECTRIC.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.SOURCE.getDamageTypeId() = {}", ElementType.SOURCE.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.NATURAL.getDamageTypeId() = {}", ElementType.NATURAL.getDamageTypeId());
        LegendsOfTheStones.LOGGER.info("ElementType.QUANTUM.getDamageTypeId() = {}", ElementType.QUANTUM.getDamageTypeId());


        registerUniform(ElementType.FIRE, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.PHYSICAL, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.WIND, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.WATER, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.EARTH, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.ICE, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.ELECTRIC, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.SOURCE, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.NATURAL, 0.0f, 0.0f, EntityType.PLAYER);
        registerUniform(ElementType.QUANTUM, 0.0f, 0.0f, EntityType.PLAYER);

        try {
            registerFireResistances();
            registerPhysicalResistances();
            registerEarthResistances();
            registerWaterResistances();
            registerWindResistances();
            registerNaturalResistances();
            registerQuantumResistances();
            registerIceResistances();
            registerElectricResistances();

            LegendsOfTheStones.LOGGER.info("Element Resistance Registry initialized! Total: {} entities",
                    ElementResistanceManager.getRegisteredEntityCount());
        } catch (Exception e) {
            LegendsOfTheStones.LOGGER.error("Failed to initialize Element Resistance Registry!", e);
            e.printStackTrace();
        }
    }

    private static void registerFireResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering FIRE resistances...");

        registerUniform(ElementType.FIRE, 1.0f, 1.0f
        );

        registerUniform(ElementType.FIRE, -0.25f, -0.25f
        );

        registerUniform(ElementType.FIRE, 0.5f, 0.5f
        );
    }

    private static void registerPhysicalResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering PHYSICAL resistances...");

        registerUniform(ElementType.PHYSICAL, 1.0f, 1.0f
        );

        registerUniform(ElementType.PHYSICAL, 0.5f, 0.5f,
                EntityType.IRON_GOLEM
        );

        registerUniform(ElementType.PHYSICAL, 0.25f, 0.25f
        );

        registerCustom(ElementType.PHYSICAL, 0.0f, -0.5f
        );
    }

    private static void registerWindResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering WIND resistances...");

        registerUniform(ElementType.WIND, 1.0f, 1.0f
        );

        registerUniform(ElementType.WIND, 0.5f, 0.5f,
                EntityType.IRON_GOLEM
        );

        registerUniform(ElementType.WIND, 0.25f, 0.25f
        );

        registerCustom(ElementType.WIND, 0.0f, -0.5f
        );
    }

    private static void registerEarthResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering EARTH resistances...");

        registerUniform(ElementType.EARTH, 1.0f, 1.0f
        );

        registerUniform(ElementType.EARTH, 0.5f, 0.5f
        );

        registerUniform(ElementType.EARTH, 0.25f, 0.25f
        );

        registerCustom(ElementType.EARTH, 0.0f, -0.5f
        );
    }

    private static void registerWaterResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering WATER resistances...");

        registerUniform(ElementType.WATER, 1.0f, 1.0f
        );

        registerUniform(ElementType.WATER, 0.5f, 0.5f
        );

        registerUniform(ElementType.WATER, 0.25f, 0.25f
        );

        registerCustom(ElementType.WATER, 0.0f, -0.5f,
                EntityType.IRON_GOLEM
        );
    }

    private static void registerIceResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering ICE resistances...");

        registerUniform(ElementType.ICE, 1.0f, 1.0f
        );

        registerUniform(ElementType.ICE, 0.5f, 0.5f
        );

        registerUniform(ElementType.ICE, 0.25f, 0.25f
        );

        registerCustom(ElementType.ICE, 0.0f, -0.5f
        );
    }

    private static void registerElectricResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering ELECTRIC resistances...");

        registerUniform(ElementType.ELECTRIC, 1.0f, 1.0f
        );

        registerUniform(ElementType.ELECTRIC, 0.5f, 0.5f
        );

        registerUniform(ElementType.ELECTRIC, 0.25f, 0.25f
        );

        registerCustom(ElementType.ELECTRIC, 0.0f, -0.5f
        );
    }

    private static void registerSourceResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering SOURCE resistances...");

        registerUniform(ElementType.SOURCE, 1.0f, 1.0f
        );

        registerUniform(ElementType.SOURCE, 0.5f, 0.5f
        );

        registerUniform(ElementType.SOURCE, 0.25f, 0.25f
        );

        registerCustom(ElementType.SOURCE, 0.0f, -0.5f
        );
    }

    private static void registerNaturalResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering NATURAL resistances...");

        registerUniform(ElementType.NATURAL, 1.0f, 1.0f
        );

        registerUniform(ElementType.NATURAL, 0.5f, 0.5f
        );

        registerUniform(ElementType.NATURAL, 0.25f, 0.25f
        );

        registerCustom(ElementType.NATURAL, 0.0f, -0.5f
        );
    }

    private static void registerQuantumResistances() {
        LegendsOfTheStones.LOGGER.debug("Registering QUANTUM resistances...");

        registerUniform(ElementType.QUANTUM, 1.0f, 1.0f
        );

        registerUniform(ElementType.QUANTUM, 0.5f, 0.5f
        );

        registerUniform(ElementType.QUANTUM, 0.25f, 0.25f
        );

        registerCustom(ElementType.QUANTUM, 0.0f, -0.5f
        );
    }


    @SafeVarargs
    private static void registerUniform(ElementType elementType, float resistance, EntityType<?>... entityTypes) {
        registerUniform(elementType, resistance, resistance, entityTypes);
    }

    @SafeVarargs
    private static void registerUniform(ElementType elementType, float accumulationResistance, float damageResistance, EntityType<?>... entityTypes) {
        for (EntityType<?> type : entityTypes) {
            if (type == null) continue;
            Map<ElementType, ElementResistanceManager.Resistance> map = new EnumMap<>(ElementType.class);
            map.put(elementType, new ElementResistanceManager.Resistance(accumulationResistance, damageResistance));
            ElementResistanceManager.registerResistance(type, map);
        }
    }

    @SafeVarargs
    private static void registerCustom(ElementType elementType, float accumulationResistance, float damageResistance, EntityType<?>... entityTypes) {
        for (EntityType<?> type : entityTypes) {
            if (type == null) continue;
            Map<ElementType, ElementResistanceManager.Resistance> map = new EnumMap<>(ElementType.class);
            map.put(elementType, new ElementResistanceManager.Resistance(accumulationResistance, damageResistance));
            ElementResistanceManager.registerResistance(type, map);
        }
    }

    public static void registerSingle(EntityType<?> entityType, ElementType elementType,
                                      float accumulationResistance, float damageResistance) {
        if (entityType == null || elementType == null) return;
        Map<ElementType, ElementResistanceManager.Resistance> map = new EnumMap<>(ElementType.class);
        map.put(elementType, new ElementResistanceManager.Resistance(accumulationResistance, damageResistance));
        ElementResistanceManager.registerResistance(entityType, map);
    }

    public static void registerSingleUniform(EntityType<?> entityType, ElementType elementType, float resistance) {
        registerSingle(entityType, elementType, resistance, resistance);
    }

    public static void registerMultiple(EntityType<?> entityType, Map<ElementType, ElementResistanceManager.Resistance> resistanceMap) {
        if (entityType == null || resistanceMap == null || resistanceMap.isEmpty()) return;
        ElementResistanceManager.registerResistance(entityType, new EnumMap<>(resistanceMap));
    }

    public static boolean hasResistances(EntityType<?> entityType) {
        return entityType != null && ElementResistanceManager.getRegisteredEntityCount() > 0;
    }
}