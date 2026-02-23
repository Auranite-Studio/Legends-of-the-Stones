package com.auranite.legendsofthestones.legendsofthestones.effect;

import com.auranite.legendsofthestones.legendsofthestones.LegendsOfTheStones;
import com.auranite.legendsofthestones.legendsofthestones.LegendsOfTheStonesMobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.LivingEntity;


@EventBusSubscriber(modid = LegendsOfTheStones.MODID)
public class BreakEffectHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        if (target.hasEffect(LegendsOfTheStonesMobEffects.BREAK)) {
            LegendsOfTheStones.LOGGER.debug("BREAK on {}: 100% armor ignored, raw damage = {}",
                    target.getName().getString(), event.getAmount());
        }
    }
}