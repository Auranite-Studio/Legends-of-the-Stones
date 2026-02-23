package com.auranite.legendsofthestones.legendsofthestones.effect;

import com.auranite.legendsofthestones.legendsofthestones.ElementType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BreakEffect extends MobEffect {
    public BreakEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}