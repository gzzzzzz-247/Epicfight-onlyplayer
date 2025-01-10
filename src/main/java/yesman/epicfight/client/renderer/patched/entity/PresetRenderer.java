package yesman.epicfight.client.renderer.patched.entity;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.forgeevent.PrepareModelEvent;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.LayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.LayerUtil;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.mixin.MixinLivingEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * This class is for LivingEntity with renderer that doesn't extend LivingEntityRenderer (e.g. Geckolib renderer based entities)
 */
@OnlyIn(Dist.CLIENT)
public class PresetRenderer extends PatchedEntityRenderer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityRenderer<LivingEntity>, AnimatedMesh> implements LayerRenderer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>> {
	private final LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> presetRenderer;
	protected final Map<Class<?>, PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, ? extends RenderLayer<LivingEntity, EntityModel<LivingEntity>>>> patchedLayers = Maps.newHashMap();
	protected final List<PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, ? extends RenderLayer<LivingEntity, EntityModel<LivingEntity>>>> customLayers = Lists.newArrayList();
	protected final MeshProvider<AnimatedMesh> mesh;
	
	public PresetRenderer(EntityRendererProvider.Context context, EntityType<?> entityType, LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer, MeshProvider<AnimatedMesh> mesh) {
		this.presetRenderer = renderer;
		this.mesh = mesh;
		
		ResourceLocation type = EntityType.getKey(entityType);
		FileToIdConverter filetoidconverter = FileToIdConverter.json("animated_layers/" + type.getPath());
		List<Pair<ResourceLocation, JsonElement>> layers = Lists.newArrayList();
		
		for (Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(context.getResourceManager()).entrySet()) {
			Reader reader = null;
			
			try {
				reader = entry.getValue().openAsReader();
				JsonElement jsonelement = GsonHelper.fromJson(new GsonBuilder().create(), reader, JsonElement.class);
				layers.add(Pair.of(entry.getKey(), jsonelement));
			} catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
				EpicFightMod.LOGGER.error("Failed to parse layer file {} for {}", entry.getKey(), type);
				jsonparseexception.printStackTrace();
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
				}
			}
		}
		
		LayerUtil.addLayer(this, entityType, layers);
	}
	
	@Override
	public void render(LivingEntity entity, LivingEntityPatch<LivingEntity> entitypatch, EntityRenderer<LivingEntity> renderer, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		super.render(entity, entitypatch, renderer, buffer, poseStack, packedLight, partialTicks);
		
		Minecraft mc = Minecraft.getInstance();
		MixinLivingEntityRenderer livingEntityRendererAccessor = (MixinLivingEntityRenderer)this.presetRenderer;
		
		boolean isVisible = livingEntityRendererAccessor.invokeIsBodyVisible(entity);
		boolean isVisibleToPlayer = !isVisible && !entity.isInvisibleTo(mc.player);
		boolean isGlowing = mc.shouldEntityAppearGlowing(entity);
		RenderType renderType = livingEntityRendererAccessor.invokeGetRenderType(entity, isVisible, isVisibleToPlayer, isGlowing);
		Armature armature = entitypatch.getArmature();
		poseStack.pushPose();
		this.mulPoseStack(poseStack, armature, entity, entitypatch, partialTicks);
		OpenMatrix4f[] poseMatrices = this.getPoseMatrices(entitypatch, armature, partialTicks, false);
		
		if (renderType != null) {
		    this.prepareVanillaModel(entity, this.presetRenderer.getModel(), this.presetRenderer, partialTicks);
			AnimatedMesh mesh = this.getMeshProvider(entitypatch).get();
			this.prepareModel(mesh, entity, entitypatch, this.presetRenderer);
			
			PrepareModelEvent prepareModelEvent = new PrepareModelEvent(this, mesh, entitypatch, buffer, poseStack, packedLight, partialTicks);
			
			if (!MinecraftForge.EVENT_BUS.post(prepareModelEvent)) {
				mesh.draw(poseStack, buffer, renderType, packedLight, 1.0F, 1.0F, 1.0F, isVisibleToPlayer ? 0.15F : 1.0F, this.getOverlayCoord(entity, entitypatch, partialTicks), armature, poseMatrices);
			}
		}
		
		if (!entity.isSpectator()) {
			this.renderLayer(this.presetRenderer, entitypatch, entity, poseMatrices, buffer, poseStack, packedLight, partialTicks);
		}
		
		if (renderType != null) {
			if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
				for (Layer layer : entitypatch.getClientAnimator().getAllLayers()) {
					AnimationPlayer animPlayer = layer.animationPlayer;
					float playTime = animPlayer.getPrevElapsedTime() + (animPlayer.getElapsedTime() - animPlayer.getPrevElapsedTime()) * partialTicks;
					animPlayer.getAnimation().renderDebugging(poseStack, buffer, entitypatch, playTime, partialTicks);
				}
			}
		}
		
		poseStack.popPose();
	}
	
	// can't transform the access modifier of getBob method because of overriding
	public float getVanillaRendererBob(LivingEntity entity, LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer, float partialTicks) {
		return entity.tickCount + partialTicks;
	}
	
	protected void prepareVanillaModel(LivingEntity entityIn, EntityModel<LivingEntity> model, LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer, float partialTicks) {
		boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit());
		model.riding = shouldSit;
		model.young = entityIn.isBaby();
		float f = Mth.rotLerp(partialTicks, entityIn.yBodyRotO, entityIn.yBodyRot);
		float f1 = Mth.rotLerp(partialTicks, entityIn.yHeadRotO, entityIn.yHeadRot);
		float f2 = f1 - f;
		
		if (shouldSit && entityIn.getVehicle() instanceof LivingEntity livingentity) {
			f = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
			f2 = f1 - f;
			float f3 = Mth.wrapDegrees(f2);
			if (f3 < -85.0F) {
				f3 = -85.0F;
			}

			if (f3 >= 85.0F) {
				f3 = 85.0F;
			}

			f = f1 - f3;
			if (f3 * f3 > 2500.0F) {
				f += f3 * 0.2F;
			}

			f2 = f1 - f;
		}

		float f6 = Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot());
		
		if (LivingEntityRenderer.isEntityUpsideDown(entityIn)) {
			f6 *= -1.0F;
			f2 *= -1.0F;
		}
		
		float f7 = this.getVanillaRendererBob(entityIn, renderer, partialTicks);
		float f8 = 0.0F;
		float f5 = 0.0F;
		
		if (!shouldSit && entityIn.isAlive()) {
			f8 = entityIn.walkAnimation.speed(partialTicks);
			f5 = entityIn.walkAnimation.position() - entityIn.walkAnimation.speed() * (1.0F - partialTicks);
			if (entityIn.isBaby()) {
				f5 *= 3.0F;
			}

			if (f8 > 1.0F) {
				f8 = 1.0F;
			}
		}
		
		model.prepareMobModel(entityIn, f5, f8, partialTicks);
		model.setupAnim(entityIn, f5, f8, f7, f2, f6);
	}
	
	protected void prepareModel(AnimatedMesh mesh, LivingEntity entity, LivingEntityPatch<LivingEntity> entitypatch, LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer) {
		mesh.initialize();
	}
	
	protected void renderLayer(LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer, LivingEntityPatch<LivingEntity> entitypatch, LivingEntity entity, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		float f = MathUtils.lerpBetween(entity.yBodyRotO, entity.yBodyRot, partialTicks);
        float f1 = MathUtils.lerpBetween(entity.yHeadRotO, entity.yHeadRot, partialTicks);
        float f2 = f1 - f;
		float f7 = entity.getViewXRot(partialTicks);
		float bob = this.getVanillaRendererBob(entity, renderer, partialTicks);
		
		for (RenderLayer<LivingEntity, EntityModel<LivingEntity>> layer : renderer.layers) {
			Class<?> layerClass = layer.getClass();
			
			if (layerClass.isAnonymousClass()) {
				layerClass = layerClass.getSuperclass();
			}
			
			if (this.patchedLayers.containsKey(layerClass)) {
				this.patchedLayers.get(layerClass).renderLayer(entity, entitypatch, layer, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
			}
		}
		
		for (PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, ? extends RenderLayer<LivingEntity, EntityModel<LivingEntity>>> patchedLayer : this.customLayers) {
			patchedLayer.renderLayer(entity, entitypatch, null, poseStack, buffer, packedLight, poses, bob, f2, f7, partialTicks);
		}
	}
	
	protected int getOverlayCoord(LivingEntity entity, LivingEntityPatch<LivingEntity> entitypatch, float partialTicks) {
		return OverlayTexture.pack(0, OverlayTexture.v(entity.hurtTime > 5));
	}
	
	@Override
	public void addPatchedLayer(Class<?> originalLayerClass, PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, ? extends RenderLayer<LivingEntity, EntityModel<LivingEntity>>> patchedLayer) {
		this.patchedLayers.putIfAbsent(originalLayerClass, patchedLayer);
	}
	
	@Override
	public void addCustomLayer(PatchedLayer<LivingEntity, LivingEntityPatch<LivingEntity>, EntityModel<LivingEntity>, ? extends RenderLayer<LivingEntity, EntityModel<LivingEntity>>> patchedLayer) {
		this.customLayers.add(patchedLayer);
	}
	
	@Override
	public MeshProvider<AnimatedMesh> getDefaultMesh() {
		return this.mesh;
	}
}
