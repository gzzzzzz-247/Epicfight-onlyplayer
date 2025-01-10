package yesman.epicfight.compat;

import com.mojang.blaze3d.vertex.PoseStack;

import de.teamlapen.vampirism.client.renderer.entity.layers.VampirePlayerHeadLayer;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.entity.player.VampirismPlayerAttributes;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.ModelRenderLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.mixin.VampirismMixinVampirePlayerHeadLayer;

public class VampirismCompat implements ICompatModule {
	@Override
	public void onModEventBus(IEventBus eventBus) {
		
	}

	@Override
	public void onForgeEventBus(IEventBus eventBus) {
		
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onModEventBusClient(IEventBus eventBus) {
		eventBus.<PatchedRenderersEvent.Modify>addListener((event) -> {
			if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerrenderer) {
				playerrenderer.addPatchedLayerAlways(VampirePlayerHeadLayer.class, new EpicFightVampirePlayerHeadLayer());
			}
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void onForgeEventBusClient(IEventBus eventBus) {
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class EpicFightVampirePlayerHeadLayer extends ModelRenderLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, VampirePlayerHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>, HumanoidMesh> {
		public EpicFightVampirePlayerHeadLayer() {
			super(() -> Meshes.BIPED);
		}
		
		@Override
		protected void renderLayer( AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch
				                  , AbstractClientPlayer entityliving
				                  , VampirePlayerHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> vanillaLayer
				                  , PoseStack poseStack
				                  , MultiBufferSource buffer
				                  , int packedLight
				                  , OpenMatrix4f[] poses
				                  , float bob
				                  , float yRot
				                  , float xRot
				                  , float partialTicks
				                  )
		{
			if (!VampirismConfig.CLIENT.renderVampireEyes.get() || !entityliving.isAlive()) {
				return;
			}
			
	        VampirismPlayerAttributes atts = VampirismPlayerAttributes.get(entityliving);
	        
	        if (atts.vampireLevel > 0 && !atts.getVampSpecial().disguised && !entityliving.isInvisible()) {
	        	VampirismMixinVampirePlayerHeadLayer accessor = ((VampirismMixinVampirePlayerHeadLayer)vanillaLayer);
	        	
	        	int eyeType = Math.max(0, Math.min(atts.getVampSpecial().eyeType, accessor.getEyeOverlays().length - 1));
	            int fangType = Math.max(0, Math.min(atts.getVampSpecial().fangType, accessor.getFangOverlays().length - 1));
	            int packerOverlay = LivingEntityRenderer.getOverlayCoords(entityliving, 0);
	            
	            RenderType eyeRenderType = EpicFightRenderTypes.getTriangulated(atts.getVampSpecial().glowingEyes ? RenderType.eyes(accessor.getEyeOverlays()[eyeType]) : RenderType.entityCutoutNoCull(accessor.getEyeOverlays()[eyeType]));
	            this.mesh.get().draw(poseStack, buffer, eyeRenderType, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, packerOverlay, entitypatch.getArmature(), poses);
	            
	            RenderType fangRenderType = EpicFightRenderTypes.getTriangulated(RenderType.entityCutoutNoCull(accessor.getFangOverlays()[fangType]));
	            this.mesh.get().draw(poseStack, buffer, fangRenderType, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, packerOverlay, entitypatch.getArmature(), poses);
	            
	        }
		}
	}
}
