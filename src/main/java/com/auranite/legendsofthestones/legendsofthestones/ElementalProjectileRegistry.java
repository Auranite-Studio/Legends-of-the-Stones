package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр для привязки типов снарядов к элементальному урону и множителю накопления.
 * Все регистрации должны выполняться после инициализации сущностей (в CommonSetup).
 */
public class ElementalProjectileRegistry {

    // === КЭШ: EntityType -> ElementType ===
    private static final Map<EntityType<?>, ElementType> PROJECTILE_ELEMENT_MAP = new ConcurrentHashMap<>();

    // === КЭШ: EntityType -> Accumulation Multiplier ===
    private static final Map<EntityType<?>, Float> PROJECTILE_ACCUM_MAP = new ConcurrentHashMap<>();

    // === КЭШ: Class -> ElementType ===
    private static final Map<Class<? extends Entity>, ElementType> PROJECTILE_CLASS_MAP = new ConcurrentHashMap<>();

    // === КЭШ: Class -> Accumulation Multiplier ===
    private static final Map<Class<? extends Entity>, Float> PROJECTILE_CLASS_ACCUM_MAP = new ConcurrentHashMap<>();

    // === ФЛАГ: применять ли элемент от атакующего ===
    private static boolean inheritElementFromShooter = true;

    // === ИНИЦИАЛИЗАЦИЯ ===
    public static void register(IEventBus modEventBus) {
        LegendsOfTheStones.LOGGER.info("ElementalProjectileRegistry initialized");
    }

    // === РЕГИСТРАЦИЯ СНАРЯДОВ ===

    /**
     * Регистрирует тип сущности-снаряда с элементальным типом и множителем накопления
     */
    public static void registerProjectile(EntityType<?> entityType, ElementType element, float accumulationMultiplier) {
        if (entityType == null || element == null) {
            LegendsOfTheStones.LOGGER.warn("Cannot register null projectile type or element");
            return;
        }
        PROJECTILE_ELEMENT_MAP.put(entityType, element);
        PROJECTILE_ACCUM_MAP.put(entityType, accumulationMultiplier);
        LegendsOfTheStones.LOGGER.debug("Registered projectile {} → {} (accum: x{})", entityType, element, accumulationMultiplier);
    }

    /**
     * Регистрирует снаряд по классу сущности с элементальным типом и множителем
     */
    public static void registerProjectileByClass(Class<? extends Entity> entityClass, ElementType element, float accumulationMultiplier) {
        if (entityClass == null || element == null) {
            LegendsOfTheStones.LOGGER.warn("Cannot register null projectile class or element");
            return;
        }
        PROJECTILE_CLASS_MAP.put(entityClass, element);
        PROJECTILE_CLASS_ACCUM_MAP.put(entityClass, accumulationMultiplier);
        LegendsOfTheStones.LOGGER.debug("Registered projectile class {} → {} (accum: x{})", entityClass.getSimpleName(), element, accumulationMultiplier);
    }

    // === ПОЛУЧЕНИЕ ЭЛЕМЕНТА ===

    public static Optional<ElementType> getElementForType(EntityType<?> entityType) {
        return Optional.ofNullable(PROJECTILE_ELEMENT_MAP.get(entityType));
    }

    public static Optional<ElementType> getElementForEntity(Entity entity) {
        if (entity == null) return Optional.empty();

        // 1. Проверка по EntityType
        ElementType byType = PROJECTILE_ELEMENT_MAP.get(entity.getType());
        if (byType != null) return Optional.of(byType);

        // 2. Проверка по классу
        for (Map.Entry<Class<? extends Entity>, ElementType> entry : PROJECTILE_CLASS_MAP.entrySet()) {
            if (entry.getKey().isInstance(entity)) {
                return Optional.of(entry.getValue());
            }
        }

        // 3. Проверка attachment
        if (LegendsOfTheStonesAttachments.hasProjectileElement(entity)) {
            return Optional.ofNullable(LegendsOfTheStonesAttachments.getProjectileElement(entity));
        }

        return Optional.empty();
    }

    // === ПОЛУЧЕНИЕ МНОЖИТЕЛЯ НАКОПЛЕНИЯ ===

    public static Optional<Float> getAccumulationMultiplierForEntity(Entity entity) {
        if (entity == null) return Optional.empty();

        // 1. По EntityType
        Float byType = PROJECTILE_ACCUM_MAP.get(entity.getType());
        if (byType != null) return Optional.of(byType);

        // 2. По классу
        for (Map.Entry<Class<? extends Entity>, Float> entry : PROJECTILE_CLASS_ACCUM_MAP.entrySet()) {
            if (entry.getKey().isInstance(entity)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    public static boolean isElementalProjectile(Entity entity) {
        return getElementForEntity(entity).isPresent();
    }

    public static int getRegisteredCount() {
        return PROJECTILE_ELEMENT_MAP.size();
    }

    // === АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ЭЛЕМЕНТА ===

    public static boolean applyElementToProjectile(Entity projectile, LivingEntity shooter) {
        if (projectile == null || projectile.level().isClientSide) return false;

        Optional<ElementType> registeredElement = getElementForEntity(projectile);
        ElementType elementToApply = null;

        if (registeredElement.isPresent()) {
            elementToApply = registeredElement.get();
        }
        else if (inheritElementFromShooter && shooter != null) {
            net.minecraft.world.item.ItemStack weapon = shooter.getMainHandItem();
            elementToApply = ElementDamageHandler.getElementTypeFromItem(weapon);
        }

        if (elementToApply != null) {
            LegendsOfTheStonesAttachments.setProjectileElement(projectile, elementToApply);
            return true;
        }

        return false;
    }

    // === НАСТРОЙКИ ===

    public static void setInheritElementFromShooter(boolean value) {
        inheritElementFromShooter = value;
    }

    public static boolean getInheritElementFromShooter() {
        return inheritElementFromShooter;
    }

    // === УТИЛИТЫ ДЛЯ СОЗДАНИЯ ===

    public static <T extends Entity> T createAndLaunchElementalProjectile(
            net.minecraft.server.level.ServerLevel level,
            LivingEntity shooter,
            EntityType<T> projectileType,
            float velocity,
            float inaccuracy
    ) {
        T projectile = projectileType.create(level);
        if (projectile == null) return null;

        projectile.moveTo(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(),
                shooter.getYHeadRot(), shooter.getXRot());

        applyElementToProjectile(projectile, shooter);

        if (projectile instanceof net.minecraft.world.entity.projectile.Projectile proj) {
            proj.shootFromRotation(shooter, shooter.getXRot(), shooter.getYHeadRot(), 0.0F, velocity, inaccuracy);
            proj.setOwner(shooter);
        }

        level.addFreshEntity(projectile);
        return projectile;
    }

    public static <T extends Entity> T createElementalProjectileWithOverride(
            net.minecraft.server.level.ServerLevel level,
            LivingEntity shooter,
            EntityType<T> projectileType,
            ElementType forcedElement,
            float velocity,
            float inaccuracy
    ) {
        T projectile = projectileType.create(level);
        if (projectile == null) return null;

        projectile.moveTo(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(),
                shooter.getYHeadRot(), shooter.getXRot());

        if (forcedElement != null && !level.isClientSide) {
            LegendsOfTheStonesAttachments.setProjectileElement(projectile, forcedElement);
        }

        if (projectile instanceof net.minecraft.world.entity.projectile.Projectile proj) {
            proj.shootFromRotation(shooter, shooter.getXRot(), shooter.getYHeadRot(), 0.0F, velocity, inaccuracy);
            proj.setOwner(shooter);
        }

        level.addFreshEntity(projectile);
        return projectile;
    }
}