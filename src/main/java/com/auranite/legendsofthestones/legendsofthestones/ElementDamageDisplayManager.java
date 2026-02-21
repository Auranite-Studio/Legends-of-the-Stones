package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(Dist.CLIENT, modid = LegendsOfTheStones.MODID)
public class ElementDamageDisplayManager {

    // === КОНСТАНТЫ ОТОБРАЖЕНИЯ ===
    private static final int DAMAGE_NUMBER_LIFETIME = 20;
    private static final int STATUS_TEXT_LIFETIME = 40;
    private static final byte FLAG_SEE_THROUGH = 2;

    // === ХРАНИЛИЩА ДИСПЛЕЕВ ===
    private static final Map<Integer, TextDisplay> ACTIVE_DAMAGE_DISPLAYS = new ConcurrentHashMap<>();
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
        int cleanedCount = 0;

        Iterator<Map.Entry<Integer, TextDisplay>> damageIterator = ACTIVE_DAMAGE_DISPLAYS.entrySet().iterator();
        while (damageIterator.hasNext()) {
            Map.Entry<Integer, TextDisplay> entry = damageIterator.next();
            TextDisplay display = entry.getValue();
            if (display == null || display.isRemoved() || display.level() == null) {
                damageIterator.remove();
                cleanedCount++;
                continue;
            }
            Entity target = display.level().getEntity(entry.getKey());
            if (target == null || !target.isAlive()) {
                if (!display.isRemoved()) display.discard();
                damageIterator.remove();
                cleanedCount++;
            }
        }

        Iterator<Map.Entry<Integer, TextDisplay>> statusIterator = ACTIVE_STATUS_DISPLAYS.entrySet().iterator();
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
        ACTIVE_DAMAGE_DISPLAYS.values().forEach(display -> {
            if (display != null && !display.isRemoved()) display.discard();
        });
        ACTIVE_DAMAGE_DISPLAYS.clear();

        ACTIVE_STATUS_DISPLAYS.values().forEach(display -> {
            if (display != null && !display.isRemoved()) display.discard();
        });
        ACTIVE_STATUS_DISPLAYS.clear();
    }

    public void clearActiveDisplays(LivingEntity entity) {
        int entityId = entity.getId();
        TextDisplay oldDamage = ACTIVE_DAMAGE_DISPLAYS.remove(entityId);
        if (oldDamage != null && !oldDamage.isRemoved()) oldDamage.discard();

        TextDisplay oldStatus = ACTIVE_STATUS_DISPLAYS.remove(entityId);
        if (oldStatus != null && !oldStatus.isRemoved()) oldStatus.discard();
    }

    // === СОБЫТИЯ КЛИЕНТА ===
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        dev.foxgirl.damagenumbers.DamageNumbers.DamageNumbersHandler handler = dev.foxgirl.damagenumbers.DamageNumbers.getHandler();
        if (handler instanceof dev.foxgirl.damagenumbers.client.DamageNumbersImpl impl) {
            impl.onEntityHealthChange(event.getEntity(), 
                                      event.getEntity().getHealth() + event.getOriginalDamage(), 
                                      event.getEntity().getHealth());
        }
    }

    // === СПАВН ЭФФЕКТОВ ===

    public void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        int entityId = entity.getId();

        TextDisplay oldDisplay = ACTIVE_DAMAGE_DISPLAYS.remove(entityId);
        if (oldDisplay != null && !oldDisplay.isRemoved()) oldDisplay.discard();

        int color = getDamageColor(type);
        TextDisplay display = createTextDisplay(
                serverLevel,
                entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.5,
                entity.getZ(),
                String.format("%.1f", amount),
                color
        );
        if (display != null) {
            serverLevel.addFreshEntity(display);
            ACTIVE_DAMAGE_DISPLAYS.put(entityId, display);
            LegendsOfTheStones.queueServerWork(DAMAGE_NUMBER_LIFETIME, () -> {
                TextDisplay stored = ACTIVE_DAMAGE_DISPLAYS.remove(entityId);
                if (stored != null && !stored.isRemoved()) stored.discard();
            });
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

    // === СОЗДАНИЕ TEXTDISPLAY ===

    private static TextDisplay createTextDisplay(ServerLevel level, double x, double y, double z, Component textComponent, int color) {
        // ✅ ПРАВИЛЬНО: используем EntityType.TEXT_DISPLAY.create()
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