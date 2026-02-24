package com.auranite.legendsofthestones.events;

import com.auranite.legendsofthestones.ElementDamageDisplayManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ModEventHandlers {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ElementDamageDisplayManager.cleanupOrphanedDisplaysOnWorldLoad(serverLevel);
        }
    }
}