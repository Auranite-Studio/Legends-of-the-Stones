package com.auranite.legendsofthestones.legendsofthestones;

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
	private static final Map<Integer, Map<ElementType, Long>> LAST_DAMAGE_TIME = new ConcurrentHashMap<>();

	private static MinecraftServer currentServer = null;
	private static int serverTickCounter = 0;
	private static final int CLEANUP_INTERVAL = 100;

	// === ССЫЛКА НА МЕНЕДЖЕР ОТОБРАЖЕНИЯ ===
	private static ElementDamageDisplayManager displayManager;

	public static void setDisplayManager(ElementDamageDisplayManager manager) {
		displayManager = manager;
	}

	// === ИНИЦИАЛИЗАЦИЯ ЦВЕТОВ ===
	public static void initDamageColors() {
		// Базовые элементы
		ElementDamageDisplayManager.registerDamageColor(ElementType.FIRE, 0xFF5500);
		ElementDamageDisplayManager.registerDamageColor(ElementType.PHYSICAL, 0xFFAA00);
		ElementDamageDisplayManager.registerDamageColor(ElementType.WIND, 0x00FFFF);
		ElementDamageDisplayManager.registerDamageColor(ElementType.WATER, 0x0080FF);
		ElementDamageDisplayManager.registerDamageColor(ElementType.EARTH, 0x8B4513);

		// Новые элементы
		ElementDamageDisplayManager.registerDamageColor(ElementType.ICE, 0x00BFFF);       // ❄️ Голубой лёд
		ElementDamageDisplayManager.registerDamageColor(ElementType.ELECTRIC, 0xFFFF00);  // ⚡ Жёлтый разряд
		ElementDamageDisplayManager.registerDamageColor(ElementType.SOURCE, 0x9932CC);    // 🔮 Тёмная орхидея
		ElementDamageDisplayManager.registerDamageColor(ElementType.NATURAL, 0x32CD32);   // 🌿 Лаймовый
		ElementDamageDisplayManager.registerDamageColor(ElementType.QUANTUM, 0xFF00FF);   // ⚛️ Маджента
	}

	// === СОБЫТИЯ ===
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Pre event) {
		currentServer = event.getServer();

		// Обработка отложенных удалений (критично для предотвращения крашей при выгрузке чанков)
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
		LivingEntity target = event.getEntity();
		DamageSource source = event.getSource();
		ElementType type = getElementTypeFromSource(source);

		if (type == null) {
			if (canShowDamage(target)) {
				spawnDamageNumber(target, event.getOriginalDamage(), null);
			}
			return;
		}

		// === РАСЧЁТ МНОЖИТЕЛЯ НАКОПЛЕНИЯ ===
		float effectiveAccumMultiplier = 1.0f;

		// 1. Приоритет: множитель из самого снаряда (ИСПРАВЛЕНО: убрана лямбда)
		if (source.getDirectEntity() != null) {
			Optional<Float> projectileAccum = ElementalProjectileRegistry.getAccumulationMultiplierForEntity(source.getDirectEntity());
			if (projectileAccum.isPresent()) {
				effectiveAccumMultiplier = projectileAccum.get();
				LegendsOfTheStones.LOGGER.debug("Using projectile accum multiplier: x{} for {}", effectiveAccumMultiplier, source.getDirectEntity().getType());
			}
		}

		// 2. Fallback: множитель из оружия атакующего
		if (effectiveAccumMultiplier == 1.0f && source.getEntity() instanceof LivingEntity attacker) {
			ItemStack weapon = attacker.getMainHandItem();
			float weaponAccum = ElementalWeaponRegistry.getAccumulationMultiplier(weapon);
			float componentAccum = ElementalWeaponComponent.getAccumMultiplier(weapon);
			if (componentAccum != 1.0f) {
				effectiveAccumMultiplier = componentAccum;
			} else if (weaponAccum != 1.0f) {
				effectiveAccumMultiplier = weaponAccum;
			}
		}

		ElementResistanceManager.Resistance resistance = ElementResistanceManager.getResistance(target, type);
		LegendsOfTheStones.LOGGER.debug("Resistance check: {} vs {} → {}", target.getType(), type, resistance);

		if (ElementResistanceManager.isImmune(target, type)) {
			event.setNewDamage(0f);
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {}! Damage set to 0", target.getName().getString(), type);
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
				target.getName().getString(), type, effectiveAccumMultiplier, pointsBefore, pointsToAdd, pointsAfter, thresholdReached);

		float finalDamage = event.getOriginalDamage();
		float originalDamage = finalDamage;
		finalDamage = ElementResistanceManager.calculateReducedDamage(target, type, finalDamage);

		LegendsOfTheStones.LOGGER.debug("Damage calculation: {} × (1 - {}) = {} (Resistance: {})",
				originalDamage, resistance.damageResistance(), finalDamage, resistance);

		if (thresholdReached) {
			finalDamage = applyThresholdEffect(target, type, event, finalDamage);
			LegendsOfTheStonesAttachments.resetPoints(target, type);
			LegendsOfTheStones.LOGGER.info("✨ {}! Entity: {}, Type: {} Resonance Breakthrough",
					target.getName().getString(), type);
		}

		event.setNewDamage(finalDamage);

		if (canShowDamage(target)) {
			spawnDamageNumber(target, finalDamage, type);
		}
		updateLastDamageTime(target, type);
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		clearActiveDisplays(entity);
		DAMAGE_COOLDOWNS.remove(entity.getId());
		LAST_DAMAGE_TIME.remove(entity.getId());
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity livingEntity) {
			clearActiveDisplays(livingEntity);
			DAMAGE_COOLDOWNS.remove(entity.getId());
			LAST_DAMAGE_TIME.remove(entity.getId());
		}
	}

	// ✅ НОВОЕ: Очистка ПРИ ВЫХОДЕ ИГРОКА (до выгрузки мира)
	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		// Исправление ошибки приведения типа: явная проверка instanceof
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		LegendsOfTheStones.LOGGER.info("Player {} logged out. Force cleaning all damage displays immediately.", player.getName().getString());

		if (displayManager != null) {
			// Очищаем дисплеи, где игрок был целью
			clearActiveDisplays(player);

			// В синглплеере или если игрок был источником урона для многих мобов,
			// лучше очистить ВСЕ дисплеи превентивно, чтобы они не сохранились в чанках.
			displayManager.cleanupAllDisplays();
		}

		DAMAGE_COOLDOWNS.clear();
		LAST_DAMAGE_TIME.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel) {
			// Дублирующая очистка на случай, если мир выгрузился по другой причине
			if (displayManager != null) {
				displayManager.cleanupAllDisplays();
			}
			LegendsOfTheStones.LOGGER.info("Level unloading. Final cleanup check performed.");
		}
	}

	// ✅ НОВОЕ: ОБРАБОТКА ВЫГРУЗКИ ЧАНКА (отложенное удаление)
	@SubscribeEvent
	public static void onChunkUnload(ChunkDataEvent.Save event) {
		if (displayManager == null) return;

		if (!(event.getLevel() instanceof ServerLevel level)) {
			return;
		}

		int chunkX = event.getChunk().getPos().x;
		int chunkZ = event.getChunk().getPos().z;

		// Этот метод только помечает сущности на удаление (добавляет в очередь),
		// чтобы не вызвать ConcurrentModificationException во время сохранения чанка.
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
			if (registryElement.isPresent()) {
				return registryElement.get();
			}
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
		for (ElementType type : ElementType.values()) {
			if (type.getDamageTypeId().equals(msgId)) return type;
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

	// === УПРАВЛЕНИЕ ВРЕМЕНЕМ ПОСЛЕДНЕГО УРОНА ===
	private static void updateLastDamageTime(LivingEntity entity, ElementType type) {
		int entityId = entity.getId();
		long gameTime = entity.level().getGameTime();
		LAST_DAMAGE_TIME.computeIfAbsent(entityId, k -> new EnumMap<>(ElementType.class))
				.put(type, gameTime);
	}

	private static void checkAndResetInactivePoints() {
		if (currentServer == null) return;
		long currentTime = currentServer.overworld().getGameTime();

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

			if (livingEntity == null) {
				entityIterator.remove();
				continue;
			}

			Iterator<Map.Entry<ElementType, Long>> typeIterator = typeTimes.entrySet().iterator();
			while (typeIterator.hasNext()) {
				Map.Entry<ElementType, Long> typeEntry = typeIterator.next();
				ElementType type = typeEntry.getKey();
				long lastTime = typeEntry.getValue();

				if (currentTime - lastTime >= RESET_DELAY_TICKS) {
					int pointsBefore = LegendsOfTheStonesAttachments.getPoints(livingEntity, type);
					if (pointsBefore > 0) {
						LegendsOfTheStonesAttachments.resetPoints(livingEntity, type);
						LegendsOfTheStones.LOGGER.debug("Reset {} points for {} (inactive for {} ticks)",
								pointsBefore, type, RESET_DELAY_TICKS);
					}
					typeIterator.remove();
				}
			}
			if (typeTimes.isEmpty()) entityIterator.remove();
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

	// === ДЕЛЕГИРОВАНИЕ ВИЗУАЛИЗАЦИИ ===
	private static void clearActiveDisplays(LivingEntity entity) {
		if (displayManager != null) {
			displayManager.clearActiveDisplays(entity);
		}
	}

	private static void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
		if (displayManager != null) {
			displayManager.spawnDamageNumber(entity, amount, type);
		}
	}

	public static void spawnStatusText(LivingEntity entity, Component textComponent, int color) {
		if (displayManager != null) {
			displayManager.spawnStatusText(entity, textComponent, color);
		}
	}

	public static void spawnStatusText(LivingEntity entity, String text, int color) {
		if (displayManager != null) {
			displayManager.spawnStatusText(entity, text, color);
		}
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
				target.igniteForSeconds(5);
				spawnStatusText(target, Component.translatable("elemental.tooltip.overheating"), 0xFF5500);
				yield currentDamage;
			}
			case PHYSICAL -> {
				spawnStatusText(target, Component.translatable("elemental.tooltip.crit_dmg"), 0xFFAA00);
				yield currentDamage * 5.0f;
			}
			case WIND -> {
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
				target.push(0, 0.5, 0);
				spawnStatusText(target, Component.translatable("elemental.tooltip.wind_whirlwind"), 0x00FFFF);
				yield currentDamage;
			}
			case WATER -> {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
				if (target.isOnFire()) target.extinguishFire();
				spawnStatusText(target, Component.translatable("elemental.tooltip.water_flood"), 0x0080FF);
				yield currentDamage;
			}
			case EARTH -> {
				target.addEffect(new MobEffectInstance(MobEffects.OOZING, 80, 4));
				spawnStatusText(target, Component.translatable("elemental.tooltip.earth_petrify"), 0x8B4513);
				yield currentDamage * 1.5f;
			}
			case ICE -> {
				target.setTicksFrozen(160);
				if (target.isOnFire()) {
					target.extinguishFire();
					target.hurt(target.damageSources().magic(), 2.0f);
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.ice_freeze"), 0x00BFFF);
				yield currentDamage * 1.25f;
			}
			case ELECTRIC -> {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
				if (!target.level().isClientSide) {
					target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0),
							e -> e != target && e.isAlive()).forEach(e -> {
						e.hurt(target.damageSources().magic(), currentDamage * 0.5f);
						e.igniteForSeconds(2);
					});
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.electric_shock"), 0xFFFF00);
				yield currentDamage * 1.5f;
			}
			case SOURCE -> {
				var random = target.level().random;
				switch (random.nextInt(4)) {
					case 0 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
					case 1 -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
					case 2 -> target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
					case 3 -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.source_void"), 0x9932CC);
				yield currentDamage * 2.0f;
			}
			case NATURAL -> {
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
				if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != target) {
					attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.natural_bloom"), 0x32CD32);
				yield currentDamage;
			}
			case QUANTUM -> {
				if (!target.level().isClientSide && target.getRandom().nextFloat() < 0.5f) {
					double dx = (target.getRandom().nextDouble() - 0.5) * 10;
					double dz = (target.getRandom().nextDouble() - 0.5) * 10;
					target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
					target.level().broadcastEntityEvent(target, (byte) 46);
				}
				float quantumMultiplier = 0.5f + target.getRandom().nextFloat() * 2.0f;
				spawnStatusText(target, Component.translatable("elemental.tooltip.quantum_flux"), 0xFF00FF);
				yield currentDamage * quantumMultiplier;
			}
			default -> currentDamage;
		};
	}

	private static float applyThresholdEffectWithDamage(LivingEntity target, ElementType type, float originalDamage) {
		LegendsOfTheStones.LOGGER.info("THRESHOLD REACHED! Entity: {}, Type: {}", target.getName().getString(), type);
		return switch (type) {
			case FIRE -> {
				target.igniteForSeconds(5);
				spawnStatusText(target, Component.translatable("elemental.tooltip.overheating"), 0xFF5500);
				yield originalDamage;
			}
			case PHYSICAL -> {
				spawnStatusText(target, Component.translatable("elemental.tooltip.crit_dmg"), 0xFFAA00);
				yield originalDamage * 5.0f;
			}
			case WIND -> {
				target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
				target.push(0, 0.5, 0);
				spawnStatusText(target, Component.translatable("elemental.tooltip.wind_whirlwind"), 0x00FFFF);
				yield originalDamage;
			}
			case WATER -> {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
				if (target.isOnFire()) target.extinguishFire();
				spawnStatusText(target, Component.translatable("elemental.tooltip.water_flood"), 0x0080FF);
				yield originalDamage;
			}
			case EARTH -> {
				target.addEffect(new MobEffectInstance(MobEffects.OOZING, 80, 4));
				spawnStatusText(target, Component.translatable("elemental.tooltip.earth_petrify"), 0x8B4513);
				yield originalDamage * 1.5f;
			}
			case ICE -> {
				target.setTicksFrozen(160);
				if (target.isOnFire()) {
					target.extinguishFire();
					target.hurt(target.damageSources().magic(), 2.0f);
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.ice_freeze"), 0x00BFFF);
				yield originalDamage * 1.25f;
			}
			case ELECTRIC -> {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
				if (!target.level().isClientSide) {
					target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0),
							e -> e != target && e.isAlive()).forEach(e -> {
						e.hurt(target.damageSources().magic(), originalDamage * 0.5f);
						e.igniteForSeconds(2);
					});
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.electric_shock"), 0xFFFF00);
				yield originalDamage * 1.5f;
			}
			case SOURCE -> {
				var random = target.level().random;
				switch (random.nextInt(4)) {
					case 0 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
					case 1 -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
					case 2 -> target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0));
					case 3 -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
				}
				spawnStatusText(target, Component.translatable("elemental.tooltip.source_void"), 0x9932CC);
				yield originalDamage * 2.0f;
			}
			case NATURAL -> {
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
				spawnStatusText(target, Component.translatable("elemental.tooltip.natural_bloom"), 0x32CD32);
				yield originalDamage;
			}
			case QUANTUM -> {
				if (!target.level().isClientSide && target.getRandom().nextFloat() < 0.5f) {
					double dx = (target.getRandom().nextDouble() - 0.5) * 10;
					double dz = (target.getRandom().nextDouble() - 0.5) * 10;
					target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
					target.level().broadcastEntityEvent(target, (byte) 46);
				}
				float quantumMultiplier = 0.5f + target.getRandom().nextFloat() * 2.0f;
				spawnStatusText(target, Component.translatable("elemental.tooltip.quantum_flux"), 0xFF00FF);
				yield originalDamage * quantumMultiplier;
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

	public static void dealElementDamage(Entity target, ElementType type, float amount, int accumulationPoints) {
		if (!(target.level() instanceof ServerLevel serverLevel)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamage: not server level");
			return;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamage: target is not LivingEntity");
			return;
		}
		if (ElementResistanceManager.isImmune(target, type)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (manual call)!", target.getName().getString(), type);
			return;
		}

		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, type.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {} - урон НЕ будет нанесён!", rl);
			return;
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
			if (canShowDamage(livingTarget)) {
				spawnDamageNumber(livingTarget, finalDamage, type);
			}
		} else {
			if (canShowDamage(livingTarget)) {
				spawnDamageNumber(livingTarget, finalDamage, type);
			}
		}
		target.hurt(source, finalDamage);
		updateLastDamageTime(livingTarget, type);
	}

	public static void dealElementDamageWithAccum(Entity target, ElementType type, float amount, float accumMultiplier) {
		if (!(target.level() instanceof ServerLevel serverLevel)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamageWithAccum: not server level");
			return;
		}
		if (!(target instanceof LivingEntity livingTarget)) {
			LegendsOfTheStones.LOGGER.warn("dealElementDamageWithAccum: target is not LivingEntity");
			return;
		}
		if (ElementResistanceManager.isImmune(target, type)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (manual call)!", target.getName().getString(), type);
			return;
		}

		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, type.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {} - урон НЕ будет нанесён!", rl);
			return;
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
			if (canShowDamage(livingTarget)) {
				spawnDamageNumber(livingTarget, finalDamage, type);
			}
		} else {
			if (canShowDamage(livingTarget)) {
				spawnDamageNumber(livingTarget, finalDamage, type);
			}
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
		LAST_DAMAGE_TIME.computeIfPresent(entity.getId(), (id, map) -> {
			map.remove(type);
			return map.isEmpty() ? null : map;
		});
	}

	public static void resetAllElementPoints(LivingEntity entity) {
		for (ElementType type : ElementType.values()) {
			LegendsOfTheStonesAttachments.resetPoints(entity, type);
		}
		LAST_DAMAGE_TIME.remove(entity.getId());
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

	public static void applyElementalDamageInstant(
			Entity target,
			Entity source,
			ElementType elementType,
			float baseDamage,
			float accumMultiplier
	) {
		if (!(target.level() instanceof ServerLevel serverLevel) || !(target instanceof LivingEntity livingTarget)) {
			return;
		}
		if (ElementResistanceManager.isImmune(target, elementType)) {
			LegendsOfTheStones.LOGGER.debug("{} is IMMUNE to {} (instant)", target.getName().getString(), elementType);
			return;
		}

		var damageTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		var rl = ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, elementType.getDamageTypeId());
		var damageTypeHolder = damageTypeRegistry.getHolder(rl);
		if (damageTypeHolder.isEmpty()) {
			LegendsOfTheStones.LOGGER.error("Damage type NOT FOUND: {}", rl);
			return;
		}

		DamageSource dmgSource = new DamageSource(damageTypeHolder.get(), source, source);
		float finalDamage = ElementResistanceManager.calculateReducedDamage(livingTarget, elementType, baseDamage);

		int basePoints = (int) baseAccumulation;
		int pointsToAdd = ElementResistanceManager.calculateAccumulationPoints(livingTarget, elementType, basePoints);
		pointsToAdd = Math.round(pointsToAdd * accumMultiplier);

		if (pointsToAdd > 0) {
			int before = LegendsOfTheStonesAttachments.getPoints(livingTarget, elementType);
			LegendsOfTheStonesAttachments.addPoints(livingTarget, elementType, pointsToAdd);
			int after = LegendsOfTheStonesAttachments.getPoints(livingTarget, elementType);
			boolean thresholdReached = after >= THRESHOLD;

			LegendsOfTheStones.LOGGER.info("[Instant] {} | {} | x{} | {} +{} → {} | Breakthrough: {}",
					livingTarget.getName().getString(), elementType, accumMultiplier,
					before, pointsToAdd, after, thresholdReached);

			if (thresholdReached) {
				finalDamage = applyThresholdEffectWithDamage(livingTarget, elementType, baseDamage);
				LegendsOfTheStonesAttachments.resetPoints(livingTarget, elementType);
			}
		}
		if (canShowDamage(livingTarget)) {
			spawnDamageNumber(livingTarget, finalDamage, elementType);
		}
		target.hurt(dmgSource, finalDamage);
		updateLastDamageTime(livingTarget, elementType);
	}
}