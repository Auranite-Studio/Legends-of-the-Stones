package com.auranite.legendsofthestones.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class WetnessEffect extends MobEffect {

    public WetnessEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}