package com.auranite.legendsofthestones.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
	private static void renderShadow(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (entity instanceof Player player && mc.options.getCameraType().isFirstPerson() && player == mc.player) {
			if (player.getPersistentData().getBoolean("FirstPersonAnimation"))
				ci.cancel();
		}
	}
}