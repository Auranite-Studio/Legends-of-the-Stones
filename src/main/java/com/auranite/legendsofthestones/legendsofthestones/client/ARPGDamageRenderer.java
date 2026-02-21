package com.auranite.legendsofthestones.legendsofthestones.client;

import com.auranite.legendsofthestones.legendsofthestones.ElementDamageDisplayManager;
import com.auranite.legendsofthestones.legendsofthestones.LegendsOfTheStones;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
@OnlyIn(Dist.CLIENT)
public class ARPGDamageRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            try {
                ElementDamageDisplayManager.renderDamageNumbers(Minecraft.getInstance(), event.getPoseStack());
            } catch (Exception e) {
                LegendsOfTheStones.LOGGER.error("Error rendering damage numbers", e);
            }
        }
    }
}