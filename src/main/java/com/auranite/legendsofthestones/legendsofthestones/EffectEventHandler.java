package com.auranite.legendsofthestones.legendsofthestones;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority; // ✅ ИМПОРТ
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.entity.LivingEntity;

@EventBusSubscriber(modid = LegendsOfTheStones.MODID)
public class EffectEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST) // ✅ Запускается ПЕРВЫМ
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity e ? e : null;

        // 🔹 Shock: снижение урона от атакующего
        if (attacker != null && attacker.hasEffect(LegendsOfTheStonesMobEffects.SHOCK)) {
            int amplifier = attacker.getEffect(LegendsOfTheStonesMobEffects.SHOCK).getAmplifier();
            float reduction = 1.0f - ((amplifier + 1) * 0.10f);
            event.setNewDamage(event.getNewDamage() * reduction);
        }

        // 🔹 BREAK: игнорирование брони
        if (target.hasEffect(LegendsOfTheStonesMobEffects.BREAK)) {
            // Логика игнорирования брони
        }

        // 🔹 RIFT: увеличение входящего урона
        if (target.hasEffect(LegendsOfTheStonesMobEffects.RIFT)) {
            int amplifier = target.getEffect(LegendsOfTheStonesMobEffects.RIFT).getAmplifier();
            float multiplier = 1.0f + (amplifier + 1) * 0.15f;
            event.setNewDamage(event.getNewDamage() * multiplier);
        }

        // ✅ BLOOM: универсальная уязвимость +25%
        if (target.hasEffect(LegendsOfTheStonesMobEffects.BLOOM)) {
            float universalVulnerability = 1.25f;
            event.setNewDamage(event.getNewDamage() * universalVulnerability);
        }
    }
}