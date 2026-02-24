package com.auranite.legendsofthestones;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * 🔹 КОМПОНЕНТ ДАННЫХ ДЛЯ ЭЛЕМЕНТАЛЬНОГО ОРУЖИЯ 🔹
 *
 * Хранит ElementType и множитель накопления в предмете через DataComponent.
 * Работает в Minecraft 1.20.5+
 *
 * Для Minecraft 1.20.4 и ниже используйте NBT версию (см. комментарий в конце).
 */
public class ElementalWeaponComponent {

    public static final String ELEMENT_TYPE_KEY = "element_type";
    public static final String ACCUM_MULTIPLIER_KEY = "accum_multiplier";

    /**
     * Добавляет элементальный тип к ItemStack.
     */
    public static ItemStack withElement(ItemStack stack, ElementType type) {
        return withElementAndAccum(stack, type, 1.0f);
    }

    /**
     * Добавляет элементальный тип и множитель накопления к ItemStack.
     */
    public static ItemStack withElementAndAccum(ItemStack stack, ElementType type, float accumMultiplier) {
        if (stack == null || stack.isEmpty() || type == null) return stack;

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData.update(tag -> {
            tag.putString(ELEMENT_TYPE_KEY, type.name());
            tag.putFloat(ACCUM_MULTIPLIER_KEY, accumMultiplier);
        });
        stack.set(DataComponents.CUSTOM_DATA, customData);

        return stack;
    }

    /**
     * Получает элементальный тип из ItemStack.
     */
    public static Optional<ElementType> getElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return Optional.empty();

        String typeName = customData.copyTag().getString(ELEMENT_TYPE_KEY);
        if (typeName.isEmpty()) return Optional.empty();

        try {
            return Optional.of(ElementType.valueOf(typeName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Получает множитель накопления из ItemStack.
     * @return множитель или 1.0f если не указан
     */
    public static float getAccumMultiplier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0f;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 1.0f;

        return customData.copyTag().getFloat(ACCUM_MULTIPLIER_KEY);
    }

    /**
     * Проверяет, имеет ли предмет элементальный тип.
     */
    public static boolean hasElement(ItemStack stack) {
        return getElement(stack).isPresent();
    }

    /**
     * Удаляет элементальный тип с предмета.
     */
    public static ItemStack removeElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            customData.update(tag -> {
                tag.remove(ELEMENT_TYPE_KEY);
                tag.remove(ACCUM_MULTIPLIER_KEY);
            });
            stack.set(DataComponents.CUSTOM_DATA, customData);
        }

        return stack;
    }
}

/*
 * ============================================================================
 * NBT ВЕРСИЯ ДЛЯ MINECRAFT 1.20.4 И НИЖЕ
 * ============================================================================
 * Замените весь класс выше на этот если используете 1.20.4 или ниже:
 *
package com.esmods.keepersofthestonestwo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ElementalWeaponComponent {

    public static final String ELEMENT_TYPE_KEY = "element_type";
    public static final String ACCUM_MULTIPLIER_KEY = "accum_multiplier";

    public static ItemStack withElement(ItemStack stack, ElementType type) {
        return withElementAndAccum(stack, type, 1.0f);
    }

    public static ItemStack withElementAndAccum(ItemStack stack, ElementType type, float accumMultiplier) {
        if (stack == null || stack.isEmpty() || type == null) return stack;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ELEMENT_TYPE_KEY, type.name());
        tag.putFloat(ACCUM_MULTIPLIER_KEY, accumMultiplier);

        return stack;
    }

    public static Optional<ElementType> getElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_TYPE_KEY)) return Optional.empty();

        String typeName = tag.getString(ELEMENT_TYPE_KEY);
        try {
            return Optional.of(ElementType.valueOf(typeName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static float getAccumMultiplier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0f;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ACCUM_MULTIPLIER_KEY)) return 1.0f;

        return tag.getFloat(ACCUM_MULTIPLIER_KEY);
    }

    public static boolean hasElement(ItemStack stack) {
        return getElement(stack).isPresent();
    }

    public static ItemStack removeElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(ELEMENT_TYPE_KEY);
            tag.remove(ACCUM_MULTIPLIER_KEY);
        }

        return stack;
    }
}
 * ============================================================================
 */