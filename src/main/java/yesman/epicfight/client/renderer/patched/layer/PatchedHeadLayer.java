package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class PatchedHeadLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E> & HeadedModel> extends PatchedLayer<E, T, M, CustomHeadLayer<E, M>> {
	@Override
	protected void renderLayer(T entitypatch, E entityliving, CustomHeadLayer<E, M> vanillaLayer, PoseStack postStack, MultiBufferSource buffer, int packedLightIn, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		ItemStack itemstack = entityliving.getItemBySlot(EquipmentSlot.HEAD);
		
		if (!itemstack.isEmpty()) {
			ModelPart model = vanillaLayer.getParentModel().getHead();
			E entity = entitypatch.getOriginal();
			OpenMatrix4f modelMatrix = new OpenMatrix4f();
			modelMatrix.scale(new Vec3f(-1.0F, -1.0F, 1.0F)).mulFront(poses[9]);
			model.x = 0;
			model.y = 0;
			model.z = 0;
			model.xRot = 0;
			model.yRot = 0;
			model.zRot = 0;
			OpenMatrix4f transpose = OpenMatrix4f.transpose(modelMatrix, null);
			postStack.pushPose();
			
			MathUtils.translateStack(postStack, modelMatrix);
			MathUtils.rotateStack(postStack, transpose);
			
			if (entitypatch.getOriginal().isBaby()) {
				postStack.translate(0.0F, -1.2F, 0.0F);
				postStack.scale(1.6F, 1.6F, 1.6F);
			}
			
			vanillaLayer.render(postStack, buffer, packedLightIn, entity, entity.walkAnimation.position(), entity.walkAnimation.speed(), packedLightIn, entity.tickCount, yRot, xRot);
			postStack.popPose();
		}
	}
}