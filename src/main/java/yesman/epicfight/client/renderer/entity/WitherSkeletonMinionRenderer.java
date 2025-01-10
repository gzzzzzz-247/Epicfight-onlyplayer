package yesman.epicfight.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import yesman.epicfight.main.EpicFightMod;

public class WitherSkeletonMinionRenderer extends WitherSkeletonRenderer {

	private static final ResourceLocation WITHER_SKELETON_LOCATION = new ResourceLocation(EpicFightMod.MODID, "textures/entity/wither_skeleton_minion.png");

	public WitherSkeletonMinionRenderer(Context context) {
		super(context);
	}

	public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
		return WITHER_SKELETON_LOCATION;
	}
}