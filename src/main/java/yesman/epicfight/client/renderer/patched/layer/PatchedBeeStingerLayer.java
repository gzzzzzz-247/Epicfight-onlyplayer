package yesman.epicfight.client.renderer.patched.layer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class PatchedBeeStingerLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends PlayerModel<E>> extends PatchedStuckInBodyLayer<E, T, M, BeeStingerLayer<E, M>> {
	private static final ResourceLocation BEE_STINGER_LOCATION = new ResourceLocation("textures/entity/bee/bee_stinger.png");
	
	protected void renderStuckItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity, float pf1, float pf2, float pf3, float partialTick) {
		float f = Mth.sqrt(pf1 * pf1 + pf3 * pf3);
		float f1 = (float) (Math.atan2((double) pf1, (double) pf3) * (double) (180F / (float) Math.PI));
		float f2 = (float) (Math.atan2((double) pf2, (double) f) * (double) (180F / (float) Math.PI));
		poseStack.translate(0.0F, 0.0F, 0.0F);
		poseStack.mulPose(Axis.YP.rotationDegrees(f1 - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(f2));
		poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
		poseStack.scale(0.03125F, 0.03125F, 0.03125F);
		poseStack.translate(2.5F, 0.0F, 0.0F);
		VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BEE_STINGER_LOCATION));
		
		for (int i = 0; i < 4; ++i) {
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			PoseStack.Pose posestack$pose = poseStack.last();
			Matrix4f matrix4f = posestack$pose.pose();
			Matrix3f matrix3f = posestack$pose.normal();
			vertex(vertexconsumer, matrix4f, matrix3f, -4.5F, -1, 0.0F, 0.0F, packedLight);
			vertex(vertexconsumer, matrix4f, matrix3f, 4.5F, -1, 0.125F, 0.0F, packedLight);
			vertex(vertexconsumer, matrix4f, matrix3f, 4.5F, 1, 0.125F, 0.0625F, packedLight);
			vertex(vertexconsumer, matrix4f, matrix3f, -4.5F, 1, 0.0F, 0.0625F, packedLight);
		}
	}
	
	private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, float x, int y, float z, float u, int v) {
		vertexConsumer.vertex(pose, x, (float) y, 0.0F).color(255, 255, 255, 255).uv(z, u).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(v).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
	}
	
	@Override
	protected int numStuck(E entity) {
		return entity.getStingerCount();
	}
}
