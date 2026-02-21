package com.auranite.legendsofthestones.legendsofthestones;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3f;

public class ElementDamageDisplayManager {

    // === КОНСТАНТЫ ОТОБРАЖЕНИЯ ===
    private static final int DAMAGE_NUMBER_LIFETIME = 20;
    private static final int STATUS_TEXT_LIFETIME = 40;
    private static final byte FLAG_SEE_THROUGH = 2;

    // === ХРАНИЛИЩА ДИСПЛЕЕВ ===
    // We're replacing TextDisplay with particles, so we no longer need these maps
    // private static final Map<Integer, TextDisplay> ACTIVE_DAMAGE_DISPLAYS = new ConcurrentHashMap<>();
    // private static final Map<Integer, TextDisplay> ACTIVE_STATUS_DISPLAYS = new ConcurrentHashMap<>();
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
        // Since we're using particles instead of TextDisplay entities, no cleanup needed
        // This method now serves as a placeholder for future particle cleanup if needed
    }

    public void cleanupAllDisplays() {
        // Since we're using particles instead of TextDisplay entities, no cleanup needed
        // This method now serves as a placeholder for future particle cleanup if needed
    }

    public void clearActiveDisplays(LivingEntity entity) {
        // Since we're using particles instead of TextDisplay entities, no cleanup needed
        // This method now serves as a placeholder for future particle cleanup if needed
    }

    // === СПАВН ЭФФЕКТОВ ===

    public void spawnDamageNumber(LivingEntity entity, float amount, ElementType type) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        
        // Convert damage amount to string
        String damageString = String.format("%.1f", amount);
        
        // Get color for the element type
        int color = getDamageColor(type);
        
        // Extract RGB values from the color
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        // Create dust particle with the element's color
        DustParticleOptions particleOptions = new DustParticleOptions(new Vector3f(red, green, blue), 1.0F);
        
        // Spawn particles at the entity's position with some offset above
        Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.5, 0);
        
        // Spawn multiple particles to create a more visible effect
        for (int i = 0; i < 10; i++) {
            // Add slight random offset to spread particles
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            
            serverLevel.sendParticles(
                particleOptions,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1, // count
                0, // speed (for dust particles)
                0, // x offset
                0, // y offset
                0  // z offset
            );
        }
    }

    public void spawnStatusText(LivingEntity entity, Component textComponent, int color) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        
        // Extract RGB values from the color
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        // Create dust particle with the specified color
        DustParticleOptions particleOptions = new DustParticleOptions(new Vector3f(red, green, blue), 1.0F);
        
        // Spawn particles at the entity's position with some offset above
        Vec3 pos = entity.position().add(0, entity.getBbHeight() + 1.2, 0);
        
        // Spawn multiple particles to create a more visible effect
        for (int i = 0; i < 10; i++) {
            // Add slight random offset to spread particles
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            
            serverLevel.sendParticles(
                particleOptions,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1, // count
                0, // speed (for dust particles)
                0, // x offset
                0, // y offset
                0  // z offset
            );
        }
    }

    public void spawnStatusText(LivingEntity entity, String text, int color) {
        spawnStatusText(entity, Component.literal(text), color);
    }

    // === СОЗДАНИЕ PARTICLE EFFECTS ===
    
    // The original TextDisplay creation methods are no longer needed since we're using particles
    // The functionality has been replaced with particle spawning in spawnDamageNumber and spawnStatusText methods
}