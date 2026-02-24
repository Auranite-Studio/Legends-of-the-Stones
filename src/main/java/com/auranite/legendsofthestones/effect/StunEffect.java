package com.auranite.legendsofthestones.effect;

import com.auranite.legendsofthestones.LegendsOfTheStones;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StunEffect extends MobEffect {
    public StunEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.addAttributeModifier(Attributes.ARMOR, ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "effect.stun_0"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "effect.stun_1"), -99.99, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "effect.stun_2"), -99.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "effect.stun_3"), -99.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath(LegendsOfTheStones.MODID, "effect.stun_4"), -99.99, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        return true;
    }
}