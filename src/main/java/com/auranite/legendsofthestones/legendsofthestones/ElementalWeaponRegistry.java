package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 🔹 РЕЕСТР ЭЛЕМЕНТАЛЬНОГО ОРУЖИЯ 🔹
 *
 * Позволяет привязывать ElementType и множитель накопления к предметам.
 * 🔹 ЛЮБОЙ предмет или пустая рука по умолчанию наносят PHYSICAL урон 🔹
 * 🔹 Без сортировки предметов по классам — максимальная простота 🔹
 */
public class ElementalWeaponRegistry {

	private static final Map<Item, WeaponData> WEAPON_DATA = new WeakHashMap<>();

	private ElementalWeaponRegistry() {}

	/**
	 * Регистрирует предмет как элементальное оружие с кастомным накоплением.
	 */
	public static void registerWeapon(Item item, ElementType type, float accumulationMultiplier) {
		if (item == null || type == null) return;
		WEAPON_DATA.put(item, new WeaponData(type, Math.max(0f, accumulationMultiplier)));
		LegendsOfTheStones.LOGGER.debug("⚔️ Registered elemental weapon: {} → {} (accum: x{})",
				item.getDescriptionId(), type, accumulationMultiplier);
	}

	/**
	 * Регистрирует предмет со стандартным накоплением (множитель 1.0).
	 */
	public static void registerWeapon(Item item, ElementType type) {
		registerWeapon(item, type, 1.0f);
	}

	/**
	 * Получает данные оружия из реестра.
	 */
	public static WeaponData getWeaponData(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return null;
		return WEAPON_DATA.get(stack.getItem());
	}

	/**
	 * 🔹 Получает ElementType предмета для расчёта урона.
	 * @return зарегистрированный ElementType, если предмет есть в реестре
	 * @return ElementType.PHYSICAL в ВСЕХ остальных случаях
	 */
	public static ElementType getElementType(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return ElementType.PHYSICAL;
		}
		WeaponData data = getWeaponData(stack);
		return data != null ? data.type() : ElementType.PHYSICAL;
	}

	/**
	 * Получает множитель накопления предмета.
	 * @return множитель из реестра или 1.0f по умолчанию
	 */
	public static float getAccumulationMultiplier(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return 1.0f;
		WeaponData data = getWeaponData(stack);
		return data != null ? data.accumulationMultiplier() : 1.0f;
	}

	/**
	 * Проверяет, может ли предмет наносить урон.
	 * 🔹 Всегда true — любой предмет или пустая рука наносят PHYSICAL урон 🔹
	 */
	public static boolean canDealDamage(ItemStack stack) {
		return true;
	}

	/**
	 * @deprecated Используйте {@link #canDealDamage(ItemStack)} или {@link #getElementType(ItemStack)}
	 */
	@Deprecated
	public static boolean isElementalWeapon(ItemStack stack) {
		return getElementType(stack) != null;
	}

	/**
	 * Очищает реестр (для тестов).
	 */
	public static void clear() {
		WEAPON_DATA.clear();
	}

	/**
	 * Возвращает количество зарегистрированных предметов.
	 */
	public static int getRegisteredCount() {
		return WEAPON_DATA.size();
	}

	/**
	 * Record для хранения данных оружия.
	 */
	public record WeaponData(ElementType type, float accumulationMultiplier) {
		@Override
		public String toString() {
			return String.format("WeaponData{type=%s, accum=x%.2f}", type, accumulationMultiplier);
		}
	}
}