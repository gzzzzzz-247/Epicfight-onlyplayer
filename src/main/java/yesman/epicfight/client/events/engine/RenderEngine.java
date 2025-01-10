package yesman.epicfight.client.events.engine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.ItemSkin;
import yesman.epicfight.api.client.model.ItemSkins;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.EntityIndicator;
import yesman.epicfight.client.gui.VersionNotifier;
import yesman.epicfight.client.gui.screen.config.UISetupScreen;
import yesman.epicfight.client.gui.screen.overlay.OverlayManager;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.AimHelperRenderer;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCustomEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PresetRenderer;
import yesman.epicfight.client.renderer.patched.item.RenderBow;
import yesman.epicfight.client.renderer.patched.item.RenderCrossbow;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.renderer.patched.item.RenderKatana;
import yesman.epicfight.client.renderer.patched.item.RenderMap;
import yesman.epicfight.client.renderer.patched.item.RenderShield;
import yesman.epicfight.client.renderer.patched.item.RenderTrident;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.BowCapability;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CrossbowCapability;
import yesman.epicfight.world.capabilities.item.MapCapability;
import yesman.epicfight.world.capabilities.item.ShieldCapability;
import yesman.epicfight.world.capabilities.item.TridentCapability;
import yesman.epicfight.world.entity.EpicFightEntities;
import yesman.epicfight.world.gamerule.EpicFightGamerules;
import yesman.epicfight.world.item.EpicFightItems;

@SuppressWarnings("rawtypes")
@OnlyIn(Dist.CLIENT)
public class RenderEngine {
	private static final Vec3f AIMING_CORRECTION = new Vec3f(-1.5F, 0.0F, 1.25F);

	public final BattleModeGui battleModeUI = new BattleModeGui(Minecraft.getInstance());
	public final VersionNotifier versionNotifier = new VersionNotifier(Minecraft.getInstance());
	public final Minecraft minecraft;

	private final BiMap<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> entityRendererProvider;
	private final Map<EntityType<?>, PatchedEntityRenderer> entityRendererCache;
	private final Map<Item, RenderItemBase> itemRendererMapByInstance;
	private final Map<Class<?>, RenderItemBase> itemRendererMapByClass;
	private final Set<Component> sentMessages;
	private final OverlayManager overlayManager;

	private AimHelperRenderer aimHelper;
	private FirstPersonRenderer firstPersonRenderer;
	private PHumanoidRenderer<?, ?, ?, ?, ?> basicHumanoidRenderer;
	private boolean zoomingIn;
	private int modelInitTimer;

	private final int maxZoomCount = 20;
	private int zoomOutStandbyTicks = 0;
	private int zoomCount = 0;

	public RenderEngine() {
		Events.renderEngine = this;
		RenderItemBase.renderEngine = this;
		EntityIndicator.init();

		this.minecraft = Minecraft.getInstance();
		this.entityRendererProvider = HashBiMap.create();
		this.entityRendererCache = Maps.newHashMap();
		this.itemRendererMapByInstance = Maps.newHashMap();
		this.itemRendererMapByClass = Maps.newHashMap();
		this.sentMessages = Sets.newHashSet();
		this.overlayManager = new OverlayManager();
	}

	public void bootstrap(EntityRendererProvider.Context context) {
		this.entityRendererProvider.clear();
		this.entityRendererProvider.put(EntityType.PLAYER,
				(entityType) -> new PPlayerRenderer(context, entityType).initLayerLast(context, entityType));

		this.firstPersonRenderer = new FirstPersonRenderer(context, EntityType.PLAYER);
		this.basicHumanoidRenderer = new PHumanoidRenderer<>(() -> Meshes.BIPED, context, EntityType.PLAYER);
		this.aimHelper = new AimHelperRenderer();

		RenderItemBase baseRenderer = new RenderItemBase();
		RenderBow bowRenderer = new RenderBow();
		RenderCrossbow crossbowRenderer = new RenderCrossbow();
		RenderTrident tridentRenderer = new RenderTrident();
		RenderMap mapRenderer = new RenderMap();
		RenderShield shieldRenderer = new RenderShield();

		// Clear item renderers
		this.itemRendererMapByInstance.clear();
		this.itemRendererMapByClass.clear();

		this.itemRendererMapByInstance.put(Items.AIR, baseRenderer);
		this.itemRendererMapByInstance.put(Items.BOW, bowRenderer);
		this.itemRendererMapByInstance.put(Items.SHIELD, shieldRenderer);
		this.itemRendererMapByInstance.put(Items.CROSSBOW, crossbowRenderer);
		this.itemRendererMapByInstance.put(Items.TRIDENT, tridentRenderer);
		this.itemRendererMapByInstance.put(Items.FILLED_MAP, mapRenderer);
		this.itemRendererMapByInstance.put(EpicFightItems.UCHIGATANA.get(), new RenderKatana());

		// Render by item class
		this.itemRendererMapByClass.put(BowItem.class, bowRenderer);
		this.itemRendererMapByClass.put(CrossbowItem.class, crossbowRenderer);
		this.itemRendererMapByClass.put(ShieldItem.class, baseRenderer);
		this.itemRendererMapByClass.put(TridentItem.class, tridentRenderer);
		this.itemRendererMapByClass.put(ShieldItem.class, shieldRenderer);
		// Render by capability class
		this.itemRendererMapByClass.put(BowCapability.class, bowRenderer);
		this.itemRendererMapByClass.put(CrossbowCapability.class, crossbowRenderer);
		this.itemRendererMapByClass.put(TridentCapability.class, tridentRenderer);
		this.itemRendererMapByClass.put(MapCapability.class, mapRenderer);
		this.itemRendererMapByClass.put(ShieldCapability.class, shieldRenderer);

		ModLoader.get().postEvent(
				new PatchedRenderersEvent.Add(this.entityRendererProvider, this.itemRendererMapByInstance, context));

		this.resetRenderers();
	}

	public void resetRenderers() {
		this.sentMessages.clear();
		this.entityRendererCache.clear();

		for (Map.Entry<EntityType<?>, Function<EntityType<?>, PatchedEntityRenderer>> entry : this.entityRendererProvider
				.entrySet()) {
			this.entityRendererCache.put(entry.getKey(), entry.getValue().apply(entry.getKey()));
		}

		ModLoader.get().postEvent(new PatchedRenderersEvent.Modify(this.entityRendererCache));
	}

	@SuppressWarnings("unchecked")
	public void registerCustomEntityRenderer(EntityType<?> entityType, String rendererName, CompoundTag compound) {
		if (StringUtil.isNullOrEmpty(rendererName)) {
			return;
		}

		EntityRenderDispatcher erd = this.minecraft.getEntityRenderDispatcher();
		EntityRendererProvider.Context context = new EntityRendererProvider.Context(erd,
				this.minecraft.getItemRenderer(), this.minecraft.getBlockRenderer(), erd.getItemInHandRenderer(),
				this.minecraft.getResourceManager(), this.minecraft.getEntityModels(), this.minecraft.font);

		if ("player".equals(rendererName)) {
			this.entityRendererCache.put(entityType, this.basicHumanoidRenderer);
		} else if ("epicfight:custom".equals(rendererName)) {
			if (compound.getBoolean("humanoid")) {
				this.entityRendererCache.put(entityType,
						new PCustomHumanoidEntityRenderer<>(
								() -> Meshes.getOrCreateAnimatedMesh(this.minecraft.getResourceManager(),
										new ResourceLocation(compound.getString("model")), HumanoidMesh::new),
								context, entityType));
			} else {
				this.entityRendererCache.put(entityType,
						new PCustomEntityRenderer(
								() -> Meshes.getOrCreateAnimatedMesh(this.minecraft.getResourceManager(),
										new ResourceLocation(compound.getString("model")), AnimatedMesh::new),
								context));
			}
		} else {
			EntityType<?> presetEntityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(rendererName));

			if (this.entityRendererProvider.containsKey(presetEntityType)) {
				PatchedEntityRenderer renderer = this.entityRendererProvider.get(presetEntityType).apply(entityType);

				if (!(this.minecraft.getEntityRenderDispatcher().renderers
						.get(entityType) instanceof LivingEntityRenderer)
						&& (renderer instanceof PatchedLivingEntityRenderer patchedLivingEntityRenderer)) {
					this.entityRendererCache.put(entityType,
							new PresetRenderer(context, entityType,
									(LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) context
											.getEntityRenderDispatcher().renderers.get(presetEntityType),
									patchedLivingEntityRenderer.getDefaultMesh()));
				} else {
					this.entityRendererCache.put(entityType,
							this.entityRendererProvider.get(presetEntityType).apply(entityType));
				}
			} else {
				throw new IllegalArgumentException("Datapack Mob Patch Crash: Invalid Renderer type " + rendererName);
			}
		}
	}

	public RenderItemBase getItemRenderer(ItemStack itemstack) {
		RenderItemBase renderItem = this.itemRendererMapByInstance.get(itemstack.getItem());

		if (renderItem == null) {
			renderItem = this.findMatchingRendererByClass(itemstack.getItem().getClass());

			if (renderItem == null) {
				CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(itemstack);
				renderItem = this.findMatchingRendererByClass(itemCap.getClass());
			}

			if (renderItem == null) {
				renderItem = this.itemRendererMapByInstance.get(Items.AIR);
			}

			this.itemRendererMapByInstance.put(itemstack.getItem(), renderItem);
		}

		return renderItem;
	}

	private RenderItemBase findMatchingRendererByClass(Class<?> clazz) {
		RenderItemBase renderer = null;

		for (; clazz != null && renderer == null; clazz = clazz.getSuperclass()) {
			renderer = this.itemRendererMapByClass.get(clazz);
		}

		return renderer;
	}

	@SuppressWarnings("unchecked")
	public void renderEntityArmatureModel(LivingEntity livingEntity, LivingEntityPatch<?> entitypatch,
			EntityRenderer<? extends Entity> renderer, MultiBufferSource buffer, PoseStack matStack, int packedLight,
			float partialTicks) {
		this.getEntityRenderer(livingEntity).render(livingEntity, entitypatch, renderer, buffer, matStack, packedLight,
				partialTicks);
	}

	public PatchedEntityRenderer getEntityRenderer(Entity entity) {
		return this.getEntityRenderer(entity.getType());
	}

	public PatchedEntityRenderer getEntityRenderer(EntityType entityType) {
		return this.entityRendererCache.get(entityType);
	}

	public boolean hasRendererFor(Entity entity) {
		return this.entityRendererCache.computeIfAbsent(entity.getType(),
				(key) -> this.entityRendererProvider.containsKey(key)
						? this.entityRendererProvider.get(entity.getType()).apply(entity.getType())
						: null) != null;
	}

	public Set<ResourceLocation> getRendererEntries() {
		Set<ResourceLocation> availableRendererEntities = this.entityRendererProvider.keySet().stream()
				.map((entityType) -> EntityType.getKey(entityType)).collect(Collectors.toSet());
		availableRendererEntities.add(new ResourceLocation(EpicFightMod.MODID, "custom"));

		return availableRendererEntities;
	}

	// Nothing happens if player is already zooming-in
	public void zoomIn() {
		if (!this.zoomingIn) {
			this.zoomingIn = true;
			this.zoomCount = this.zoomCount == 0 ? 1 : this.zoomCount;
		}
	}

	// Nothing happens if player is already zooming-out
	public void zoomOut(int zoomOutTicks) {
		if (this.zoomingIn) {
			this.zoomingIn = false;
			this.zoomOutStandbyTicks = zoomOutTicks;
		}
	}

	public void setModelInitializerTimer(int tick) {
		this.modelInitTimer = tick;
	}

	public void addMessage(Component message) {
		Minecraft.getInstance().gui.getChat().addMessage(message);
	}

	public void addMessageIfAbsent(Component message) {
		if (!this.sentMessages.contains(message)) {
			this.sentMessages.add(message);
			this.addMessage(message);
		}
	}

	private void setRangedWeaponThirdPerson(ViewportEvent.ComputeCameraAngles event, CameraType pov,
			double partialTicks) {
		if (ClientEngine.getInstance().getPlayerPatch() == null) {
			return;
		}

		Camera camera = event.getCamera();
		Entity entity = minecraft.getCameraEntity();
		Vec3 vector = camera.getPosition();
		double totalX = vector.x();
		double totalY = vector.y();
		double totalZ = vector.z();

		if (pov == CameraType.THIRD_PERSON_BACK) {
			double posX = vector.x();
			double posY = vector.y();
			double posZ = vector.z();
			double entityPosX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
			double entityPosY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks + entity.getEyeHeight();
			double entityPosZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
			float intpol = pov == CameraType.THIRD_PERSON_BACK ? ((float) zoomCount / (float) maxZoomCount) : 0;
			Vec3f interpolatedCorrection = new Vec3f(AIMING_CORRECTION.x * intpol, AIMING_CORRECTION.y * intpol,
					AIMING_CORRECTION.z * intpol);
			OpenMatrix4f rotationMatrix = ClientEngine.getInstance().getPlayerPatch().getMatrix((float) partialTicks);
			Vec3f rotateVec = OpenMatrix4f.transform3v(rotationMatrix, interpolatedCorrection, null);
			double d3 = Math
					.sqrt((rotateVec.x * rotateVec.x) + (rotateVec.y * rotateVec.y) + (rotateVec.z * rotateVec.z));
			double smallest = d3;
			double d00 = posX + rotateVec.x;
			double d11 = posY - rotateVec.y;
			double d22 = posZ + rotateVec.z;

			for (int i = 0; i < 8; ++i) {
				float f = (float) ((i & 1) * 2 - 1);
				float f1 = (float) ((i >> 1 & 1) * 2 - 1);
				float f2 = (float) ((i >> 2 & 1) * 2 - 1);
				f = f * 0.1F;
				f1 = f1 * 0.1F;
				f2 = f2 * 0.1F;
				HitResult raytraceresult = minecraft.level
						.clip(new ClipContext(new Vec3(entityPosX + f, entityPosY + f1, entityPosZ + f2),
								new Vec3(d00 + f + f2, d11 + f1, d22 + f2), ClipContext.Block.COLLIDER,
								ClipContext.Fluid.NONE, entity));

				if (raytraceresult != null) {
					double d7 = raytraceresult.getLocation().distanceTo(new Vec3(entityPosX, entityPosY, entityPosZ));
					if (d7 < smallest) {
						smallest = d7;
					}
				}
			}

			float dist = d3 == 0 ? 0 : (float) (smallest / d3);
			totalX += rotateVec.x * dist;
			totalY -= rotateVec.y * dist;
			totalZ += rotateVec.z * dist;
		}

		camera.setPosition(totalX, totalY, totalZ);
	}

	public void correctCamera(ViewportEvent.ComputeCameraAngles event, float partialTicks) {
		LocalPlayerPatch localPlayerPatch = ClientEngine.getInstance().getPlayerPatch();
		Camera camera = event.getCamera();
		CameraType cameraType = this.minecraft.options.getCameraType();

		if (localPlayerPatch != null) {
			if (localPlayerPatch.getTarget() != null && localPlayerPatch.isTargetLockedOn()) {
				float xRot = localPlayerPatch.getLerpedLockOnX(event.getPartialTick());
				float yRot = localPlayerPatch.getLerpedLockOnY(event.getPartialTick());

				if (cameraType.isMirrored()) {
					yRot += 180.0F;
					xRot *= -1.0F;
				}

				camera.setRotation(yRot, xRot);
				event.setPitch(xRot);
				event.setYaw(yRot);

				if (!cameraType.isFirstPerson()) {
					Entity cameraEntity = this.minecraft.cameraEntity;

					camera.setPosition(Mth.lerp(partialTicks, cameraEntity.xo, cameraEntity.getX()),
							Mth.lerp(partialTicks, cameraEntity.yo, cameraEntity.getY())
									+ Mth.lerp(partialTicks, camera.eyeHeightOld, camera.eyeHeight),
							Mth.lerp(partialTicks, cameraEntity.zo, cameraEntity.getZ()));

					camera.move(-camera.getMaxZoom(4.0D), 0.0D, 0.0D);
				}
			}
		}
	}

	public OverlayManager getOverlayManager() {
		return this.overlayManager;
	}

	public FirstPersonRenderer getFirstPersonRenderer() {
		return firstPersonRenderer;
	}

	public void upSlideSkillUI() {
		this.battleModeUI.slideUp();
	}

	public void downSlideSkillUI() {
		this.battleModeUI.slideDown();
	}

	public boolean shouldRenderVanillaModel() {
		return ClientEngine.getInstance().isVanillaModelDebuggingMode() || this.modelInitTimer > 0;
	}

	@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
	public static class Events {
		static RenderEngine renderEngine;

		@SubscribeEvent
		public static void renderLivingEvent(
				RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
			LivingEntity livingentity = event.getEntity();

			if (livingentity.level() == null) {
				return;
			}

			if (renderEngine.hasRendererFor(livingentity)) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(livingentity,
						LivingEntityPatch.class);
				float originalYRot = 0.0F;

				// Draw the player in inventory
				if ((event.getPartialTick() == 0.0F || event.getPartialTick() == 1.0F)
						&& entitypatch instanceof LocalPlayerPatch localPlayerPatch) {
					if (entitypatch.overrideRender()) {
						originalYRot = localPlayerPatch.getModelYRot();
						localPlayerPatch.setModelYRotInGui(livingentity.getYRot());
						event.getPoseStack().translate(0, 0.1D, 0);

						boolean usingShader = EpicFightMod.CLIENT_CONFIGS.useAnimationShader.getValue();

						if (usingShader) {
							EpicFightMod.CLIENT_CONFIGS.useAnimationShader.setValue(false);
						}

						renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(),
								event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(),
								event.getPartialTick());

						if (usingShader) {
							EpicFightMod.CLIENT_CONFIGS.useAnimationShader.setValue(true);
						}

						localPlayerPatch.disableModelYRotInGui(originalYRot);
						event.setCanceled(true);
					}

					return;
				}

				if (entitypatch != null && entitypatch.overrideRender()) {
					renderEngine.renderEntityArmatureModel(livingentity, entitypatch, event.getRenderer(),
							event.getMultiBufferSource(), event.getPoseStack(), event.getPackedLight(),
							event.getPartialTick());

					if (renderEngine.shouldRenderVanillaModel()) {
						event.getPoseStack().translate(1.0F, 0.0F, 0.0F);
						--renderEngine.modelInitTimer;
					} else {
						event.setCanceled(true);
					}
				}
			}

			if (ClientEngine.getInstance().getPlayerPatch() != null && !renderEngine.minecraft.options.hideGui
					&& !livingentity.level().getGameRules().getBoolean(EpicFightGamerules.DISABLE_ENTITY_UI)) {
				LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(livingentity,
						LivingEntityPatch.class);

				for (EntityIndicator entityIndicator : EntityIndicator.ENTITY_INDICATOR_RENDERERS) {
					if (entityIndicator.shouldDraw(livingentity, entitypatch,
							ClientEngine.getInstance().getPlayerPatch())) {
						entityIndicator.drawIndicator(livingentity, entitypatch,
								ClientEngine.getInstance().getPlayerPatch(), event.getPoseStack(),
								event.getMultiBufferSource(), event.getPartialTick());
					}
				}
			}
		}

		@SubscribeEvent
		public static void itemTooltip(ItemTooltipEvent event) {
			if (event.getEntity() != null && event.getEntity().level().isClientSide) {
				if (EpicFightMod.CLIENT_CONFIGS.showEpicFightAttributes.getValue()) {
					CapabilityItem cap = EpicFightCapabilities.getItemStackCapabilityOr(event.getItemStack(), null);
					LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(),
							LocalPlayerPatch.class);

					if (cap != null && playerpatch != null) {
						if (ControllEngine.isKeyDown(EpicFightKeyMappings.WEAPON_INNATE_SKILL_TOOLTIP)) {
							Skill weaponInnateSkill = cap.getInnateSkill(playerpatch, event.getItemStack());

							if (weaponInnateSkill != null) {
								event.getToolTip().clear();
								List<Component> skilltooltip = weaponInnateSkill.getTooltipOnItem(event.getItemStack(),
										cap, playerpatch);

								for (Component s : skilltooltip) {
									event.getToolTip().add(s);
								}
							}
						} else {
							List<Component> tooltip = event.getToolTip();
							cap.modifyItemTooltip(event.getItemStack(), event.getToolTip(), playerpatch);

							for (int i = 0; i < tooltip.size(); i++) {
								Component textComp = tooltip.get(i);

								if (!textComp.getSiblings().isEmpty()) {
									Component sibling = textComp.getSiblings().get(0);

									if (sibling instanceof MutableComponent mutableComponent && mutableComponent
											.getContents() instanceof TranslatableContents translatableContent) {
										if (translatableContent.getArgs().length > 1 && translatableContent
												.getArgs()[1] instanceof MutableComponent mutableComponent$2) {
											if (mutableComponent$2
													.getContents() instanceof TranslatableContents translatableContent$2) {
												if (translatableContent$2.getKey()
														.equals(Attributes.ATTACK_SPEED.getDescriptionId())) {
													float weaponSpeed = (float) playerpatch.getWeaponAttribute(
															Attributes.ATTACK_SPEED, event.getItemStack());
													tooltip.remove(i);
													tooltip.add(i, Component
															.literal(String.format(" %.2f ",
																	playerpatch.getModifiedAttackSpeed(cap,
																			weaponSpeed)))
															.append(Component.translatable(
																	Attributes.ATTACK_SPEED.getDescriptionId())));

												} else if (translatableContent$2.getKey()
														.equals(Attributes.ATTACK_DAMAGE.getDescriptionId())) {
													float weaponDamage = (float) playerpatch.getWeaponAttribute(
															Attributes.ATTACK_DAMAGE, event.getItemStack());
													float damageBonus = EnchantmentHelper
															.getDamageBonus(event.getItemStack(), MobType.UNDEFINED);
													String damageFormat = ItemStack.ATTRIBUTE_MODIFIER_FORMAT
															.format(playerpatch.getModifiedBaseDamage(weaponDamage)
																	+ damageBonus);

													tooltip.remove(i);
													tooltip.add(i, Component
															.literal(String.format(" %s ", damageFormat))
															.append(Component.translatable(
																	Attributes.ATTACK_DAMAGE.getDescriptionId()))
															.withStyle(ChatFormatting.DARK_GREEN));
												}
											}
										}
									}
								}
							}

							Skill weaponInnateSkill = cap.getInnateSkill(playerpatch, event.getItemStack());

							if (weaponInnateSkill != null) {
								event.getToolTip().add(Component.translatable(
										"inventory.epicfight.guide_innate_tooltip",
										EpicFightKeyMappings.WEAPON_INNATE_SKILL_TOOLTIP.getKey().getDisplayName())
										.withStyle(ChatFormatting.DARK_GRAY));
							}
						}
					}
				}
			}
		}

		@SubscribeEvent
		public static void cameraSetupEvent(ViewportEvent.ComputeCameraAngles event) {
			if (renderEngine.zoomCount > 0 && EpicFightMod.CLIENT_CONFIGS.aimingCorrection.getValue()) {
				renderEngine.setRangedWeaponThirdPerson(event, renderEngine.minecraft.options.getCameraType(),
						event.getPartialTick());

				if (renderEngine.zoomOutStandbyTicks > 0) {
					renderEngine.zoomOutStandbyTicks--;
				} else {
					renderEngine.zoomCount = renderEngine.zoomingIn ? renderEngine.zoomCount + 1
							: renderEngine.zoomCount - 1;
				}

				renderEngine.zoomCount = Math.min(renderEngine.maxZoomCount, renderEngine.zoomCount);
			}

			renderEngine.correctCamera(event, (float) event.getPartialTick());
		}

		@SubscribeEvent
		public static void fogEvent(ViewportEvent.RenderFog event) {
		}

		@SubscribeEvent
		public static void renderGui(RenderGuiEvent.Pre event) {
			Window window = Minecraft.getInstance().getWindow();
			LocalPlayerPatch playerpatch = ClientEngine.getInstance().getPlayerPatch();

			if (playerpatch != null) {
				for (SkillContainer skillContainer : playerpatch.getSkillCapability().skillContainers) {
					if (skillContainer.getSkill() != null) {
						skillContainer.getSkill().onScreen(playerpatch, window.getGuiScaledWidth(),
								window.getGuiScaledHeight());
					}
				}

				renderEngine.overlayManager.renderTick(window.getGuiScaledWidth(), window.getGuiScaledHeight());

				if (Minecraft.renderNames() && !(Minecraft.getInstance().screen instanceof UISetupScreen)) {
					renderEngine.battleModeUI.renderGui(playerpatch, event.getGuiGraphics(), event.getPartialTick());
				}

				// Shows the epic fight version in beta
				// renderEngine.betaWarningMessage.render(event.getGuiGraphics(), true);
			}
		}

		@SuppressWarnings("unchecked")
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void renderHand(RenderHandEvent event) {
			LocalPlayerPatch playerpatch = ClientEngine.getInstance().getPlayerPatch();

			if (playerpatch != null) {
				boolean isBattleMode = playerpatch.isBattleMode();

				if ((isBattleMode || !EpicFightMod.CLIENT_CONFIGS.filterAnimation.getValue())
						&& EpicFightMod.CLIENT_CONFIGS.firstPersonModel.getValue()) {
					ItemSkin mainhandItemSkin = ItemSkins
							.getItemSkin(playerpatch.getOriginal().getMainHandItem().getItem());
					ItemSkin offhandItemSkin = ItemSkins
							.getItemSkin(playerpatch.getOriginal().getOffhandItem().getItem());
					boolean useEpicFightModel = (mainhandItemSkin == null
							|| !mainhandItemSkin.forceVanillaFirstPerson())
							&& (offhandItemSkin == null || !offhandItemSkin.forceVanillaFirstPerson());

					if (useEpicFightModel) {
						if (event.getHand() == InteractionHand.MAIN_HAND) {
							renderEngine.firstPersonRenderer.render(playerpatch.getOriginal(), playerpatch,
									(LivingEntityRenderer) renderEngine.minecraft.getEntityRenderDispatcher()
											.getRenderer(playerpatch.getOriginal()),
									event.getMultiBufferSource(),
									event.getPoseStack(), event.getPackedLight(), event.getPartialTick());
						}

						event.setCanceled(true);
					}
				}
			}
		}

		@SubscribeEvent
		public static void renderWorldLast(RenderLevelStageEvent event) {
			if (EpicFightMod.CLIENT_CONFIGS.aimingCorrection.getValue() && renderEngine.zoomCount > 0
					&& renderEngine.minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK
					&& event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
				renderEngine.aimHelper.doRender(event.getPoseStack(), event.getPartialTick());
			}
		}
	}
}
