package com.auranite.legendsofthestones.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void renderHandsWithItems(float f, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
		if (localPlayer instanceof Player player && player.getPersistentData().getBoolean("FirstPersonAnimation") && Minecraft.getInstance().player == player) {
			ci.cancel();
		}
	}
}