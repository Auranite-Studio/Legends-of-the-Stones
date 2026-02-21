package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElementDamageDisplayManager {

    // === КОНСТАНТЫ ОТОБРАЖЕНИЯ ===
    private static final int DAMAGE_NUMBER_LIFETIME = 30;
    private static final int STATUS_TEXT_LIFETIME = 50;
    private static final byte FLAG_SEE_THROUGH = 2;

    // === ФИЗИКА ДЛЯ УРОНА ===
    private static final double DAMAGE_GRAVITY = -0.02;
    private static final double DAMAGE_INITIAL_VELOCITY_Y = 0.18;
    private static final double HORIZONTAL_DRIFT = 0.02;

    // === ФИЗИКА ДЛЯ СТАТУСА (без падения) ===
    private static final double STATUS_FLOAT_AMPLITUDE = 0.02; // Амплитуда парения
    private static final double STATUS_FLOAT_SPEED = 0.15;     // Скорость парения

    private static final int INTERPOLATION_DURATION = 3;

    // === ВСПОМОГАТЕЛЬНЫЙ КЛАСС ===
    private static class DisplayInfo {
        final TextDisplay display;
        final int targetEntityId;
        final boolean isStatus; // true = статус, false = урон

        DisplayInfo(TextDisplay display, int targetEntityId, boolean isStatus) {
            this.display = display;
            this.targetEntityId = targetEntityId;
            this.isStatus = isStatus;
        }
    }

    // === ХРАНИЛИЩА (Ключ - UUID дисплея) ===
    private static final Map<UUID, DisplayInfo> ACTIVE_DAMAGE_DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<UUID, DisplayInfo> ACTIVE_STATUS_DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<ElementType, Integer> DAMAGE_COLORS = new EnumMap<>(ElementType.class);

    // === ФИЗИКА [velX, velY, velZ, ticksAlive, maxTicks, originalColor, floatOffset] ===
    // Для статуса: velY используется как фаза для парения
    private static final Map<UUID, double[]> ACTIVE_PHYSICS = new ConcurrentHashMap<>();

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

        // Очистка урона
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
                if (!info.display.isRemoved()) info.display.discard();
                damageIterator.remove();
                ACTIVE_PHYSICS.remove(displayUuid);
                cleanedCount++;
            }
        }

        // Очистка статусов
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
                if (!info.display.isRemoved()) info.display.discard();
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
        ACTIVE_DAMAGE_DISPLAYS.values().forEach(info -> {
            if (info != null && info.display != null && !info.display.isRemoved()) info.display.discard();
        });
        ACTIVE_DAMAGE_DISPLAYS.clear();

        ACTIVE_STATUS_DISPLAYS.values().forEach(info -> {
            if (info != null && info.display != null && !info.display.isRemoved()) info.display.discard();
        });
        ACTIVE_STATUS_DISPLAYS.clear();

        ACTIVE_PHYSICS.clear();
    }

    public void clearActiveDisplays(LivingEntity entity) {
        int entityId = entity.getId();

        ACTIVE_DAMAGE_DISPLAYS.entrySet().removeIf(entry -> {
            DisplayInfo info = entry.getValue();
            if (info != null && info.targetEntityId == entityId) {
                if (info.display != null && !info.display.isRemoved()) info.display.discard();
                ACTIVE_PHYSICS.remove(entry.getKey());
                return true;
            }
            return false;
        });

        ACTIVE_STATUS_DISPLAYS.entrySet().removeIf(entry -> {
            DisplayInfo info = entry.getValue();
            if (info != null && info.targetEntityId == entityId) {
                if (info.display != null && !info.display.isRemoved()) info.display.discard();
                ACTIVE_PHYSICS.remove(entry.getKey());
                return true;
            }
            return false;
        });
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
                color
        );

        if (display != null) {
            serverLevel.addFreshEntity(display);

            UUID displayUuid = display.getUUID();
            ACTIVE_DAMAGE_DISPLAYS.put(displayUuid, new DisplayInfo(display, entityId, false));

            // [velX, velY, velZ, ticksAlive, maxTicks, originalColor, floatOffset]
            double randomX = (serverLevel.random.nextFloat() - 0.5f) * HORIZONTAL_DRIFT;
            double randomZ = (serverLevel.random.nextFloat() - 0.5f) * HORIZONTAL_DRIFT;
            ACTIVE_PHYSICS.put(displayUuid, new double[]{randomX, DAMAGE_INITIAL_VELOCITY_Y, randomZ, 0, DAMAGE_NUMBER_LIFETIME, color, 0});

            schedulePhysicsUpdate(serverLevel, displayUuid);

            LegendsOfTheStones.queueServerWork(DAMAGE_NUMBER_LIFETIME, () -> {
                DisplayInfo info = ACTIVE_DAMAGE_DISPLAYS.remove(displayUuid);
                if (info != null && !info.display.isRemoved()) {
                    ACTIVE_PHYSICS.remove(displayUuid);
                    info.display.discard();
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
                color
        );
        if (display != null) {
            serverLevel.addFreshEntity(display);

            UUID displayUuid = display.getUUID();
            ACTIVE_STATUS_DISPLAYS.put(displayUuid, new DisplayInfo(display, entityId, true));

            // [velX, velY, velZ, ticksAlive, maxTicks, originalColor, floatOffset]
            // Для статуса: используем floatOffset как фазу для парения
            double randomPhase = serverLevel.random.nextDouble() * Math.PI * 2;
            ACTIVE_PHYSICS.put(displayUuid, new double[]{0, 0, 0, 0, STATUS_TEXT_LIFETIME, color, randomPhase});

            schedulePhysicsUpdate(serverLevel, displayUuid);

            LegendsOfTheStones.queueServerWork(STATUS_TEXT_LIFETIME, () -> {
                DisplayInfo info = ACTIVE_STATUS_DISPLAYS.remove(displayUuid);
                if (info != null && !info.display.isRemoved()) {
                    ACTIVE_PHYSICS.remove(displayUuid);
                    info.display.discard();
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

            if (display == null || display.isRemoved() || physics == null) {
                ACTIVE_PHYSICS.remove(displayUuid);
                return;
            }

            physics[3]++;
            int ticksAlive = (int) physics[3];
            int maxTicks = (int) physics[4];
            int originalColor = (int) physics[5];
            double floatPhase = physics[6];

            // === РАЗНАЯ ЛОГИКА ДЛЯ УРОНА И СТАТУСА ===
            DisplayInfo info = ACTIVE_DAMAGE_DISPLAYS.get(displayUuid);
            if (info == null) info = ACTIVE_STATUS_DISPLAYS.get(displayUuid);

            if (info != null && info.isStatus) {
                // === СТАТУС: Парение + Переливание ===

                // 1. Плавное парение на месте (синусоида)
                floatPhase += STATUS_FLOAT_SPEED;
                physics[6] = floatPhase;
                double floatOffset = Math.sin(floatPhase) * STATUS_FLOAT_AMPLITUDE;
                display.setPos(display.getX(), display.getY() + floatOffset, display.getZ());

                // 2. Переливание цветом (пульсация между исходным цветом и белым)
                double pulse = (Math.sin(floatPhase * 2) + 1) / 2; // 0..1
                int r = (originalColor >> 16) & 0xFF;
                int g = (originalColor >> 8) & 0xFF;
                int b = originalColor & 0xFF;

                // Смешиваем с белым (255, 255, 255)
                int shimmerR = (int) (r + (255 - r) * pulse * 0.5);
                int shimmerG = (int) (g + (255 - g) * pulse * 0.5);
                int shimmerB = (int) (b + (255 - b) * pulse * 0.5);
                int shimmerColor = (shimmerR << 16) | (shimmerG << 8) | shimmerB;

                Component currentText = display.getText();
                if (currentText != null) {
                    display.setText(currentText.copy().withStyle(Style.EMPTY.withColor(shimmerColor).withBold(true)));
                }

            } else {
                // === УРОН: Падение с гравитацией ===

                physics[1] += DAMAGE_GRAVITY;
                display.setPos(display.getX() + physics[0], display.getY() + physics[1], display.getZ() + physics[2]);

                // Fade-out для урона
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

            if (!display.isRemoved() && ticksAlive < maxTicks) {
                schedulePhysicsUpdate(level, displayUuid);
            } else {
                ACTIVE_PHYSICS.remove(displayUuid);
            }
        });
    }

    // === СОЗДАНИЕ ===

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, Component textComponent, int color) {
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

        return display;
    }

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, String text, int color) {
        return createTextDisplay(level, x, y, z, Component.literal(text), color);
    }
}