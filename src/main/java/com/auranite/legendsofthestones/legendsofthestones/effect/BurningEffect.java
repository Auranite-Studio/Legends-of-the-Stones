package com.auranite.legendsofthestones.legendsofthestones.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import com.auranite.legendsofthestones.legendsofthestones.ElementType;

public class BurningEffect extends MobEffect {
    public BurningEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.isOnFire()) {
            entity.igniteForSeconds(1);
        }
        return true;
    }
}