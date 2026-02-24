package com.auranite.legendsofthestones.effect;

import com.auranite.legendsofthestones.ElementDamageHandler;
import com.auranite.legendsofthestones.ElementType;
import com.auranite.legendsofthestones.LegendsOfTheStonesMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class BloomEffect extends MobEffect {
    public BloomEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // ✅ Выполнять логику только на сервере
        if (entity.level().isClientSide) {
            return true;
        }

        // ✅ Получаем длительность через Holder
        MobEffectInstance effectInstance = entity.getEffect(LegendsOfTheStonesMobEffects.BLOOM);
        if (effectInstance == null) {
            return false;
        }
        int duration = effectInstance.getDuration();


            if (duration % 20 == 0) {
                float damage = 1.0f + amplifier * 0.5f;
                ElementDamageHandler.dealElementDamage(entity, ElementType.NATURAL, damage, 0);
            }

        return true;
    }
}