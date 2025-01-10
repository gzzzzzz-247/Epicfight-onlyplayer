package yesman.epicfight.client.gui.datapack.widgets;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.property.JointMask.JointMaskSet;
import yesman.epicfight.api.client.animation.property.JointMaskReloadListener;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.datapack.screen.DatapackEditScreen;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.client.gui.datapack.screen.SelectAnimationScreen;
import yesman.epicfight.client.gui.datapack.screen.SelectFromRegistryScreen;
import yesman.epicfight.client.gui.datapack.screen.SelectModelScreen;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;

@OnlyIn(Dist.CLIENT)
public abstract class PopupBox<T> extends AbstractWidget implements DataBindingComponent<T, Pair<String, T>> {
	public static final ResourceLocation POPUP_ICON = new ResourceLocation(EpicFightMod.MODID, "textures/gui/popup_icon.png");
	
	protected final Screen owner;
	protected final Font font;
	protected final Function<T, String> toDisplayString;
	
	protected T item;
	protected String itemDisplayName;
	protected Predicate<T> filter;
	protected Consumer<Pair<String, T>> responder;
	
	public PopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Function<T, String> displayStringMapper, Consumer<Pair<String, T>> responder) {
		super(x1, y1, x2, y2, title);
		
		this.owner = owner;
		this.font = font;
		this.responder = responder;
		this.toDisplayString = displayStringMapper;
		
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.horizontalSizingOption = horizontal;
		this.verticalSizingOption = vertical;
	}
	
	public Predicate<T> getFilter() {
		return this.filter == null ? (item) -> true : this.filter;
	}
	
	public PopupBox<T> applyFilter(Predicate<T> filter) {
		this.filter = filter;
		return this;
	}
	
	@Override
	protected boolean clicked(double x, double y) {
		return this.active && this.visible && x >= (double)this._getX() && y >= (double) this._getY() && x < (double) (this._getX() + this.width) && y < (double) (this._getY() + this.height);
	}
	
	protected boolean clickedPopupButton(double x, double y) {
		return this.active && this.visible && x >= (double)this._getX() + this.width - 14 && y >= (double) this._getY() && x < (double) (this._getX() + this.width) && y < (double) (this._getY() + this.height);
	}
	
	@Override
	public abstract void onClick(double x, double y);
	
	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (this.active && this.visible) {
			if (this.isValidClickButton(button)) {
				if (this.clicked(x, y)) {
					this.playDownSound(Minecraft.getInstance().getSoundManager());
					this.onClick(x, y);
					return true;
				}
			}
			
			return false;
		} else {
			return false;
		}
	}
	
	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		int outlineColor = this.isFocused() ? -1 : this.isActive() ? -6250336 : -12566463;
		
		guiGraphics.fill(this._getX() - 1, this._getY() - 1, this._getX() + this.width + 1, this._getY() + this.height + 1, outlineColor);
		guiGraphics.fill(this._getX(), this._getY(), this._getX() + this.width, this._getY() + this.height, -16777216);
		
		String correctedString = StringUtil.isNullOrEmpty(this.itemDisplayName) ? "" : this.font.plainSubstrByWidth(this.itemDisplayName, this.width - 16);
		guiGraphics.drawString(this.font, correctedString, this._getX() + 4, this._getY() + this.height / 2 - this.font.lineHeight / 2 + 1, 16777215, false);
		
		RenderSystem.enableBlend();
		
		if (!this.isActive()) {
			RenderSystem.setShaderColor(0.3F, 0.3F, 0.3F, 1.0F);
		}
		
		this.renderTexture(guiGraphics, POPUP_ICON, this._getX() + this.width - this.height, this._getY(), 0, 0, 0, this.height, this.height, this.height, this.height);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}
	
	@Override
	protected MutableComponent createNarrationMessage() {
		Component component = this._getMessage();
		return Component.translatable("gui.epicfight.narrate.popbupBox", component);
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementInput) {
		narrationElementInput.add(NarratedElementType.TITLE, this.createNarrationMessage());
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class RegistryPopupBox<T> extends PopupBox<T> {
		protected final IForgeRegistry<T> registry;
		protected final Consumer<T> onPressRow;
		
		public RegistryPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, IForgeRegistry<T> registry, Consumer<Pair<String, T>> responder) {
			this(owner, font, x1, x2, y1, y2, horizontal, vertical, title, registry, (item) -> {}, responder);
		}
		
		public RegistryPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, IForgeRegistry<T> registry, Consumer<T> onPressRow, Consumer<Pair<String, T>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (item) -> registry.containsValue(item) ? registry.getKey(item).toString() : ParseUtil.nullParam(item), responder);
			
			this.registry = registry;
			this.onPressRow = onPressRow;
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, this.registry, (name, item) -> this._setValue(item), (name, item) -> this._setValue(item), this.onPressRow, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class SoundPopupBox extends RegistryPopupBox<SoundEvent> {
		public SoundPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, SoundEvent>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, ForgeRegistries.SOUND_EVENTS, (soundevent) -> {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundevent, 1.0F));
			}, responder);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class AnimationPopupBox extends PopupBox<StaticAnimation> {
		private Supplier<Armature> armature;
		private MeshProvider<AnimatedMesh> mesh;
		
		public AnimationPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, StaticAnimation>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (animation) -> ParseUtil.nullOrToString(animation, (a) -> a.getRegistryName().toString()), responder);
		}
		
		public void setModel(Supplier<Armature> armature, MeshProvider<AnimatedMesh> mesh) {
			this.armature = armature;
			this.mesh = mesh;
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				if (this.armature.get() == null || this.mesh.get() == null) {
					this.owner.getMinecraft().setScreen(new MessageScreen<>("", "Define model and armature first.", this.owner, (button2) -> this.owner.getMinecraft().setScreen(this.owner), 180, 60));
				} else {
					this.owner.getMinecraft().setScreen(new SelectAnimationScreen(this.owner, this::_setValue, this::_setValue, this.getFilter(), this.armature.get(), this.mesh));
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ColliderPopupBox extends PopupBox<Collider> {
		public ColliderPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, Collider>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (collider) -> ParseUtil.nullOrToString(collider, (c) -> ParseUtil.nullParam(ColliderPreset.getKey(c))), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, ParseUtil.mapEntryToPair(ColliderPreset.entries()), "Collider", (name, item) -> this._setValue(item), (name, item) -> this._setValue(item), (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class WeaponTypePopupBox extends PopupBox<Function<Item, CapabilityItem.Builder>> {
		public WeaponTypePopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, Function<Item, CapabilityItem.Builder>>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (builder) -> {
				Map<Function<Item, CapabilityItem.Builder>, ResourceLocation> map = Maps.newHashMap();
				
				WeaponTypeReloadListener.entries().forEach((entry) -> map.put(entry.getValue(), entry.getKey()));
				DatapackEditScreen.getSerializableWeaponTypes().forEach((entry) -> map.put(entry.getValue(), entry.getKey()));
				
				return ParseUtil.nullParam(map.get(builder));
			}, responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				Set<Map.Entry<ResourceLocation, Function<Item, CapabilityItem.Builder>>> weaponTypeEntry = Sets.newHashSet();
				weaponTypeEntry.addAll(WeaponTypeReloadListener.entries());
				weaponTypeEntry.addAll(DatapackEditScreen.getSerializableWeaponTypes());
				
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, ParseUtil.mapEntryToPair(weaponTypeEntry), "Weapon Type", (name, item) -> {
					var responder = this.responder;
					
					this._setResponder(null);
					this._setValue(item);
					responder.accept(Pair.of(name, item));
					this._setResponder(responder);
					
					this.setDisplayText(name);
				}, (name, item) -> {
					var responder = this.responder;
					
					this._setResponder(null);
					this._setValue(item);
					responder.accept(Pair.of(name, item));
					this._setResponder(responder);
					
					this.setDisplayText(name);
				}, (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class JointMaskPopupBox extends PopupBox<JointMaskSet> {
		public JointMaskPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, JointMaskSet>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (jointMask) -> ParseUtil.nullParam(JointMaskReloadListener.getKey(jointMask)), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, ParseUtil.mapEntryToPair(JointMaskReloadListener.entries()), "Joint Mask", (name, item) -> this._setValue(item), (name, item) -> this._setValue(item), (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class BuiltinMobpatchPopupBox extends PopupBox<EntityType<?>> {
		public BuiltinMobpatchPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, EntityType<?>>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (entityType) -> ParseUtil.nullParam(EntityType.getKey(entityType)), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				Set<Pair<ResourceLocation, EntityType<?>>> set = Sets.newHashSet();
				EntityPatchProvider.getPatchedEntities().forEach((entityType) -> set.add(Pair.of(EntityType.getKey(entityType), entityType)));
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, set, "Preset", (name, item) -> this._setValue(item), (name, item) -> this._setValue(item), (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class MeshPopupBox extends PopupBox<MeshProvider<AnimatedMesh>> {
		public MeshPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, MeshProvider<AnimatedMesh>>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (mesh) -> ParseUtil.nullOrToString(mesh, (mp) -> ParseUtil.nullParam(Meshes.getKey(mp.get()))), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				this.owner.getMinecraft().setScreen(new SelectModelScreen(this.owner, (name, item) -> {
					this._setValue(item);
					this.setDisplayText(name);
				}, (name, item) -> {
					this._setValue(item);
					this.setDisplayText(name);
				}));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ArmaturePopupBox extends PopupBox<Armature> {
		public ArmaturePopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, Armature>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (armature) -> ParseUtil.nullParam(Armatures.getKey(armature)), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, ParseUtil.mapEntryToPair(Armatures.entries()), "Armature", (name, item) -> {
					this._setValue(item);
					this.setDisplayText(name);
				}, (name, item) -> {
					this._setValue(item);
					this.setDisplayText(name);
				}, (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class RendererPopupBox extends PopupBox<ResourceLocation> {
		public RendererPopupBox(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, ResourceLocation>> responder) {
			super(owner, font, x1, x2, y1, y2, horizontal, vertical, title, (entityType) -> ParseUtil.nullOrToString(entityType, (rl) -> rl.toString()), responder);
		}
		
		@Override
		public void onClick(double x, double y) {
			if (this.clickedPopupButton(x, y)) {
				Set<Pair<ResourceLocation, ResourceLocation>> set = Sets.newHashSet();
				ClientEngine.getInstance().renderEngine.getRendererEntries().forEach((rl) -> set.add(Pair.of(rl, rl)));
				
				this.owner.getMinecraft().setScreen(new SelectFromRegistryScreen<>(this.owner, set, "Renderer", (name, item) -> this._setValue(item), (name, item) -> this._setValue(item), (c) -> {}, this.getFilter()));
			}
		}
	}
	
	@FunctionalInterface
	public static interface PopupBoxProvider<T, P extends PopupBox<T>> {
		public P create(Screen owner, Font font, int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Component title, Consumer<Pair<String, T>> responder);
	}
	
	/*******************************************************************
	 * @DataBindingComponent variables                                 *
	 *******************************************************************/
	private int x1;
	private int x2;
	private int y1;
	private int y2;
	private final HorizontalSizing horizontalSizingOption;
	private final VerticalSizing verticalSizingOption;
	
	@Override
	public void setX1(int x1) {
		this.x1 = x1;
	}

	@Override
	public void setX2(int x2) {
		this.x2 = x2;
	}

	@Override
	public void setY1(int y1) {
		this.y1 = y1;
	}

	@Override
	public void setY2(int y2) {
		this.y2 = y2;
	}
	
	@Override
	public int getX1() {
		return this.x1;
	}

	@Override
	public int getX2() {
		return this.x2;
	}

	@Override
	public int getY1() {
		return this.y1;
	}

	@Override
	public int getY2() {
		return this.y2;
	}

	@Override
	public HorizontalSizing getHorizontalSizingOption() {
		return this.horizontalSizingOption;
	}

	@Override
	public VerticalSizing getVerticalSizingOption() {
		return this.verticalSizingOption;
	}
	
	@Override
	public void _setActive(boolean active) {
		this.active = active;
	}
	
	@Override
	public void _setResponder(Consumer<Pair<String, T>> responder) {
		this.responder = responder;
	}
	
	@Override
	public Consumer<Pair<String, T>> _getResponder() {
		return this.responder;
	}
	
	@Override
	public void _setValue(@Nullable T item) {
		this.item = item;
		this.itemDisplayName = this.toDisplayString.apply(item);
		
		if (this.responder != null) {
			this.responder.accept(Pair.of(this.itemDisplayName, item));
		}
		
		if (!StringUtil.isNullOrEmpty(this.itemDisplayName) && !this.itemDisplayName.equals(this.font.plainSubstrByWidth(this.itemDisplayName, this.width - 16))) {
			this.setTooltip(Tooltip.create(Component.literal(this.itemDisplayName)));
		} else {
			this.setTooltip(null);
		}
	}
	
	public void setDisplayText(String displayName) {
		this.itemDisplayName = displayName;
	}
	
	@Override
	public T _getValue() {
		return this.item;
	}
	
	@Override
	public void reset() {
		this.item = null;
		this.itemDisplayName = "";
	}
	
	@Override
	public void _tick() {
	}

	@Override
	public int _getX() {
		return this.getX();
	}

	@Override
	public int _getY() {
		return this.getY();
	}

	@Override
	public int _getWidth() {
		return this.getWidth();
	}

	@Override
	public int _getHeight() {
		return this.getHeight();
	}

	@Override
	public void _setX(int x) {
		this.setX(x);
	}

	@Override
	public void _setY(int y) {
		this.setY(y);
	}

	@Override
	public void _setWidth(int width) {
		this.setWidth(width);
	}

	@Override
	public void _setHeight(int height) {
		this.setHeight(height);
	}

	@Override
	public Component _getMessage() {
		return this.getMessage();
	}

	@Override
	public void _renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
	}
}