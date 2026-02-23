package com.auranite.legendsofthestones.legendsofthestones.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import com.auranite.legendsofthestones.legendsofthestones.ElementType;

public class WetnessEffect extends MobEffect {
    public WetnessEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    public static float getAccumulationMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.10f; // +10% за уровень
    }
}