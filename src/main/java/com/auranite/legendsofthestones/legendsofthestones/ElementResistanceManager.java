package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElementResistanceManager {

	private static final Map<EntityType<?>, Map<ElementType, Resistance>> ENTITY_RESISTANCES = new ConcurrentHashMap<>();
	private static final Map<TagKey<EntityType<?>>, Boolean> PROCESSED_TAGS = new ConcurrentHashMap<>();

	private ElementResistanceManager() {}

	// ═══════════════════════════════════════════════════════════
	// РЕГИСТРАЦИЯ
	// ═══════════════════════════════════════════════════════════

	public static void registerResistance(EntityType<?> entityType, Map<ElementType, Resistance> resistanceMap) {
		if (entityType == null || resistanceMap == null || resistanceMap.isEmpty()) return;

		Map<ElementType, Resistance> existing = ENTITY_RESISTANCES.computeIfAbsent(
				entityType, k -> new EnumMap<>(ElementType.class)
		);
		existing.putAll(resistanceMap);
	}

	public static void loadFromTag(ElementType elementType, TagKey<EntityType<?>> tag,
								   Resistance resistance, net.minecraft.core.HolderLookup.Provider lookupProvider) {
		if (elementType == null || tag == null || resistance == null || lookupProvider == null) return;

		PROCESSED_TAGS.putIfAbsent(tag, true);
		var entityLookup = lookupProvider.lookupOrThrow(Registries.ENTITY_TYPE);

		entityLookup.get(tag).ifPresent(tagged -> {
			for (var holder : tagged) {
				EntityType<?> entityType = holder.value();
				if (entityType == null) continue;

				Map<ElementType, Resistance> resistanceMap = ENTITY_RESISTANCES
						.computeIfAbsent(entityType, k -> new EnumMap<>(ElementType.class));
				resistanceMap.put(elementType, resistance);
			}
		});
	}

	// ═══════════════════════════════════════════════════════════
	// ЛЕНИВАЯ ЗАГРУЗКА
	// ═══════════════════════════════════════════════════════════

	private static void tryLazyLoadFromTags(EntityType<?> entityType, ElementType elementType) {
		if (entityType == null || elementType == null) return;

		String elementLower = elementType.name().toLowerCase();
		String modid = LegendsOfTheStones.MODID;

		if (entityType.is(createTag(modid, elementLower, "immune"))) {
			registerResistance(entityType, Map.of(elementType, Resistance.IMMUNE));
			return;
		}
		if (entityType.is(createTag(modid, elementLower, "resistance"))) {
			registerResistance(entityType, Map.of(elementType, Resistance.HALF_RESIST));
			return;
		}
		if (entityType.is(createTag(modid, elementLower, "weakness"))) {
			registerResistance(entityType, Map.of(elementType, Resistance.WEAKNESS));
		}
	}

	private static TagKey<EntityType<?>> createTag(String modid, String element, String modifier) {
		return TagKey.create(Registries.ENTITY_TYPE,
				ResourceLocation.fromNamespaceAndPath(modid, "element/" + element + "/" + modifier));
	}

	// ═══════════════════════════════════════════════════════════
	// ПОЛУЧЕНИЕ СОПРОТИВЛЕНИЙ
	// ═══════════════════════════════════════════════════════════

	public static Resistance getResistance(Entity entity, ElementType type) {
		if (entity == null || type == null) return Resistance.ZERO;
		return getResistance(entity.getType(), type);
	}

	public static Resistance getResistance(EntityType<?> entityType, ElementType type) {
		if (entityType == null || type == null) return Resistance.ZERO;

		Map<ElementType, Resistance> typeMap = ENTITY_RESISTANCES.get(entityType);

		if (typeMap == null || !typeMap.containsKey(type)) {
			tryLazyLoadFromTags(entityType, type);
			typeMap = ENTITY_RESISTANCES.get(entityType);
		}

		if (typeMap == null) return Resistance.ZERO;

		Resistance res = typeMap.get(type);
		return res != null ? res : Resistance.ZERO;
	}

	// ═══════════════════════════════════════════════════════════
	// УТИЛИТЫ
	// ═══════════════════════════════════════════════════════════

	public static int calculateAccumulationPoints(Entity entity, ElementType type, int basePoints) {
		Resistance resistance = getResistance(entity, type);
		float multiplier = Math.max(0f, 1f - resistance.accumulationResistance());
		return Math.round(basePoints * multiplier);
	}

	public static float calculateReducedDamage(Entity entity, ElementType type, float baseDamage) {
		Resistance resistance = getResistance(entity, type);
		float multiplier = Math.max(0f, 1f - resistance.damageResistance());
		return Math.max(0f, baseDamage * multiplier);
	}

	public static boolean isImmune(Entity entity, ElementType type) {
		return getResistance(entity, type).isImmune();
	}

	public static boolean isWeakness(Entity entity, ElementType type) {
		return getResistance(entity, type).isWeakness();
	}

	/**
	 * Проверка: есть ли ЛЮБЫЕ сопротивления у типа сущности
	 */
	public static boolean hasResistanceFor(EntityType<?> entityType) {
		return entityType != null && ENTITY_RESISTANCES.containsKey(entityType);
	}

	/**
	 * Проверка: есть ли сопротивление конкретного элемента у сущности (Entity)
	 */
	public static boolean hasResistanceFor(Entity entity, ElementType type) {
		if (entity == null || type == null) return false;
		return hasResistanceFor(entity.getType(), type);
	}

	/**
	 * Проверка: есть ли сопротивление конкретного элемента у типа сущности (EntityType)
	 * <-- ДОБАВЛЕНО: Этого метода не хватало
	 */
	public static boolean hasResistanceFor(EntityType<?> entityType, ElementType type) {
		if (entityType == null || type == null) return false;

		// Проверяем кеш
		Map<ElementType, Resistance> typeMap = ENTITY_RESISTANCES.get(entityType);
		if (typeMap != null && typeMap.containsKey(type)) {
			Resistance res = typeMap.get(type);
			return res != null && res != Resistance.ZERO;
		}

		// Если не в кеше, пробуем ленивую загрузку
		tryLazyLoadFromTags(entityType, type);
		typeMap = ENTITY_RESISTANCES.get(entityType);

		if (typeMap == null) return false;
		Resistance res = typeMap.get(type);
		return res != null && res != Resistance.ZERO;
	}

	// ═══════════════════════════════════════════════════════════
	// УПРАВЛЕНИЕ КЕШЕМ
	// ═══════════════════════════════════════════════════════════

	public static void clearAllResistances() {
		ENTITY_RESISTANCES.clear();
		PROCESSED_TAGS.clear();
	}

	public static int getRegisteredEntityCount() {
		return ENTITY_RESISTANCES.size();
	}

	public static int getTotalResistanceEntries() {
		return ENTITY_RESISTANCES.values().stream().mapToInt(Map::size).sum();
	}

	public static void debugPrintRegistry() {
		LegendsOfTheStones.LOGGER.info("=== RESISTANCE REGISTRY ===");
		LegendsOfTheStones.LOGGER.info("Entities: {}, Entries: {}",
				getRegisteredEntityCount(), getTotalResistanceEntries());
	}

	// ═══════════════════════════════════════════════════════════
	// RECORD: Resistance
	// ═══════════════════════════════════════════════════════════

	public record Resistance(float accumulationResistance, float damageResistance) {
		public static final Resistance ZERO = new Resistance(0.0f, 0.0f);
		public static final Resistance IMMUNE = new Resistance(1.0f, 1.0f);
		public static final Resistance HALF_RESIST = new Resistance(0.5f, 0.5f);
		public static final Resistance WEAKNESS = new Resistance(0.0f, -0.5f);

		public boolean isImmune() { return accumulationResistance >= 1.0f && damageResistance >= 1.0f; }
		public boolean isWeakness() { return accumulationResistance < 0f || damageResistance < 0f; }
		public float getAccumulationMultiplier() { return Math.max(0f, 1f - accumulationResistance); }
		public float getDamageMultiplier() { return Math.max(0f, 1f - damageResistance); }
	}
}