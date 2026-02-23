package com.auranite.legendsofthestones.legendsofthestones;

import com.auranite.legendsofthestones.legendsofthestones.effect.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class LegendsOfTheStonesMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, LegendsOfTheStones.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> BURNING = REGISTRY.register("burning", () -> new BurningEffect(0xFF5500));
    public static final DeferredHolder<MobEffect, MobEffect> WETNESS = REGISTRY.register("wetness", () -> new WetnessEffect(0x0080FF));
    public static final DeferredHolder<MobEffect, MobEffect> STUN = REGISTRY.register("stun", () -> new StunEffect(0x8B4513));
    public static final DeferredHolder<MobEffect, MobEffect> FREEZE = REGISTRY.register("freeze", () -> new FreezeEffect(0x00BFFF));
    public static final DeferredHolder<MobEffect, MobEffect> SHOCK = REGISTRY.register("shock", () -> new ShockEffect(0x9932CC)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE,
                    ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "shock_damage_reduction"),
                    -0.10,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> BREAK = REGISTRY.register("break", () -> new BreakEffect(0x9400D3));
    public static final DeferredHolder<MobEffect, MobEffect> BLOOM = REGISTRY.register("bloom", () -> new BloomEffect(0x32CD32));
    public static final DeferredHolder<MobEffect, MobEffect> RIFT = REGISTRY.register("rift", () -> new RiftEffect(0xFF5C77));

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null) {
            expireEffects(event.getEntity(), effectInstance);
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null) {
            expireEffects(event.getEntity(), effectInstance);
        }
    }

    private static void expireEffects(Entity entity, MobEffectInstance effectInstance) {
    }
}