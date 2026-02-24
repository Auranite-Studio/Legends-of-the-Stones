package com.auranite.legendsofthestones.mixins;

import com.auranite.legendsofthestones.LegendsOfTheStonesPlayerAnimationAPI;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerAnimationRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public PlayerAnimationRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> entityModel, float f) {
		super(context, entityModel, f);
	}

	private String master = null;

	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void hideBonesInFirstPerson(AbstractClientPlayer entity, float f, float g, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
		if (master == null) {
			if (!LegendsOfTheStonesPlayerAnimationAPI.animations.isEmpty())
				master = "power";
			else
				return;
		}
		if (!master.equals("power")) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (entity.getPersistentData().getBoolean("FirstPersonAnimation") && mc.options.getCameraType().isFirstPerson() && entity == mc.player) {
			this.model.head.visible = false;
			this.model.body.visible = false;
			this.model.leftLeg.visible = false;
			this.model.rightLeg.visible = false;
			this.model.rightArm.visible = false;
			this.model.leftArm.visible = false;
			this.model.hat.visible = false;
			this.model.leftSleeve.visible = false;
			this.model.rightSleeve.visible = false;
			this.model.leftPants.visible = false;
			this.model.rightPants.visible = false;
			this.model.jacket.visible = false;
			this.model.rightArm.visible = true;
			this.model.rightSleeve.visible = true;
			this.model.leftArm.visible = true;
			this.model.leftSleeve.visible = true;
		}
	}

	@Inject(method = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("RETURN"))
	private void setupRotations(AbstractClientPlayer player, PoseStack poseStack, float f, float bodyYaw, float deltaTick, float g, CallbackInfo ci) {
		if (master == null) {
			if (!LegendsOfTheStonesPlayerAnimationAPI.animations.isEmpty())
				master = "power";
			else
				return;
		}
		if (!master.equals("power")) {
			return;
		}
		LegendsOfTheStonesPlayerAnimationAPI.PlayerAnimation animation = LegendsOfTheStonesPlayerAnimationAPI.active_animations.get(player);
		if (animation == null)
			return;
		LegendsOfTheStonesPlayerAnimationAPI.PlayerBone bone = animation.bones.get("body");
		if (bone == null)
			return;
		float animationProgress = player.getPersistentData().getFloat("PlayerAnimationProgress");
		Vec3 scale = LegendsOfTheStonesPlayerAnimationAPI.PlayerBone.interpolate(bone.scales, animationProgress);
		if (scale != null) {
			poseStack.scale((float) scale.x, (float) scale.y, (float) scale.z);
		}
		Vec3 position = LegendsOfTheStonesPlayerAnimationAPI.PlayerBone.interpolate(bone.positions, animationProgress);
		if (position != null) {
			poseStack.translate((float) -position.x * 0.0625f, (float) (position.y * 0.0625f), (float) position.z * 0.0625f);
		}
		Vec3 rotation = LegendsOfTheStonesPlayerAnimationAPI.PlayerBone.interpolate(bone.rotations, animationProgress);
		if (rotation != null) {
			poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotation.z));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) -rotation.y));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) -rotation.x));
		}
	}
}