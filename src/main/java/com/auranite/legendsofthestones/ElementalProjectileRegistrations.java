package com.auranite.legendsofthestones;

import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Класс для регистрации элементальных снарядов.
 * Регистрация выполняется в FMLCommonSetupEvent, когда все сущности уже доступны.
 */
public class ElementalProjectileRegistrations {

    /**
     * Метод для вызова через modEventBus.addListener()
     */
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ElementalProjectileRegistrations::registerAll);
    }

    /**
     * Регистрирует все элементальные снаряды
     */
    public static void registerAll() {
        // === СТАНДАРТНЫЕ СНАРЯДЫ MINECRAFT ===

        // Физические
        ElementalProjectileRegistry.registerProjectile(EntityType.ARROW, ElementType.PHYSICAL,5f );
        ElementalProjectileRegistry.registerProjectile(EntityType.SPECTRAL_ARROW, ElementType.PHYSICAL, 5f);

        // Огненные
        ElementalProjectileRegistry.registerProjectile(EntityType.FIREBALL, ElementType.FIRE, 100f);
        ElementalProjectileRegistry.registerProjectile(EntityType.SMALL_FIREBALL, ElementType.FIRE, 50f);
        ElementalProjectileRegistry.registerProjectile(EntityType.DRAGON_FIREBALL, ElementType.SOURCE, 25f);

        // Другие
        ElementalProjectileRegistry.registerProjectile(EntityType.FIREWORK_ROCKET, ElementType.PHYSICAL, 50f);
        ElementalProjectileRegistry.registerProjectile(EntityType.WITHER_SKULL, ElementType.EARTH, 20f);
        ElementalProjectileRegistry.registerProjectile(EntityType.SHULKER_BULLET, ElementType.WIND, 20f);
        ElementalProjectileRegistry.registerProjectile(EntityType.LLAMA_SPIT, ElementType.WATER, 3f);
        ElementalProjectileRegistry.registerProjectile(EntityType.BREEZE_WIND_CHARGE, ElementType.WIND, 20f);
        ElementalProjectileRegistry.registerProjectile(EntityType.WIND_CHARGE, ElementType.WIND, 20f);

        // === КАСТОМНЫЕ СНАРЯДЫ МОДА ===
        // ✅ ТЕПЕРЬ МОЖНО ИСПОЛЬЗОВАТЬ .get() БЕЗ ОШИБОК
        registerCustomProjectiles();

        LegendsOfTheStones.LOGGER.info("Registered {} elemental projectile types",
                ElementalProjectileRegistry.getRegisteredCount());
    }

    /**
     * Регистрация кастомных снарядов
     */
    private static void registerCustomProjectiles() {

//        ElementalProjectileRegistry.registerProjectile(
//                LegendsOfTheStonesEntities.WATER_ATTACK_PROJECTILE.get(),  // ← .get() теперь безопасен
//                ElementType.WATER,50f
//        );
    }
}