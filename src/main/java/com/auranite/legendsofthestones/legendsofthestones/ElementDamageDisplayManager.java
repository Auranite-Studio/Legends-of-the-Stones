package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ElementDamageDisplayManager {

    // === КОНСТАНТЫ ОТОБРАЖЕНИЯ ===
    private static final int DAMAGE_NUMBER_LIFETIME = 60; // Увеличенное время жизни для плавного движения
    private static final int STATUS_TEXT_LIFETIME = 40;
    private static final byte FLAG_SEE_THROUGH = 2;

    // === НОВАЯ СИСТЕМА ОТОБРАЖЕНИЯ УРОНА ===
    private static final Queue<DamageNumber> DAMAGE_NUMBERS = new ConcurrentLinkedQueue<>();

    public static class DamageNumber {
        public LivingEntity target;
        public float damage;
        public ElementType type;
        public long spawnTime;
        public Vec3 startPos;
        public Vec3 currentPos;
        public float alpha;

        public DamageNumber(LivingEntity target, float damage, ElementType type, long spawnTime) {
            this.target = target;
            this.damage = damage;
            this.type = type;
            this.spawnTime = spawnTime;
            this.startPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ());
            this.currentPos = this.startPos;
            this.alpha = 1.0f;
        }

        public boolean isExpired(long currentTime) {
            return currentTime - spawnTime >= DAMAGE_NUMBER_LIFETIME;
        }

        public float getProgress(long currentTime) {
            return Mth.clamp((float)(currentTime - spawnTime) / DAMAGE_NUMBER_LIFETIME, 0.0f, 1.0f);
        }

        public void updatePosition() {
            long currentTime = target.level().getGameTime();
            float progress = getProgress(currentTime);
            
            // Плавное движение вверх и исчезновение
            double newY = startPos.y + (progress * 1.5); // Поднимаем на 1.5 блока за время жизни
            this.currentPos = new Vec3(startPos.x, newY, startPos.z);
            
            // Плавное уменьшение прозрачности
            this.alpha = 1.0f - progress;
        }
    }

    // === ХРАНИЛИЩА ДИСПЛЕЕВ (для статусных текстов по-прежнему используем старую систему) ===
    private static final Map<Integer, TextDisplay> ACTIVE_STATUS_DISPLAYS = new ConcurrentHashMap<>();
    private static final Map<ElementType, Integer> DAMAGE_COLORS = new EnumMap<>(ElementType.class);

    // === РЕГИСТРАЦИЯ ЦВЕТОВ ===
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
        long currentTime = System.currentTimeMillis();
        
        // Очистка чисел урона
        DAMAGE_NUMBERS.removeIf(damageNum -> {
            if (damageNum.target == null || damageNum.target.isRemoved() || !damageNum.target.isAlive()) {
                return true;
            }
            return damageNum.isExpired(currentTime);
        });

        // Очистка статусных дисплеев
        Iterator<Map.Entry<Integer, TextDisplay>> statusIterator = ACTIVE_STATUS_DISPLAYS.entrySet().iterator();
        int cleanedCount = 0;
        while (statusIterator.hasNext()) {
            Map.Entry<Integer, TextDisplay> entry = statusIterator.next();
            TextDisplay display = entry.getValue();
            if (display == null || display.isRemoved() || display.level() == null) {
                statusIterator.remove();
                cleanedCount++;
                continue;
            }
            Entity target = display.level().getEntity(entry.getKey());
            if (target == null || !target.isAlive()) {
                if (!display.isRemoved()) display.discard();
                statusIterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            LegendsOfTheStones.LOGGER.debug("ElementDamageDisplayManager: cleaned {} stale displays", cleanedCount);
        }
    }

    public void cleanupAllDisplays() {
        DAMAGE_NUMBERS.clear();
        
        ACTIVE_STATUS_DISPLAYS.values().forEach(display -> {
            if (display != null && !display.isRemoved()) display.discard();
        });
        ACTIVE_STATUS_DISPLAYS.clear();
    }

    public void clearActiveDisplays(LivingEntity entity) {
        int entityId = entity.getId();
        TextDisplay oldStatus = ACTIVE_STATUS_DISPLAYS.remove(entityId);
        if (oldStatus != null && !oldStatus.isRemoved()) oldStatus.discard();
    }

    // === СПАВН ЭФФЕКТОВ ===

    public void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
        if (entity.level().isClientSide()) {
            // На клиенте добавляем в очередь для отображения
            DAMAGE_NUMBERS.offer(new DamageNumber(entity, amount, type, entity.level().getGameTime()));
        } else {
            // На сервере по-прежнему отправляем информацию клиентам через сеть
            // Здесь можно реализовать отправку сетевых пакетов клиентам в радиусе
        }
    }

    public void spawnStatusText(LivingEntity entity, Component textComponent, int color) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        int entityId = entity.getId();

        TextDisplay oldStatus = ACTIVE_STATUS_DISPLAYS.remove(entityId);
        if (oldStatus != null && !oldStatus.isRemoved()) oldStatus.discard();

        TextDisplay display = createTextDisplay(
                serverLevel,
                entity.getX(),
                entity.getY() + entity.getBbHeight() + 1.2,
                entity.getZ(),
                textComponent,
                color
        );
        if (display != null) {
            serverLevel.addFreshEntity(display);
            ACTIVE_STATUS_DISPLAYS.put(entityId, display);
            LegendsOfTheStones.queueServerWork(STATUS_TEXT_LIFETIME, () -> {
                TextDisplay stored = ACTIVE_STATUS_DISPLAYS.remove(entityId);
                if (stored != null && !stored.isRemoved()) stored.discard();
            });
        }
    }

    public void spawnStatusText(LivingEntity entity, String text, int color) {
        spawnStatusText(entity, Component.literal(text), color);
    }

    // === СИСТЕМА ОТРИСОВКИ ЧИСЕЛ УРОНА ===

    public static void renderDamageNumbers(Minecraft minecraft, PoseStack poseStack) {
        if (DAMAGE_NUMBERS.isEmpty()) return;

        EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
        Font font = minecraft.font;

        long currentTime = System.currentTimeMillis();
        DAMAGE_NUMBERS.removeIf(damageNum -> {
            if (damageNum.target == null || damageNum.target.isRemoved() || !damageNum.target.isAlive()) {
                return true;
            }

            damageNum.updatePosition();

            // Получаем позицию камеры для правильного поворота текста
            Vec3 cameraPos = entityRenderDispatcher.camera.getPosition();
            
            // Обновляем позицию каждый тик для отслеживания движения цели
            damageNum.currentPos = new Vec3(
                damageNum.target.getX(), 
                damageNum.target.getY() + damageNum.target.getBbHeight() + 0.5 + (damageNum.getProgress(currentTime) * 1.5), 
                damageNum.target.getZ()
            );

            // Вычисляем расстояние до игрока
            double distanceSquared = cameraPos.distanceToSqr(damageNum.currentPos);
            if (distanceSquared > 4096.0) { // Не отображаем если слишком далеко (64^2)
                return false; // Не удаляем, так как может приблизиться
            }

            // Пропускаем если слишком близко к игроку (внутри хитбокса)
            if (distanceSquared < 1.0) {
                return false;
            }

            // Подготавливаем матрицу преобразования для текста
            poseStack.pushPose();
            
            // Переводим в мировые координаты
            poseStack.translate(
                damageNum.currentPos.x - cameraPos.x,
                damageNum.currentPos.y - cameraPos.y,
                damageNum.currentPos.z - cameraPos.z
            );

            // Поворачиваем текст лицом к камере
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            
            // Масштабируем текст в зависимости от расстояния
            float scale = (float) Math.sqrt(distanceSquared) * 0.02f;
            if (scale < 0.5f) scale = 0.5f; // Минимальный масштаб
            poseStack.scale(scale, scale, scale);

            // Инвертируем ось Y чтобы текст не был перевернутым
            poseStack.mulPose(net.minecraft.core.Direction.NORTH.getRotation());

            // Получаем цвет для типа урона
            int color = getDamageColor(damageNum.type);
            
            // Добавляем прозрачность
            int alpha = (int) (damageNum.alpha * 255);
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;
            
            int colorWithAlpha = (color & 0x00FFFFFF) | (alpha << 24);

            // Отображаем текст
            String damageText = String.format("%.1f", damageNum.damage);
            
            // Центрируем текст
            int textWidth = font.width(damageText);
            poseStack.translate(-textWidth / 2.0, -font.lineHeight / 2.0, 0.0);
            
            MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
            font.drawInBatch(
                damageText,
                0, 0, 
                colorWithAlpha, 
                false, 
                poseStack.last().pose(), 
                buffer, 
                Font.DisplayMode.SEE_THROUGH, 
                0, 
                15728880 // Световой уровень
            );
            buffer.endBatch();

            poseStack.popPose();

            return damageNum.isExpired(currentTime);
        });
    }

    // === СОЗДАНИЕ TEXTDISPLAY (для статусных текстов) ===

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, Component textComponent, int color) {
        TextDisplay display = EntityType.TEXT_DISPLAY.create(level);

        if (display == null) {
            LegendsOfTheStones.LOGGER.error("Failed to create TextDisplay entity at ({}, {}, {})", x, y, z);
            return null;
        }

        display.setPos(x, y, z);
        display.setText(textComponent.copy().withStyle(Style.EMPTY.withColor(color)));
        display.setBackgroundColor(0x00000000);
        display.setFlags(FLAG_SEE_THROUGH);
        display.setLineWidth(200);
        display.setBillboardConstraints(BillboardConstraints.CENTER);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setDeltaMovement(0, 0, 0);
        display.setViewRange(64.0f);
        display.setTransformationInterpolationDuration(0);
        display.setTransformationInterpolationDelay(0);
        display.setPosRotInterpolationDuration(0);

        return display;
    }

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, String text, int color) {
        return createTextDisplay(level, x, y, z, Component.literal(text), color);
    }
}