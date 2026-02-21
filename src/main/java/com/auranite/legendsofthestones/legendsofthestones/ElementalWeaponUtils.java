package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 🔹 УТИЛИТЫ ДЛЯ ЭЛЕМЕНТАЛЬНОГО ОРУЖИЯ 🔹
 *
 * Позволяет добавлять элементальные свойства к существующим предметам.
 * Незарегистрированные ИНСТРУМЕНТЫ по умолчанию считаются PHYSICAL.
 */
public class ElementalWeaponUtils {

    // === ПРИВАТНЫЙ КОНСТРУКТОР ===
    private ElementalWeaponUtils() {}

    /**
     * Регистрирует ванильный предмет как элементальное оружие со стандартным накоплением.
     */
    public static void registerItem(Item vanillaItem, ElementType type) {
        registerItem(vanillaItem, type, 1.0f);
    }

    /**
     * Регистрирует ванильный предмет как элементальное оружие с кастомным накоплением.
     */
    public static void registerItem(Item vanillaItem, ElementType type, float accumulationMultiplier) {
        if (vanillaItem == null || type == null) return;
        ElementalWeaponRegistry.registerWeapon(vanillaItem, type, accumulationMultiplier);
        LegendsOfTheStones.LOGGER.info("⚔️ Registered vanilla item {} as {} elemental (accum x{})",
                BuiltInRegistries.ITEM.getKey(vanillaItem), type, accumulationMultiplier);
    }

    /**
     * Регистрирует предмет по ResourceLocation со стандартным накоплением.
     */
    public static boolean registerItemById(String modId, String itemName, ElementType type) {
        return registerItemById(modId, itemName, type, 1.0f);
    }

    /**
     * Регистрирует предмет по ResourceLocation с кастомным накоплением.
     */
    public static boolean registerItemById(String modId, String itemName, ElementType type, float accumulationMultiplier) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(modId, itemName);
        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(rl);

        if (itemOpt.isPresent()) {
            ElementalWeaponRegistry.registerWeapon(itemOpt.get(), type, accumulationMultiplier);
            LegendsOfTheStones.LOGGER.info("⚔️ Registered {}:{} as {} elemental (accum x{})", modId, itemName, type, accumulationMultiplier);
            return true;
        } else {
            LegendsOfTheStones.LOGGER.warn("❌ Item not found: {}:{}", modId, itemName);
            return false;
        }
    }

    /**
     * Массовая регистрация со стандартным множителем.
     */
    @SafeVarargs
    public static void registerMultiple(ElementType type, Item... items) {
        registerMultiple(type, 1.0f, items);
    }

    /**
     * Массовая регистрация с кастомным множителем.
     */
    @SafeVarargs
    public static void registerMultiple(ElementType type, float accumulationMultiplier, Item... items) {
        if (items == null) return;
        for (Item item : items) {
            if (item != null) {
                ElementalWeaponRegistry.registerWeapon(item, type, accumulationMultiplier);
            }
        }
        LegendsOfTheStones.LOGGER.info("⚔️ Registered {} items as {} elemental (accum x{})", items.length, type, accumulationMultiplier);
    }

    /**
     * Проверяет, является ли ItemStack элементальным.
     * Возвращает true если:
     * - предмет имеет явный элемент (компонент или реестр), ИЛИ
     * - предмет не зарегистрирован, но является инструментом (PHYSICAL по умолчанию)
     */
    public static boolean isElemental(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return getElementType(stack) != null;
    }

    /**
     * Получает ElementType из ItemStack с приоритетом: компонент > реестр > PHYSICAL (только для инструментов).
     * @return ElementType или null если предмет не инструмент и не имеет элемента
     */
    public static ElementType getElementType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        // 1. Сначала проверяем компонент (наиболее специфичный, переопределяет всё)
        Optional<ElementType> component = ElementalWeaponComponent.getElement(stack);
        if (component.isPresent()) {
            return component.get();
        }

        // 2. Потом реестр (вернёт PHYSICAL для незарегистрированных инструментов)
        return ElementalWeaponRegistry.getElementType(stack);
    }

    /**
     * Получает множитель накопления из ItemStack с приоритетом: компонент > реестр.
     */
    public static float getAccumulationMultiplier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0f;

        // Сначала проверяем компонент (более специфичный)
        float componentAccum = ElementalWeaponComponent.getAccumMultiplier(stack);
        if (componentAccum != 1.0f) {
            return componentAccum;
        }

        // Потом реестр (общий для всех таких предметов)
        return ElementalWeaponRegistry.getAccumulationMultiplier(stack);
    }

    /**
     * Добавляет элемент к конкретному экземпляру ItemStack (через компонент).
     */
    public static ItemStack addElementToStack(ItemStack stack, ElementType type) {
        return addElementToStackWithAccum(stack, type, 1.0f);
    }

    /**
     * Добавляет элемент и множитель накопления к конкретному экземпляру ItemStack.
     */
    public static ItemStack addElementToStackWithAccum(ItemStack stack, ElementType type, float accumMultiplier) {
        if (stack == null || stack.isEmpty() || type == null) return stack;
        return ElementalWeaponComponent.withElementAndAccum(stack, type, accumMultiplier);
    }

    /**
     * Удаляет элемент с конкретного экземпляра ItemStack.
     * После удаления инструмент вернётся к PHYSICAL (если не зарегистрирован иначе),
     * а не-инструмент — к null.
     */
    public static ItemStack removeElementFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;
        return ElementalWeaponComponent.removeElement(stack);
    }
}