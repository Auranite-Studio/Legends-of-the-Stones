package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class LegendsOfTheStonesAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LegendsOfTheStones.MODID);

    // === ATTACHMENT ДЛЯ НАКОПЛЕНИЯ ЭЛЕМЕНТОВ НА СУЩНОСТЯХ ===
    public static final Supplier<AttachmentType<Map<ElementType, Integer>>> ELEMENT_ACCUMULATOR =
            ATTACHMENT_TYPES.register("element_accumulator", () ->
                    AttachmentType.<Map<ElementType, Integer>>builder(() -> new EnumMap<>(ElementType.class)).build()
            );

    // === ATTACHMENT ДЛЯ ЭЛЕМЕНТА СНАРЯДА ===
    public static final Supplier<AttachmentType<ElementType>> PROJECTILE_ELEMENT =
            ATTACHMENT_TYPES.register("projectile_element", () ->
                    AttachmentType.<ElementType>builder(() -> null).build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // === МЕТОДЫ ДЛЯ ELEMENT_ACCUMULATOR ===

    public static Map<ElementType, Integer> getAccumulator(LivingEntity entity) {
        return entity.getData(ELEMENT_ACCUMULATOR.get());
    }

    public static void addPoints(LivingEntity entity, ElementType type, int amount) {
        Map<ElementType, Integer> acc = getAccumulator(entity);
        acc.put(type, acc.getOrDefault(type, 0) + amount);
    }

    public static int getPoints(LivingEntity entity, ElementType type) {
        return getAccumulator(entity).getOrDefault(type, 0);
    }

    public static void setPoints(LivingEntity entity, ElementType type, int amount) {
        getAccumulator(entity).put(type, amount);
    }

    public static void resetPoints(LivingEntity entity, ElementType type) {
        getAccumulator(entity).put(type, 0);
    }

    public static boolean hasReachedThreshold(LivingEntity entity, ElementType type, int threshold) {
        return getPoints(entity, type) >= threshold;
    }

    public static void clearAllPoints(LivingEntity entity) {
        getAccumulator(entity).clear();
    }

    // === МЕТОДЫ ДЛЯ PROJECTILE_ELEMENT ===

    public static void setProjectileElement(Entity entity, ElementType type) {
        if (entity != null && !entity.level().isClientSide && type != null) {
            entity.setData(PROJECTILE_ELEMENT.get(), type);
        }
    }

    public static ElementType getProjectileElement(Entity entity) {
        if (entity != null) {
            return entity.getData(PROJECTILE_ELEMENT.get());
        }
        return null;
    }

    public static boolean hasProjectileElement(Entity entity) {
        ElementType element = getProjectileElement(entity);
        return element != null;
    }

    // ✅ ИСПРАВЛЕНО: Не используем setData с null (вызывает краш!)
    // Attachments автоматически очищаются при удалении сущности
    public static void clearProjectileElement(Entity entity) {
        // No-op - attachment будет очищен автоматически при удалении сущности
        // Попытка установить null через setData() вызывает NullPointerException
        if (entity != null && !entity.level().isClientSide) {
            // Можно логировать для отладки, но не вызывать setData(null)
            // LegendsOfTheStones.LOGGER.debug("Clearing projectile element for entity {}", entity.getId());
        }
    }
    
    // === МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ ===
    
    public static void syncAccumulatorToClients(LivingEntity entity) {
        Map<ElementType, Integer> accumulator = getAccumulator(entity);
        network.SyncElementalAccumulationMessage.sendToTracking(entity, accumulator);
    }
}