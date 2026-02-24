package com.auranite.legendsofthestones;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = LegendsOfTheStones.MODID)
public class ElementDamageHandler {

	// === КОНФИГУРАЦИЯ ===
	private static float baseAccumulation = 1.0f;
	private static final int THRESHOLD = 100;
	private static final int RESET_DELAY_TICKS = 300;

	// === КЭШИ И СОСТОЯНИЯ ===
	private static final Map<Integer, Long> DAMAGE_COOLDOWNS = new ConcurrentHashMap<>();
	private static final int COOLDOWN_TICKS = 5;

	// ✅ ИСПРАВЛЕНО: Используем synchronized для потокобезопасности вложенных мап
	private static final Map<Integer, Map<ElementType, Long>> LAST_DAMAGE_TIME = new ConcurrentHashMap<>();
	private static final Object LAST_DAMAGE_LOCK = new Object();

	private static MinecraftServer currentServer = null;
	private static int serverTickCounter = 0;
	private static final int CLEANUP_INTERVAL = 100;

	// === ССЫЛКА НА МЕНЕДЖЕР ОТОБРАЖЕНИЯ ===
	private static ElementDamageDisplayManager displayManager;

	// ✅ НОВОЕ: Защита от рекурсивного урона
	private static final ThreadLocal<Boolean> IS_PROCESSING_DAMAGE = ThreadLocal.withInitial(() -> false);

	// ✅ НОВОЕ: Лимит активных дисплеев для предотвращения лагов
	private static final int MAX_ACTIVE_DISPLAYS = 500;
	private static int currentDisplayCount = 0;
	private static final Object DISPLAY_COUNT_LOCK = new Object();

	public static void setDisplayManager(ElementDamageDisplayManager manager) {
		displayManager = manager;
	}

	// === ИНИЦИАЛИЗАЦИЯ ЦВЕТОВ ===
	public static void initDamageColors() {
		ElementDamageDisplayManager.registerDamageColor(ElementType.FIRE, 0xFF5500);
		ElementDamageDisplayManager.registerDamageColor(ElementType.PHYSICAL, 0xFFAA00);
		ElementDamageDisplayManager.registerDamageColor(ElementType.WIND, 0x00FFFF);
		ElementDamageDisplayManager.registerDamageColor(ElementType.WATER, 0x0080FF);
		ElementDamageDisplayManager.registerDamageColor(ElementType.EARTH, 0x8B4513);
		ElementDamageDisplayManager.registerDamageColor(ElementType.ICE, 0x00BFFF);
		ElementDamageDisplayManager.registerDamageColor(ElementType.ELECTRIC, 0x9932CC);
		ElementDamageDisplayManager.registerDamageColor(ElementType.SOURCE, 0xFF5C77);
		ElementDamageDisplayManager.registerDamageColor(ElementType.NATURAL, 0x32CD32);
		ElementDamageDisplayManager.registerDamageColor(ElementType.QUANTUM, 0x9400D3);
	}

	// === УПРАВЛЕНИЕ СЧЁТЧИКОМ ДИСПЛЕЕВ ===
	public static boolean canSpawnDisplay() {
		synchronized (DISPLAY_COUNT_LOCK) {
			return currentDisplayCount < MAX_ACTIVE_DISPLAYS;
		}
	}

	public static void incrementDisplayCount() {
		synchronized (DISPLAY_COUNT_LOCK) {
			currentDisplayCount++;
		}
	}

	public static void decrementDisplayCount() {
		synchronized (DISPLAY_COUNT_LOCK) {
			currentDisplayCount = Math.max(0, currentDisplayCount - 1);
		}
	}

	public static int getCurrentDisplayCount() {
		synchronized (DISPLAY_COUNT_LOCK) {
			return currentDisplayCount;
		}
	}

	// === СОБЫТИЯ ===
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Pre event) {
		currentServer = event.getServer();

		if (displayManager != null) {
			displayManager.processPendingRemovals();
		}

		serverTickCounter++;
		if (serverTickCounter >= CLEANUP_INTERVAL) {
			serverTickCounter = 0;
			checkAndResetInactivePoints();
			if (displayManager != null) {
				displayManager.cleanupStaleDisplays();
			}
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingDamageEvent.Pre event) {
		// ✅ ЗАЩИТА ОТ РЕКУРСИИ
		if (IS_PROCESSING_DAMAGE.get()) return;
		IS_PROCESSING_DAMAGE.set(true);

		try {
			processLivingHurt(event);
		} finally {
			IS_PROCESSING_DAMAGE.set(false);
		}
	}

	// Вынесена основная логика для чистоты кода
	private static void processLivingHurt(LivingDamageEvent.Pre event) {
		LivingEntity target = event.getEntity();
		LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity e ? e : null;

		// ========================================================================
		// 🔹 ОБРАБОТКА ЭФФЕКТОВ: SHOCK / RIFT / BLOOM
		// ========================================================================

		float damage = event.getNewDamage();

		if (attacker != null && attacker.hasEffect(LegendsOfTheStonesMobEffects.SHOCK)) {
			int amplifier = attacker.getEffect(LegendsOfTheStonesMobEffects.SHOCK).getAmplifier();
			float reduction = 1.0f - ((amplifier + 1) * 0.10f);
			reduction = Math.max(0.1f, reduction);
			damage *= reduction;
			LegendsOfTheStones.LOGGER.debug("SHOCK on attacker {}: x{} multiplier applied",
					attacker.getName().getString(), reduction);
		}

		if (target.hasEffect(LegendsOfTheStonesMobEffects.RIFT)) {
			int amplifier = target.getEffect(LegendsOfTheStonesMobEffects.RIFT).getAmplifier();
			float multiplier = 1.0f + (amplifier + 1) * 0.25f;
			damage *= multiplier;
			LegendsOfTheStones.LOGGER.debug("RIFT on target {}: x{} multiplier applied",
					target.getName().getString(), multiplier);
		}

		if (target.hasEffect(LegendsOfTheStonesMobEffects.BLOOM)) {
			damage *= 1.25f;
			LegendsOfTheStones.LOGGER.debug("BLOOM on target {}: +25% universal vulnerability",
					target.getName().getString());
		}

		event.setNewDamage(damage);

		// ========================================================================
		// 🔹 ЛОГИКА ЭЛЕМЕНТАЛЬНОГО УРОНА
		// ========================================================================

		DamageSource source = event.getSource();
		ElementType type = getElementTypeFromSource(source);

		if (type == null) {
			if (canShowDamage(target)) {
				spawnDamageNumber(target, event.getNewDamage(), null);
			}
			return;
		}

		// === РАСЧЁТ МНОЖИТЕЛЯ НАКОПЛЕНИЯ ===
		float effectiveAccumMultiplier = 1.0f;

		if (source.getDirectEntity() != null) {
			Optional<Float> projectileAccum = ElementalProjectileRegistry.getAccumulationMultiplierForEntity(source.getDirectEntity());
			if (projectileAccum.isPresent()) {
				effectiveAccumMultiplier = projectileAccum.get();
				LegendsOfTheStones.LOGGER.debug("Using projectile accum multiplier: x{} for {}",
						effectiveAccumMultiplier, source.getDirectEntity().getType());
			}
		}

		if (effectiveAccumMultiplier == 1.0f && source.getEntity() instanceof LivingEntity attackerEntity) {
			ItemStack weapon = attackerEntity.getMainHandItem();
			float weaponAccum = ElementalWeaponRegistry.getAccumulationMultiplier(weapon);
			float componentAccum = ElementalWeaponComponent.getAccumMultiplier(weapon);
			if (componentAccum != 1.0f) {
				effectiveAccumMultiplier = componentAccum;
			} else if (weaponAccum != 1.0f) {
				effectiveAccumMultiplier = weaponAccum;
			}
		}

		// ✅ BLOOM: +25% к накоплению
		if (target.hasEffect(LegendsOfTheStonesMobEffects.BLOOM)) {
			effectiveAccumMultiplier *= 1.25f;
			LegendsOfTheStones.LOGGER.debug("BLOOM on target {}: +25% accumulation (total x{})",
					target.getName().getString(), effectiveAccumMultiplier);
		}

		// ✅ WETNESS: +100% к накоплению за уровень (для ЛЮБОГО элемента)
		if (target.hasEffect(LegendsOfTheStonesMobEffects.WETNESS)) {
			int amplifier = target.getEffect(LegendsOfTheStonesMobEffects.WETNESS).getAmplifier();
			float wetnessAccumBonus = 1.0f + (amplifier + 1) * 1.0f;
			effectiveAccumMultiplier *= wetnessAccumBonus;
			LegendsOfTheStones.LOGGER.debug("WETNESS on target {}: accum x{} (total x{})",
					target.getName().getString(), wetnessAccumBonus, effectiveAccumMultiplier);
		}

		ElementResistanceManager.Resistance resistance = ElementResistanceManager.getResistance(target, type);
		LegendsOfTheStones.LOGGER.debug("Resistance check: {} vs {} → {}", target.getType(), type, resistance);

		if (ElementResistanceManager.isImmune(target, type)) {
			event.setNewDamage(0f);
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {}! Damage set to 0",
					target.getName().getString(), type);
			return;
		}

		int basePoints = (int) baseAccumulation;
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(target, type, basePoints);
		pointsToAdd = Math.round(pointsToAdd * effectiveAccumMultiplier);

		int pointsBefore = LegendsOfTheStonesAttachments.getPoints(target, type);
		LegendsOfTheStonesAttachments.addPoints(target, type, pointsToAdd);
		int pointsAfter = LegendsOfTheStonesAttachments.getPoints(target, type);
		boolean thresholdReached = pointsAfter >= THRESHOLD;

		LegendsOfTheStones.LOGGER.info("[Resonance] Target: {} | Element: {} | Multiplier: x{} | Points: {} +{} → {} | Breakthrough: {}",
				target.getName().getString(), type, effectiveAccumMultiplier,
				pointsBefore, pointsToAdd, pointsAfter, thresholdReached);

		float finalDamage = event.getNewDamage();
		finalDamage = ElementResistanceManager.calculateReducedDamage(target, type, finalDamage);

		if (thresholdReached) {
			finalDamage = applyThresholdEffect(target, type, event, finalDamage);
			LegendsOfTheStonesAttachments.resetPoints(target, type);
		}

		event.setNewDamage(finalDamage);

		if (canShowDamage(target)) {
			spawnDamageNumber(target, finalDamage, type);
		}
		updateLastDamageTime(target, type);
	}

	// === ОСТАЛЬНЫЕ СОБЫТИЯ ===
	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		clearActiveDisplays(entity);
		DAMAGE_COOLDOWNS.remove(entity.getId());
		synchronized (LAST_DAMAGE_LOCK) {
			LAST_DAMAGE_TIME.remove(entity.getId());
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity livingEntity) {
			clearActiveDisplays(livingEntity);
			DAMAGE_COOLDOWNS.remove(entity.getId());
			synchronized (LAST_DAMAGE_LOCK) {
				LAST_DAMAGE_TIME.remove(entity.getId());
			}
		}
	}

	// ✅ ИСПРАВЛЕНО: Удаление только данных конкретного игрока, а не глобальная очистка
	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		LegendsOfTheStones.LOGGER.info("Player {} logged out. Cleaning their displays only.", player.getName().getString());

		if (displayManager != null) {
			// ✅ Удаляем только дисплеи, связанные с этим игроком
			displayManager.clearActiveDisplays(player);
		}

		// ✅ Удаляем данные только для этого игрока
		int playerId = player.getId();
		DAMAGE_COOLDOWNS.remove(playerId);
		synchronized (LAST_DAMAGE_LOCK) {
			LAST_DAMAGE_TIME.remove(playerId);
		}

		// ❌ НЕ очищаем глобальные мапы!
	}

	// ✅ ИСПРАВЛЕНО: Не очищаем все дисплеи при выгрузке уровня
	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;

		LegendsOfTheStones.LOGGER.debug("Level {} unloading. Stale displays will be cleaned by periodic cleanup.",
				level.dimension().location());

		// ✅ cleanupStaleDisplays() в тике уже проверяет level() у каждой сущности,
		// поэтому отдельная очистка здесь не нужна и даже вредна
	}

	@SubscribeEvent
	public static void onChunkUnload(ChunkDataEvent.Save event) {
		if (displayManager == null) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		int chunkX = event.getChunk().getPos().x;
		int chunkZ = event.getChunk().getPos().z;
		int markedCount = displayManager.cleanupDisplaysInChunk(level, chunkX, chunkZ);
		if (markedCount > 0) {
			LegendsOfTheStones.LOGGER.debug("Marked {} visual effects for removal from unloading chunk [{}, {}]",
					markedCount, chunkX, chunkZ);
		}
	}

	// === ЛОГИКА ОПРЕДЕЛЕНИЯ ЭЛЕМЕНТА ===
	private static ElementType getElementTypeFromSource(DamageSource source) {
		Entity directEntity = source.getDirectEntity();
		if (directEntity != null) {
			Optional<ElementType> registryElement = ElementalProjectileRegistry.getElementForEntity(directEntity);
			if (registryElement.isPresent()) return registryElement.get();
			if (LegendsOfTheStonesAttachments.hasProjectileElement(directEntity)) {
				return LegendsOfTheStonesAttachments.getProjectileElement(directEntity);
			}
		}
		Entity causingEntity = source.getEntity();
		if (causingEntity instanceof LivingEntity attacker) {
			ItemStack weapon = attacker.getMainHandItem();
			Optional<ElementType> componentType = ElementalWeaponComponent.getElement(weapon);
			if (componentType.isPresent()) return componentType.get();
			ElementType registryType = ElementalWeaponRegistry.getElementType(weapon);
			if (registryType != null) return registryType;
		}
		String msgId = source.type().msgId();
		if (msgId != null) {
			for (ElementType type : ElementType.values()) {
				if (type.getDamageTypeId().equals(msgId) || type.getFullDamageTypeId().equals(msgId)) {
					return type;
				}
			}
			ElementType vanillaType = ElementType.fromVanillaDamageType(msgId);
			if (vanillaType != null) return vanillaType;
		}
		LegendsOfTheStones.LOGGER.debug("No matching ElementType for source: {}", source);
		return null;
	}

	public static ElementType getElementTypeFromItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return null;
		Optional<ElementType> componentType = ElementalWeaponComponent.getElement(stack);
		if (componentType.isPresent()) return componentType.get();
		return ElementalWeaponRegistry.getElementType(stack);
	}

	public static ItemStack createElementalItem(net.minecraft.world.item.Item item, ElementType type, int count) {
		ItemStack stack = new ItemStack(item, count);
		return ElementalWeaponComponent.withElement(stack, type);
	}

	public static ItemStack createElementalItemWithAccum(net.minecraft.world.item.Item item, ElementType type, int count, float accumMultiplier) {
		ItemStack stack = new ItemStack(item, count);
		return ElementalWeaponComponent.withElementAndAccum(stack, type, accumMultiplier);
	}

	// === УПРАВЛЕНИЕ ВРЕМЕНЕМ ===
	// ✅ ИСПРАВЛЕНО: Синхронизация доступа к вложенным мап
	private static void updateLastDamageTime(LivingEntity entity, ElementType type) {
		int entityId = entity.getId();
		long gameTime = entity.level().getGameTime();

		synchronized (LAST_DAMAGE_LOCK) {
			LAST_DAMAGE_TIME
					.computeIfAbsent(entityId, k -> new EnumMap<>(ElementType.class))
					.put(type, gameTime);
		}
	}

	private static void checkAndResetInactivePoints() {
		if (currentServer == null) return;
		long currentTime = currentServer.overworld().getGameTime();

		// ✅ Синхронизируем итерацию по LAST_DAMAGE_TIME
		synchronized (LAST_DAMAGE_LOCK) {
			Iterator<Map.Entry<Integer, Map<ElementType, Long>>> entityIterator = LAST_DAMAGE_TIME.entrySet().iterator();
			while (entityIterator.hasNext()) {
				Map.Entry<Integer, Map<ElementType, Long>> entityEntry = entityIterator.next();
				int entityId = entityEntry.getKey();
				Map<ElementType, Long> typeTimes = entityEntry.getValue();
				LivingEntity livingEntity = null;
				for (ServerLevel level : currentServer.getAllLevels()) {
					Entity entity = level.getEntity(entityId);
					if (entity instanceof LivingEntity le && le.isAlive()) {
						livingEntity = le;
						break;
					}
				}
				if (livingEntity == null) { entityIterator.remove(); continue; }
				Iterator<Map.Entry<ElementType, Long>> typeIterator = typeTimes.entrySet().iterator();
				while (typeIterator.hasNext()) {
					Map.Entry<ElementType, Long> typeEntry = typeIterator.next();
					ElementType type = typeEntry.getKey();
					long lastTime = typeEntry.getValue();
					if (currentTime - lastTime >= RESET_DELAY_TICKS) {
						int pointsBefore = LegendsOfTheStonesAttachments.getPoints(livingEntity, type);
						if (pointsBefore > 0) {
							LegendsOfTheStonesAttachments.resetPoints(livingEntity, type);
							LegendsOfTheStones.LOGGER.debug("Reset {} points for {} (inactive for {} ticks)", pointsBefore, type, RESET_DELAY_TICKS);
						}
						typeIterator.remove();
					}
				}
				if (typeTimes.isEmpty()) entityIterator.remove();
			}
		}
	}

	private static boolean canShowDamage(LivingEntity entity) {
		int entityId = entity.getId();
		long currentTime = entity.level().getGameTime();
		if (DAMAGE_COOLDOWNS.containsKey(entityId)) {
			long lastTime = DAMAGE_COOLDOWNS.get(entityId);
			if (currentTime - lastTime < COOLDOWN_TICKS) return false;
		}
		DAMAGE_COOLDOWNS.put(entityId, currentTime);
		return true;
	}

	// === ВИЗУАЛИЗАЦИЯ ===
	private static void clearActiveDisplays(LivingEntity entity) {
		if (displayManager != null) displayManager.clearActiveDisplays(entity);
	}

	private static void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
		// ✅ ПРОВЕРКА ЛИМИТА ДИСПЛЕЕВ
		if (!canSpawnDisplay()) {
			LegendsOfTheStones.LOGGER.debug("Display limit reached ({}), skipping damage number for {}",
					MAX_ACTIVE_DISPLAYS, entity.getName().getString());
			return;
		}

		if (displayManager != null) {
			incrementDisplayCount();
			displayManager.spawnDamageNumber(entity, amount, type);
		}
	}

	public static void spawnStatusText(LivingEntity entity, Component textComponent, int color) {
		if (!canSpawnDisplay()) return;
		if (displayManager != null) {
			incrementDisplayCount();
			displayManager.spawnStatusText(entity, textComponent, color);
		}
	}

	public static void spawnStatusText(LivingEntity entity, String text, int color) {
		spawnStatusText(entity, Component.literal(text), color);
	}

	// === ПОРОГОВЫЕ ЭФФЕКТЫ ===
	public static int getThreshold() { return THRESHOLD; }
	public static void setThreshold(int threshold) {
		LegendsOfTheStones.LOGGER.warn("setThreshold() deprecated - all types use THRESHOLD = 100");
	}
	public static void setDamageColor(ElementType type, int color) {
		ElementDamageDisplayManager.setDamageColor(type, color);
	}
	public static Map<ElementType, Integer> getAllDamageColors() {
		return ElementDamageDisplayManager.getAllDamageColors();
	}

	private static float applyThresholdEffect(LivingEntity target, ElementType type, LivingDamageEvent.Pre event, float currentDamage) {
		LegendsOfTheStones.LOGGER.info("THRESHOLD REACHED! Entity: {}, Type: {}", target.getName().getString(), type);
		return switch (type) {
			case FIRE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BURNING, 200, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.overheating"), 0xFF5500);
				yield currentDamage;
			}
			case PHYSICAL -> {
				spawnStatusText(target, Component.translatable("elemental.tooltip.crit_dmg"), 0xFFAA00);
				yield currentDamage * 5.0f;
			}
			case WIND -> {
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.wind_whirlwind"), 0x00FFFF);
				yield currentDamage;
			}
			case WATER -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.WETNESS, 300, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.water_flood"), 0x0080FF);
				yield currentDamage;
			}
			case EARTH -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.STUN, 60, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.earth_petrify"), 0x8B4513);
				yield currentDamage;
			}
			case ICE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.FREEZE, 160, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.ice_freeze"), 0x00BFFF);
				yield currentDamage;
			}
			case ELECTRIC -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.SHOCK, 140, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.electric_shock"), 0xFFFF00);
				yield currentDamage;
			}
			case SOURCE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.RIFT, 100, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.source_void"), 0x9932CC);
				yield currentDamage;
			}
			case NATURAL -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BLOOM, 120, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.natural_bloom"), 0x32CD32);
				yield currentDamage;
			}
			case QUANTUM -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BREAK, 100, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.quantum_flux"), 0xFF00FF);
				yield currentDamage;
			}
			default -> currentDamage;
		};
	}

	private static float applyThresholdEffectWithDamage(LivingEntity target, ElementType type, float originalDamage) {
		LegendsOfTheStones.LOGGER.info("THRESHOLD REACHED! Entity: {}, Type: {}", target.getName().getString(), type);
		return switch (type) {
			case FIRE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BURNING, 200, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.overheating"), 0xFF5500);
				yield originalDamage;
			}
			case PHYSICAL -> {
				spawnStatusText(target, Component.translatable("elemental.tooltip.crit_dmg"), 0xFFAA00);
				yield originalDamage * 5.0f;
			}
			case WIND -> {
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.wind_whirlwind"), 0x00FFFF);
				yield originalDamage;
			}
			case WATER -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.WETNESS, 300, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.water_flood"), 0x0080FF);
				yield originalDamage;
			}
			case EARTH -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.STUN, 60, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.earth_petrify"), 0x8B4513);
				yield originalDamage;
			}
			case ICE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.FREEZE, 160, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.ice_freeze"), 0x00BFFF);
				yield originalDamage;
			}
			case ELECTRIC -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.SHOCK, 140, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.electric_shock"), 0xFFFF00);
				yield originalDamage;
			}
			case SOURCE -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.RIFT, 100, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.source_void"), 0x9932CC);
				yield originalDamage;
			}
			case NATURAL -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BLOOM, 120, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.natural_bloom"), 0x32CD32);
				yield originalDamage;
			}
			case QUANTUM -> {
				target.addEffect(new MobEffectInstance(LegendsOfTheStonesMobEffects.BREAK, 100, 0, false, true));
				spawnStatusText(target, Component.translatable("elemental.tooltip.quantum_flux"), 0xFF00FF);
				yield originalDamage;
			}
			default -> originalDamage;
		};
	}

	// === ПУБЛИЧНЫЙ API ===
	public static void setBaseAccumulation(float value) { baseAccumulation = value; }
	public static float getBaseAccumulation() { return baseAccumulation; }

	public static void dealElementDamage(Entity target, ElementType type, float amount) {
		dealElementDamage(target, type, amount, 0);
	}

	// ✅ ИСПРАВЛЕНО: Защита от рекурсии в публичном API
	public static void dealElementDamage(Entity target, ElementType type, float amount, int accumulationPoints) {
		if (IS_PROCESSING_DAMAGE.get()) return;
		IS_PROCESSING_DAMAGE.set(true);

		try {
			processDealElementDamage(target, type, amount, accumulationPoints);
		} finally {
			IS_PROCESSING_DAMAGE.set(false);
		}
	}

	private static void processDealElementDamage(Entity target, ElementType type, float amount, int accumulationPoints) {
		if (!(target.level() instanceof ServerLevel serverLevel)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamage: not server level"); return;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamage: target is not LivingEntity"); return;
		}
		if (ElementResistanceManager.isImmune(target, type)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (manual call)!", target.getName().getString(), type); return;
		}
		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, type.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {} - урон НЕ будет нанесён!", rl); return;
		}
		DamageSource source = new DamageSource(damageTypeHolder.get());
		float finalDamage = amount;
		finalDamage = ElementResistanceManager.calculateReducedDamage(livingTarget, type, finalDamage);

		float weaponAccumMultiplier = 1.0f;
		int basePoints;
		if (accumulationPoints < 0) {
			weaponAccumMultiplier = Math.abs(accumulationPoints);
			basePoints = (int) baseAccumulation;
		} else {
			basePoints = (accumulationPoints > 0) ? accumulationPoints : (int) baseAccumulation;
		}
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(livingTarget, type, basePoints);
		pointsToAdd = Math.round(pointsToAdd * weaponAccumMultiplier);

		if (pointsToAdd > 0) {
			int pointsBefore = LegendsOfTheStonesAttachments.getPoints(livingTarget, type);
			LegendsOfTheStonesAttachments.addPoints(livingTarget, type, pointsToAdd);
			int pointsAfter = LegendsOfTheStonesAttachments.getPoints(livingTarget, type);
			boolean thresholdReached = pointsAfter >= THRESHOLD;
			LegendsOfTheStones.LOGGER.info("[Manual] Target: {} | Element: {} | WeaponAccum: x{} | Points: {} +{} → {} | Threshold: {} | Reached: {}",
					livingTarget.getName().getString(), type, weaponAccumMultiplier, pointsBefore, pointsToAdd, pointsAfter, THRESHOLD, thresholdReached);
			if (thresholdReached) {
				finalDamage = applyThresholdEffectWithDamage(livingTarget, type, amount);
				LegendsOfTheStonesAttachments.resetPoints(livingTarget, type);
				LegendsOfTheStones.LOGGER.info("[Manual] THRESHOLD TRIGGERED! Reset points for {} on {}", type, livingTarget.getName().getString());
			}
			if (canShowDamage(livingTarget)) spawnDamageNumber(livingTarget, finalDamage, type);
		} else {
			if (canShowDamage(livingTarget)) spawnDamageNumber(livingTarget, finalDamage, type);
		}
		target.hurt(source, finalDamage);
		updateLastDamageTime(livingTarget, type);
	}

	public static void dealElementDamageWithAccum(Entity target, ElementType type, float amount, float accumMultiplier) {
		if (IS_PROCESSING_DAMAGE.get()) return;
		IS_PROCESSING_DAMAGE.set(true);

		try {
			processDealElementDamageWithAccum(target, type, amount, accumMultiplier);
		} finally {
			IS_PROCESSING_DAMAGE.set(false);
		}
	}

	private static void processDealElementDamageWithAccum(Entity target, ElementType type, float amount, float accumMultiplier) {
		if (!(target.level() instanceof ServerLevel serverLevel)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamageWithAccum: not server level"); return;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamageWithAccum: target is not LivingEntity"); return;
		}
		if (ElementResistanceManager.isImmune(target, type)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (manual call)!", target.getName().getString(), type); return;
		}
		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, type.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {}", rl); return;
		}
		DamageSource source = new DamageSource(damageTypeHolder.get());
		float finalDamage = amount;
		finalDamage = ElementResistanceManager.calculateReducedDamage(livingTarget, type, finalDamage);
		int basePoints = (int) baseAccumulation;
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(livingTarget, type, basePoints);
		pointsToAdd = Math.round(pointsToAdd * accumMultiplier);
		if (pointsToAdd > 0) {
			int pointsBefore = LegendsOfTheStonesAttachments.getPoints(livingTarget, type);
			LegendsOfTheStonesAttachments.addPoints(livingTarget, type, pointsToAdd);
			int pointsAfter = LegendsOfTheStonesAttachments.getPoints(livingTarget, type);
			boolean thresholdReached = pointsAfter >= THRESHOLD;
			LegendsOfTheStones.LOGGER.info("[Manual] Target: {} | Element: {} | WeaponAccum: x{} | Points: {} +{} → {} | Threshold: {} | Reached: {}",
					livingTarget.getName().getString(), type, accumMultiplier, pointsBefore, pointsToAdd, pointsAfter, THRESHOLD, thresholdReached);
			if (thresholdReached) {
				finalDamage = applyThresholdEffectWithDamage(livingTarget, type, amount);
				LegendsOfTheStonesAttachments.resetPoints(livingTarget, type);
				LegendsOfTheStones.LOGGER.info("[Manual] THRESHOLD TRIGGERED! Reset points for {} on {}", type, livingTarget.getName().getString());
			}
			if (canShowDamage(livingTarget)) spawnDamageNumber(livingTarget, finalDamage, type);
		} else {
			if (canShowDamage(livingTarget)) spawnDamageNumber(livingTarget, finalDamage, type);
		}
		target.hurt(source, finalDamage);
		updateLastDamageTime(livingTarget, type);
	}

	public static void addElementPoints(LivingEntity entity, ElementType type, int points) {
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(entity, type, points);
		LegendsOfTheStonesAttachments.addPoints(entity, type, pointsToAdd);
		updateLastDamageTime(entity, type);
	}
	public static int getElementPoints(LivingEntity entity, ElementType type) {
		return LegendsOfTheStonesAttachments.getPoints(entity, type);
	}
	public static void resetElementPoints(LivingEntity entity, ElementType type) {
		LegendsOfTheStonesAttachments.resetPoints(entity, type);
		synchronized (LAST_DAMAGE_LOCK) {
			LAST_DAMAGE_TIME.computeIfPresent(entity.getId(), (id, map) -> {
				map.remove(type); return map.isEmpty() ? null : map;
			});
		}
	}
	public static void resetAllElementPoints(LivingEntity entity) {
		for (ElementType type : ElementType.values()) LegendsOfTheStonesAttachments.resetPoints(entity, type);
		synchronized (LAST_DAMAGE_LOCK) {
			LAST_DAMAGE_TIME.remove(entity.getId());
		}
	}
	public static int getAccumulationProgress(LivingEntity entity, ElementType type) {
		int points = getElementPoints(entity, type);
		return THRESHOLD > 0 ? (points * 100) / THRESHOLD : 0;
	}
	public static ElementResistanceManager.Resistance getEntityResistance(Entity entity, ElementType type) {
		return ElementResistanceManager.getResistance(entity, type);
	}

	// === УТИЛИТЫ ДЛЯ СНАРЯДОВ ===
	public static void markProjectileAsElemental(Entity projectile, ElementType type) {
		if (projectile != null && !projectile.level().isClientSide) {
			LegendsOfTheStonesAttachments.setProjectileElement(projectile, type);
			LegendsOfTheStones.LOGGER.debug("Marked projectile {} as {}", projectile.getId(), type);
		}
	}

	public static void applyElementalDamageInstant(Entity target, Entity source, ElementType elementalType, float baseDamage, float accumMultiplier) {
		if (IS_PROCESSING_DAMAGE.get()) return;
		IS_PROCESSING_DAMAGE.set(true);

		try {
			processApplyElementalDamageInstant(target, source, elementalType, baseDamage, accumMultiplier);
		} finally {
			IS_PROCESSING_DAMAGE.set(false);
		}
	}

	private static void processApplyElementalDamageInstant(Entity target, Entity source, ElementType elementalType, float baseDamage, float accumMultiplier) {
		if (!(target.level() instanceof ServerLevel serverLevel) || !(target instanceof LivingEntity livingTarget)) return;
		if (ElementResistanceManager.isImmune(target, elementalType)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (instant)", target.getName().getString(), elementalType); return;
		}
		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, elementalType.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {}", rl); return;
		}
		DamageSource dmgSource = new DamageSource(damageTypeHolder.get(), source, source);
		float finalDamage = ElementResistanceManager.calculateReducedDamage(livingTarget, elementalType, baseDamage);
		int basePoints = (int) baseAccumulation;
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(livingTarget, elementalType, basePoints);
		pointsToAdd = Math.round(pointsToAdd * accumMultiplier);
		if (pointsToAdd > 0) {
			int before = LegendsOfTheStonesAttachments.getPoints(livingTarget, elementalType);
			LegendsOfTheStonesAttachments.addPoints(livingTarget, elementalType, pointsToAdd);
			int after = LegendsOfTheStonesAttachments.getPoints(livingTarget, elementalType);
			boolean thresholdReached = after >= THRESHOLD;
			LegendsOfTheStones.LOGGER.info("[Instant] {} | {} | x{} | {} +{} → {} | Breakthrough: {}",
					livingTarget.getName().getString(), elementalType, accumMultiplier, before, pointsToAdd, after, thresholdReached);
			if (thresholdReached) {
				finalDamage = applyThresholdEffectWithDamage(livingTarget, elementalType, baseDamage);
				LegendsOfTheStonesAttachments.resetPoints(livingTarget, elementalType);
			}
		}
		if (canShowDamage(livingTarget)) spawnDamageNumber(livingTarget, finalDamage, elementalType);
		target.hurt(dmgSource, finalDamage);
		updateLastDamageTime(livingTarget, elementalType);
	}
}