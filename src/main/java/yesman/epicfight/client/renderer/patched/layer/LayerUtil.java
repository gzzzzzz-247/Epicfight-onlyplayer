package yesman.epicfight.client.renderer.patched.layer;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import yesman.epicfight.api.client.forgeevent.RegisterResourceLayersEvent;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.LayerRenderer;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.data.conditions.EpicFightConditions;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class LayerUtil {
	@FunctionalInterface
	public interface LayerProvider<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> {
		PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> getLayer(JsonObject properties);
	}
	
	public static <E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> void addLayer(LayerRenderer<E, T, M> renderer, EntityType<?> entityType, List<Pair<ResourceLocation, JsonElement>> layers) {
		Map<ResourceLocation, LayerProvider<E, T, M, R, AM>> layersbyid = Maps.newHashMap();
		
		layersbyid.put(new ResourceLocation(EpicFightMod.MODID, "invisible"), LayerUtil::getInvisibleLayer);
		layersbyid.put(new ResourceLocation(EpicFightMod.MODID, "eyes"), LayerUtil::getEyesLayer);
		layersbyid.put(new ResourceLocation(EpicFightMod.MODID, "model_original"), LayerUtil::getOriginalModelLayer);
		
		MinecraftForge.EVENT_BUS.post(new RegisterResourceLayersEvent<> (layersbyid));
		
		for (Pair<ResourceLocation, JsonElement> entry : layers) {
			try {
				JsonObject jsonobj = entry.getSecond().getAsJsonObject();
				
				if (!jsonobj.has("layer_type")) {
					throw new NoSuchElementException("Layer type undefined");
				}
				
				if (!jsonobj.has("target_layer")) {
					throw new NoSuchElementException("Target layer undefined");
				}
				
				String layerType = jsonobj.get("layer_type").getAsString();
				String targetLayer = jsonobj.get("target_layer").getAsString();
				
				Class<?> clss;
				
				if ("none".equals(targetLayer)) {
					clss = null;
				} else {
					clss = Class.forName(targetLayer);
				}
				
				ResourceLocation rl = new ResourceLocation(layerType);
				
				if (!layersbyid.containsKey(rl)) {
					throw new NoSuchElementException("No layer type " + layerType);
				}
				
				LayerProvider<E, T, M, R, AM> layerAdder = layersbyid.get(rl);
				PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> patchedLayer = layerAdder.getLayer(jsonobj);
				
				if (jsonobj.has("conditions")) {
					JsonArray conditionsArray = jsonobj.getAsJsonArray("conditions");
					EntityPatchCondition[] conditions = new EntityPatchCondition[conditionsArray.size()];
					int idx = 0;
					
					for (JsonElement conditionElement : conditionsArray) {
						JsonObject conditionObj = conditionElement.getAsJsonObject();
						Supplier<EntityPatchCondition> conditionProvider = EpicFightConditions.getConditionOrThrow(new ResourceLocation(GsonHelper.getAsString(conditionObj, "predicate")));
						EntityPatchCondition condition = conditionProvider.get();
						condition.read(conditionObj);
						conditions[idx] = condition;
						idx++;
					}
					
					patchedLayer = new WrappedConditionalLayer<> (patchedLayer, (entitypatch) -> {
						for (EntityPatchCondition condition : conditions) {
							if (!condition.predicate(entitypatch)) {
								return false;
							}
						}
						
						return true;
					});
				}
				
				if (clss != null) {
					renderer.addPatchedLayer(clss, patchedLayer);
				} else {
					renderer.addCustomLayer(patchedLayer);
				}
			} catch (NoSuchElementException | ClassNotFoundException | ClassCastException | CommandSyntaxException | IllegalArgumentException e) {
				EpicFightMod.LOGGER.error("Couldn't read layer file {} for {}: {}", entry.getFirst(), entityType, e.getMessage());
			}
		}
	}
	
	private static <E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> getInvisibleLayer(JsonObject properties) {
		if ("none".equals(properties.get("target_layer").getAsString())) {
			throw new IllegalArgumentException("Empty layer must define a target layer");
		}
		
		return new EmptyLayer<E, T, M> ();
	}
	
	private static <E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> getEyesLayer(JsonObject properties) {
		if (!properties.has("texture")) {
			throw new NoSuchElementException("Layer type epicfight:eyes requires to specify texture");
		}
		
		if (!properties.has("model")) {
			throw new NoSuchElementException("Layer type epicfight:eyes requires to specify model");
		}
		
		ResourceLocation textureLocation = new ResourceLocation(properties.get("texture").getAsString());
		AnimatedMesh mesh = Meshes.getOrCreateAnimatedMesh(Minecraft.getInstance().getResourceManager(), new ResourceLocation(properties.get("model").getAsString()), AnimatedMesh::new);
		
		return new PatchedEyesLayer<> (textureLocation, () -> mesh);
	}
	
	private static <E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>, R extends LivingEntityRenderer<E, M>, AM extends AnimatedMesh> PatchedLayer<E, T, M, ? extends RenderLayer<E, M>> getOriginalModelLayer(JsonObject properties) {
		if ("none".equals(properties.get("target_layer").getAsString())) {
			throw new IllegalArgumentException("Original model layer must define a target layer");
		}
		
		if (!properties.has("joint")) {
			throw new NoSuchElementException("Layer type epicfight:model_original requires to specify joint");
		}
		
		Vec3f vec = new Vec3f();
		Vec3f rot = new Vec3f();
		
		if (properties.has("translation")) {
			JsonArray translationVector = GsonHelper.getAsJsonArray(properties, "translation");
			vec.x = translationVector.get(0).getAsFloat();
			vec.y = translationVector.get(1).getAsFloat();
			vec.z = translationVector.get(2).getAsFloat();
		}
		
		if (properties.has("rotation")) {
			JsonArray rotation = GsonHelper.getAsJsonArray(properties, "rotation");
			rot.x = rotation.get(0).getAsFloat();
			rot.y = rotation.get(1).getAsFloat();
			rot.z = rotation.get(2).getAsFloat();
		}
		
		return new RenderOriginalModelLayer<> (properties.get("joint").getAsString(), vec, rot);
	}
}