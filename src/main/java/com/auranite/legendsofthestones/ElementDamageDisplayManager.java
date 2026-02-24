package com.auranite.legendsofthestones;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ElementDamageDisplayManager {

    // === КОНСТАНТЫ ОТОБРАЖЕНИЯ ===
    private static final int DAMAGE_NUMBER_LIFETIME = 30;
    private static final int STATUS_TEXT_LIFETIME = 50;
    private static final byte FLAG_SEE_THROUGH = 2;

    // === ТЕГИ ДЛЯ ОЧИСТКИ (Vanilla Entity Tags) ===
    public static final String CLEANUP_TAG = "lots:cleanup_on_load";
    public static final String SELF_DESTRUCT_TAG = "lots:self_destruct";

    // === КЛЮЧИ NBT ДЛЯ SELF-DESTRUCT ===
    private static final String NBT_MAX_LIFETIME = "lots:max_lifetime";
    private static final String NBT_AGE = "lots:age";

    // === ФИЗИКА ДЛЯ УРОНА ===
    private static final double DAMAGE_GRAVITY = -0.02;
    private static final double DAMAGE_INITIAL_VELOCITY_Y = 0.18;
    private static final double HORIZONTAL_DRIFT = 0.02;

    // === ФИЗИКА ДЛЯ СТАТУСА (без падения) ===
    private static final double STATUS_FLOAT_AMPLITUDE = 0.02;
    private static final double STATUS_FLOAT_SPEED = 0.15;
    private static final int INTERPOLATION_DURATION = 3;

    // === ЗАПАС ВРЕМЕНИ ДЛЯ SELF-DESTRUCT (в тиках) ===
    private static final int SELF_DESTRUCT_BUFFER = 20;

    // === ВСПОМОГАТЕЛЬНЫЙ КЛАСС ===
    private static class DisplayInfo {
        final TextDisplay display;
        final int targetEntityId;
        final boolean isStatus;

        DisplayInfo(TextDisplay display, int targetEntityId, boolean isStatus) {
            this.display = display;
            this.targetEntityId = targetEntityId;
            this.isStatus = isStatus;
        }
    }

    // === ХРАНИЛИЩА ===
    private static final Map<UUID, DisplayInfo> ACTIVE_DAMAGE_DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<UUID, DisplayInfo> ACTIVE_STATUS_DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<ElementType, Integer> DAMAGE_COLORS = new EnumMap<>(ElementType.class);
    private static final Map<UUID, double[]> ACTIVE_PHYSICS = new ConcurrentHashMap<>();

    // ✅ Списки для отложенного удаления
    private static final CopyOnWriteArrayList<TextDisplay> PENDING_REMOVALS = new CopyOnWriteArrayList<>();

    // === ЦВЕТА ===
    public static void registerDamageColor(ElementType type, int color) {
        DAMAGE_COLORS.put(type, color);
    }

    public static void setDamageColor(ElementType type, int color) {
        DAMAGE_COLORS.put(type, color);
    }

    public static Map<ElementType, Integer> getAllDamageColors() {
        return new EnumMap<>(DAMAGE_COLORS);
    }

    private static int getDamageColor(ElementType type) {
        if (type == null) return 0xFFFFFF;
        return DAMAGE_COLORS.getOrDefault(type, 0xFFFFFF);
    }

    // === ОЧИСТКА ===

    public void cleanupStaleDisplays() {
        int cleanedCount = 0;

        Iterator<Map.Entry<UUID, DisplayInfo>> damageIterator = ACTIVE_DAMAGE_DISPLAYS.entrySet().iterator();
        while (damageIterator.hasNext()) {
            Map.Entry<UUID, DisplayInfo> entry = damageIterator.next();
            UUID displayUuid = entry.getKey();
            DisplayInfo info = entry.getValue();

            if (info == null || info.display == null || info.display.isRemoved() || info.display.level() == null) {
                damageIterator.remove();
                ACTIVE_PHYSICS.remove(displayUuid);
                cleanedCount++;
                continue;
            }

            Entity target = info.display.level().getEntity(info.targetEntityId);
            if (target == null || !target.isAlive()) {
                if (!info.display.isRemoved()) safeRemoveDisplaySilent(info.display);
                damageIterator.remove();
                ACTIVE_PHYSICS.remove(displayUuid);
                cleanedCount++;
            }
        }

        Iterator<Map.Entry<UUID, DisplayInfo>> statusIterator = ACTIVE_STATUS_DISPLAYS.entrySet().iterator();
        while (statusIterator.hasNext()) {
            Map.Entry<UUID, DisplayInfo> entry = statusIterator.next();
            UUID displayUuid = entry.getKey();
            DisplayInfo info = entry.getValue();

            if (info == null || info.display == null || info.display.isRemoved() || info.display.level() == null) {
                statusIterator.remove();
                ACTIVE_PHYSICS.remove(displayUuid);
                cleanedCount++;
                continue;
            }

            Entity target = info.display.level().getEntity(info.targetEntityId);
            if (target == null || !target.isAlive()) {
                if (!info.display.isRemoved()) safeRemoveDisplaySilent(info.display);
                statusIterator.remove();
                ACTIVE_PHYSICS.remove(displayUuid);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            LegendsOfTheStones.LOGGER.debug("ElementDamageDisplayManager: cleaned {} stale displays", cleanedCount);
        }
    }

    public void cleanupAllDisplays() {
        LegendsOfTheStones.LOGGER.info("Force cleaning ALL element damage displays...");

        for (DisplayInfo info : ACTIVE_DAMAGE_DISPLAYS.values()) {
            if (info != null && info.display != null && !info.display.isRemoved()) {
                safeRemoveDisplaySilent(info.display);
            }
        }
        ACTIVE_DAMAGE_DISPLAYS.clear();

        for (DisplayInfo info : ACTIVE_STATUS_DISPLAYS.values()) {
            if (info != null && info.display != null && !info.display.isRemoved()) {
                safeRemoveDisplaySilent(info.display);
            }
        }
        ACTIVE_STATUS_DISPLAYS.clear();
        ACTIVE_PHYSICS.clear();

        LegendsOfTheStones.LOGGER.info("All element damage displays cleared successfully.");
    }

    public void clearActiveDisplays(LivingEntity entity) {
        if (entity == null) return;
        int entityId = entity.getId();

        ACTIVE_DAMAGE_DISPLAYS.entrySet().removeIf(entry -> {
            DisplayInfo info = entry.getValue();
            if (info != null && info.targetEntityId == entityId) {
                if (info.display != null && !info.display.isRemoved()) {
                    safeRemoveDisplaySilent(info.display);
                }
                ACTIVE_PHYSICS.remove(entry.getKey());
                return true;
            }
            return false;
        });

        ACTIVE_STATUS_DISPLAYS.entrySet().removeIf(entry -> {
            DisplayInfo info = entry.getValue();
            if (info != null && info.targetEntityId == entityId) {
                if (info.display != null && !info.display.isRemoved()) {
                    safeRemoveDisplaySilent(info.display);
                }
                ACTIVE_PHYSICS.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public int cleanupDisplaysInChunk(ServerLevel level, int chunkX, int chunkZ) {
        int count = 0;

        Iterator<Map.Entry<UUID, DisplayInfo>> damageIterator = ACTIVE_DAMAGE_DISPLAYS.entrySet().iterator();
        while (damageIterator.hasNext()) {
            Map.Entry<UUID, DisplayInfo> entry = damageIterator.next();
            DisplayInfo info = entry.getValue();

            if (info != null && info.display != null && !info.display.isRemoved()) {
                int dChunkX = (int) Math.floor(info.display.getX() / 16.0);
                int dChunkZ = (int) Math.floor(info.display.getZ() / 16.0);

                if (dChunkX == chunkX && dChunkZ == chunkZ) {
                    PENDING_REMOVALS.add(info.display);
                    damageIterator.remove();
                    ACTIVE_PHYSICS.remove(entry.getKey());
                    count++;
                }
            } else if (info == null || info.display == null || info.display.isRemoved()) {
                damageIterator.remove();
                ACTIVE_PHYSICS.remove(entry.getKey());
            }
        }

        Iterator<Map.Entry<UUID, DisplayInfo>> statusIterator = ACTIVE_STATUS_DISPLAYS.entrySet().iterator();
        while (statusIterator.hasNext()) {
            Map.Entry<UUID, DisplayInfo> entry = statusIterator.next();
            DisplayInfo info = entry.getValue();

            if (info != null && info.display != null && !info.display.isRemoved()) {
                int dChunkX = (int) Math.floor(info.display.getX() / 16.0);
                int dChunkZ = (int) Math.floor(info.display.getZ() / 16.0);

                if (dChunkX == chunkX && dChunkZ == chunkZ) {
                    PENDING_REMOVALS.add(info.display);
                    statusIterator.remove();
                    ACTIVE_PHYSICS.remove(entry.getKey());
                    count++;
                }
            } else if (info == null || info.display == null || info.display.isRemoved()) {
                statusIterator.remove();
                ACTIVE_PHYSICS.remove(entry.getKey());
            }
        }

        return count;
    }

    public void processPendingRemovals() {
        if (PENDING_REMOVALS.isEmpty()) return;

        for (TextDisplay display : PENDING_REMOVALS) {
            if (display != null && !display.isRemoved() && display.level() != null) {
                try {
                    safeRemoveDisplaySilent(display);
                } catch (Exception e) {
                    LegendsOfTheStones.LOGGER.warn("Failed to discard pending display: {}", e.getMessage());
                }
            }
        }
        PENDING_REMOVALS.clear();
    }

    // === ОЧИСТКА "ОСИРОВЕВШИХ" ДИСПЛЕЕВ ПРИ ЗАГРУЗКЕ МИРА ===
    /**
     * Вызывается при загрузке ServerLevel через server.execute() для гарантии загрузки чанков.
     * ✅ ИСПРАВЛЕНО: Используем getTags().contains() вместо hasTag() для совместимости с 1.19-1.20
     */
    public static void cleanupOrphanedDisplaysOnWorldLoad(ServerLevel level) {
        if (level == null) return;

        long startTime = System.currentTimeMillis();
        int removedCount = 0;

        // ✅ Совместимый Predicate для 1.19-1.20
        Predicate<Entity> hasCleanupTag = e -> e.getTags().contains(CLEANUP_TAG) && !e.isRemoved();

        for (TextDisplay display : level.getEntities(EntityType.TEXT_DISPLAY, hasCleanupTag)) {
            if (display != null && !display.isRemoved()) {
                display.discard();
                removedCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        if (removedCount > 0) {
            LegendsOfTheStones.LOGGER.info("Cleaned {} orphaned TextDisplay entities on world load in {}ms", removedCount, duration);
        }
    }

    // === SELF-DESTRUCT МЕХАНИЗМ (последний рубеж защиты) ===
    /**
     * Вызывается каждый тик сервера для всех уровней.
     * Проверяет TextDisplay с флагом self_destruct и удаляет их по истечении времени.
     */
    public static void tickSelfDestructDisplays(ServerLevel level) {
        if (level == null) return;

        // ✅ Совместимый Predicate для 1.19-1.20
        Predicate<Entity> hasSelfDestruct = e -> {
            CompoundTag tag = e.getPersistentData();
            return tag.getBoolean(SELF_DESTRUCT_TAG) && !e.isRemoved();
        };

        for (TextDisplay display : level.getEntities(EntityType.TEXT_DISPLAY, hasSelfDestruct)) {
            if (display == null || display.isRemoved()) continue;

            CompoundTag tag = display.getPersistentData();
            int age = tag.getInt(NBT_AGE) + 1;
            int maxLife = tag.getInt(NBT_MAX_LIFETIME);

            if (age >= maxLife) {
                display.discard();
            } else {
                tag.putInt(NBT_AGE, age);
            }
        }
    }

    // === СПАВН ===
    public void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        int entityId = entity.getId();
        int color = getDamageColor(type);

        double offsetX = (serverLevel.random.nextFloat() - 0.5f) * 0.5;
        double offsetZ = (serverLevel.random.nextFloat() - 0.5f) * 0.5;

        TextDisplay display = createTextDisplay(
                serverLevel,
                entity.getX() + offsetX,
                entity.getY() + entity.getBbHeight() + 0.5,
                entity.getZ() + offsetZ,
                String.format("%.1f", amount),
                color,
                DAMAGE_NUMBER_LIFETIME + SELF_DESTRUCT_BUFFER
        );

        if (display != null) {
            serverLevel.addFreshEntity(display);
            UUID displayUuid = display.getUUID();

            ACTIVE_DAMAGE_DISPLAYS.put(displayUuid, new DisplayInfo(display, entityId, false));

            double randomX = (serverLevel.random.nextFloat() - 0.5f) * HORIZONTAL_DRIFT;
            double randomZ = (serverLevel.random.nextFloat() - 0.5f) * HORIZONTAL_DRIFT;
            ACTIVE_PHYSICS.put(displayUuid, new double[]{randomX, DAMAGE_INITIAL_VELOCITY_Y, randomZ, 0, DAMAGE_NUMBER_LIFETIME, color, 0});

            schedulePhysicsUpdate(serverLevel, displayUuid);

            // ✅ Страховка с запасом +10 тиков
            LegendsOfTheStones.queueServerWork(DAMAGE_NUMBER_LIFETIME + 10, () -> {
                if (ACTIVE_PHYSICS.containsKey(displayUuid)) {
                    TextDisplay d = (TextDisplay) serverLevel.getEntity(displayUuid);
                    if (d != null && !d.isRemoved()) safeRemoveDisplaySilent(d);
                    cleanupDisplayResources(displayUuid);
                }
            });
        }
    }

    public void spawnStatusText(LivingEntity entity, Component textComponent, int color) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        int entityId = entity.getId();

        double offsetX = (serverLevel.random.nextFloat() - 0.5f) * 0.3;
        double offsetZ = (serverLevel.random.nextFloat() - 0.5f) * 0.3;

        TextDisplay display = createTextDisplay(
                serverLevel,
                entity.getX() + offsetX,
                entity.getY() + entity.getBbHeight() + 1.2,
                entity.getZ() + offsetZ,
                textComponent,
                color,
                STATUS_TEXT_LIFETIME + SELF_DESTRUCT_BUFFER
        );

        if (display != null) {
            serverLevel.addFreshEntity(display);
            UUID displayUuid = display.getUUID();

            ACTIVE_STATUS_DISPLAYS.put(displayUuid, new DisplayInfo(display, entityId, true));

            double randomPhase = serverLevel.random.nextDouble() * Math.PI * 2;
            ACTIVE_PHYSICS.put(displayUuid, new double[]{0, 0, 0, 0, STATUS_TEXT_LIFETIME, color, randomPhase});

            schedulePhysicsUpdate(serverLevel, displayUuid);

            // ✅ Страховка с запасом +10 тиков
            LegendsOfTheStones.queueServerWork(STATUS_TEXT_LIFETIME + 10, () -> {
                if (ACTIVE_PHYSICS.containsKey(displayUuid)) {
                    TextDisplay d = (TextDisplay) serverLevel.getEntity(displayUuid);
                    if (d != null && !d.isRemoved()) safeRemoveDisplaySilent(d);
                    cleanupDisplayResources(displayUuid);
                }
            });
        }
    }

    public void spawnStatusText(LivingEntity entity, String text, int color) {
        spawnStatusText(entity, Component.literal(text), color);
    }

    // === ФИЗИКА И ЭФФЕКТЫ ===
    private void schedulePhysicsUpdate(ServerLevel level, UUID displayUuid) {
        LegendsOfTheStones.queueServerWork(1, () -> {
            TextDisplay display = (TextDisplay) level.getEntity(displayUuid);
            double[] physics = ACTIVE_PHYSICS.get(displayUuid);
            DisplayInfo info = ACTIVE_DAMAGE_DISPLAYS.get(displayUuid);
            if (info == null) info = ACTIVE_STATUS_DISPLAYS.get(displayUuid);

            // Если сущности или физики нет — очищаем и выходим
            if (display == null || display.isRemoved() || physics == null) {
                cleanupDisplayResources(displayUuid);
                return;
            }

            physics[3]++;
            int ticksAlive = (int) physics[3];
            int maxTicks = (int) physics[4];
            int originalColor = (int) physics[5];
            double floatPhase = physics[6];

            // === ОБНОВЛЕНИЕ: СТАТУС ===
            if (info != null && info.isStatus) {
                floatPhase += STATUS_FLOAT_SPEED;
                physics[6] = floatPhase;

                double floatOffset = Math.sin(floatPhase) * STATUS_FLOAT_AMPLITUDE;
                display.setPos(display.getX(), display.getY() + floatOffset, display.getZ());

                double pulse = (Math.sin(floatPhase * 2) + 1) / 2;
                int r = (originalColor >> 16) & 0xFF;
                int g = (originalColor >> 8) & 0xFF;
                int b = originalColor & 0xFF;

                int shimmerR = (int) (r + (255 - r) * pulse * 0.5);
                int shimmerG = (int) (g + (255 - g) * pulse * 0.5);
                int shimmerB = (int) (b + (255 - b) * pulse * 0.5);
                int shimmerColor = (shimmerR << 16) | (shimmerG << 8) | shimmerB;

                Component currentText = display.getText();
                if (currentText != null) {
                    display.setText(currentText.copy().withStyle(Style.EMPTY.withColor(shimmerColor).withBold(true)));
                }

            }
            // === ОБНОВЛЕНИЕ: УРОН ===
            else {
                physics[1] += DAMAGE_GRAVITY;
                display.setPos(display.getX() + physics[0], display.getY() + physics[1], display.getZ() + physics[2]);

                int fadeStartTick = (int) (maxTicks * 0.7);
                if (ticksAlive >= fadeStartTick) {
                    int fadeTicks = maxTicks - fadeStartTick;
                    int currentFadeTick = ticksAlive - fadeStartTick;
                    float alpha = 1.0f - (currentFadeTick / (float) fadeTicks);
                    alpha = Math.max(0.0f, Math.min(1.0f, alpha));

                    Component currentText = display.getText();
                    if (currentText != null) {
                        int r = (originalColor >> 16) & 0xFF;
                        int g = (originalColor >> 8) & 0xFF;
                        int b = originalColor & 0xFF;
                        int a = (int) (alpha * 255);
                        int colorWithAlpha = (a << 24) | (r << 16) | (g << 8) | b;

                        display.setText(currentText.copy().withStyle(Style.EMPTY.withColor(colorWithAlpha).withBold(true)));
                    }
                }
            }

            // === ЕДИНОЕ МЕСТО УДАЛЕНИЯ ===
            if (ticksAlive >= maxTicks) {
                safeRemoveDisplay(level, displayUuid, display, info);
                return;
            }

            // Рекурсия
            if (!display.isRemoved()) {
                schedulePhysicsUpdate(level, displayUuid);
            }
        });
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ УДАЛЕНИЯ ===

    /**
     * Безопасное удаление с отправкой пакета клиенту
     */
    private void safeRemoveDisplay(ServerLevel level, UUID displayUuid, TextDisplay display, DisplayInfo info) {
        // ✅ Удаляем vanilla-тег, чтобы сущность не считалась "осиротевшей"
        display.removeTag(CLEANUP_TAG);

        // Отправляем пакет удаления клиенту
        level.getChunkSource().broadcastAndSend(
                display,
                new ClientboundRemoveEntitiesPacket(display.getId())
        );

        // Удаляем на сервере
        if (!display.isRemoved()) {
            display.discard();
        }

        // Чистим ресурсы
        cleanupDisplayResources(displayUuid);
    }

    /**
     * Тихое удаление (без UUID) для случаев очистки/страховки
     */
    private void safeRemoveDisplaySilent(TextDisplay display) {
        if (display == null || display.isRemoved() || display.level() == null) return;

        // ✅ Удаляем vanilla-тег
        display.removeTag(CLEANUP_TAG);

        ServerLevel level = (ServerLevel) display.level();
        level.getChunkSource().broadcastAndSend(
                display,
                new ClientboundRemoveEntitiesPacket(display.getId())
        );
        display.discard();
    }

    /**
     * Очистка коллекций менеджера
     */
    private void cleanupDisplayResources(UUID uuid) {
        ACTIVE_DAMAGE_DISPLAYS.remove(uuid);
        ACTIVE_STATUS_DISPLAYS.remove(uuid);
        ACTIVE_PHYSICS.remove(uuid);
    }

    // === СОЗДАНИЕ ===

    /**
     * Рекурсивно помечает сущность и всех её пассажиров тегами очистки.
     * ✅ Использует vanilla entity tags вместо PersistentData
     */
    private static void markForCleanup(Entity entity, int maxLifetime) {
        if (entity == null) return;

        // ✅ Vanilla-тег для очистки при загрузке мира (совместимо с 1.19+)
        entity.addTag(CLEANUP_TAG);

        // ✅ NBT-данные для self-destruct механизма
        CompoundTag tag = entity.getPersistentData();
        tag.putBoolean(SELF_DESTRUCT_TAG, true);
        tag.putInt(NBT_MAX_LIFETIME, maxLifetime);
        tag.putInt(NBT_AGE, 0);

        for (Entity passenger : entity.getPassengers()) {
            markForCleanup(passenger, maxLifetime);
        }
    }

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, Component textComponent, int color, int maxLifetime) {
        TextDisplay display = EntityType.TEXT_DISPLAY.create(level);
        if (display == null) {
            LegendsOfTheStones.LOGGER.error("Failed to create TextDisplay entity at ({}, {}, {})", x, y, z);
            return null;
        }

        display.setPos(x, y, z);
        display.setText(textComponent.copy().withStyle(Style.EMPTY.withColor(color).withBold(true)));
        display.setBackgroundColor(0x00000000);
        display.setFlags(FLAG_SEE_THROUGH);
        display.setLineWidth(200);
        display.setBillboardConstraints(BillboardConstraints.CENTER);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setViewRange(64.0f);
        display.setPosRotInterpolationDuration(INTERPOLATION_DURATION);
        display.setTransformationInterpolationDuration(INTERPOLATION_DURATION);
        display.setTransformationInterpolationDelay(0);

        // ✅ МЕТКА ДЛЯ ОЧИСТКИ: vanilla-тег + NBT для self-destruct
        markForCleanup(display, maxLifetime);

        return display;
    }

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, String text, int color, int maxLifetime) {
        return createTextDisplay(level, x, y, z, Component.literal(text), color, maxLifetime);
    }
}