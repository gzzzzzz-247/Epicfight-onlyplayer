package yesman.epicfight.client.gui.screen.config;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.capabilities.provider.ItemCapabilityProvider;

@OnlyIn(Dist.CLIENT)
public class EditSwitchingItemScreen extends Screen {
	private static final ItemStack TITLE_STACK = new ItemStack((Void)null);
	
	protected final Screen parentScreen;
	private EditSwitchingItemScreen.RegisteredItemList battleAutoSwitchItems;
	private EditSwitchingItemScreen.RegisteredItemList miningAutoSwitchItems;
	private Runnable deferredTooltip;
	
	public EditSwitchingItemScreen(Screen parentScreen) {
		super(Component.translatable(EpicFightMod.MODID + ".gui.configuration.autoswitching"));
		this.parentScreen = parentScreen;
	}

	@Override
	protected void init() {
		if (this.battleAutoSwitchItems == null) {
			this.battleAutoSwitchItems = new EditSwitchingItemScreen.RegisteredItemList(200, this.height, Component.translatable(EpicFightMod.MODID+".gui.to_battle_mode"));
			EpicFightMod.CLIENT_CONFIGS.battleAutoSwitchItems.stream().sorted((e1, e2) -> e1.getDescriptionId().compareTo(e2.getDescriptionId())).forEach((item) -> this.battleAutoSwitchItems.addEntry(item));
		} else {
			this.battleAutoSwitchItems.resize(200, this.height);
		}
		
		if (this.miningAutoSwitchItems == null) {
			this.miningAutoSwitchItems = new EditSwitchingItemScreen.RegisteredItemList(200, this.height, Component.translatable(EpicFightMod.MODID+".gui.to_mining_mode"));
			EpicFightMod.CLIENT_CONFIGS.miningAutoSwitchItems.stream().sorted((e1, e2) -> e1.getDescriptionId().compareTo(e2.getDescriptionId())).forEach((item) -> this.miningAutoSwitchItems.addEntry(item));
		} else {
			this.miningAutoSwitchItems.resize(200, this.height);
		}

		this.battleAutoSwitchItems.setLeftPos(this.width / 2 - 204);
		this.miningAutoSwitchItems.setLeftPos(this.width / 2 + 4);
		this.addRenderableWidget(this.battleAutoSwitchItems);
		this.addRenderableWidget(this.miningAutoSwitchItems);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
			EpicFightMod.CLIENT_CONFIGS.battleAutoSwitchItems.clear();
			EpicFightMod.CLIENT_CONFIGS.miningAutoSwitchItems.clear();
			EpicFightMod.CLIENT_CONFIGS.battleAutoSwitchItems.addAll(this.battleAutoSwitchItems.toList());
			EpicFightMod.CLIENT_CONFIGS.miningAutoSwitchItems.addAll(this.miningAutoSwitchItems.toList());
			EpicFightMod.CLIENT_CONFIGS.save();
			this.onClose();
		}).bounds(this.width / 2 - 80, this.height - 28, 160, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(guiGraphics);
		this.battleAutoSwitchItems.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.miningAutoSwitchItems.render(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
		
		if (this.deferredTooltip != null) {
			this.deferredTooltip.run();
			this.deferredTooltip = null;
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parentScreen);
	}

	@OnlyIn(Dist.CLIENT)
	class RegisteredItemList extends ObjectSelectionList<EditSwitchingItemScreen.RegisteredItemList.ItemEntry> {
		private final Component title;

		public RegisteredItemList(int width, int height, Component title) {
			super(EditSwitchingItemScreen.this.minecraft, width, height, 32, height - 50, 22);
			this.title = title;
			this.setRenderHeader(true, (int)(9.0F * 1.5F));
			
			if (this.getSelected() != null) {
				this.centerScrollOn(this.getSelected());
			}
			
			this.addEntry(new ButtonInEntry());
		}
		
		public void resize(int width, int height) {
			this.width = width;
			this.height = height;
			this.y0 = 32;
			this.y1 = height - 50;
			this.x0 = 0;
			this.x1 = width;
		}

		@Override
		protected void renderHeader(GuiGraphics guiGraphics, int x, int y) {
			Component component = net.minecraft.network.chat.Component.literal("").append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
			guiGraphics.drawString(this.minecraft.font, component, x + this.width / 2 - this.minecraft.font.width(component) / 2, Math.min(this.y0 + 3, y), 16777215, false);
		}

		@Override
		public int getRowWidth() {
			return this.width;
		}

		@Override
		protected int getScrollbarPosition() {
			return this.x1 - 6;
		}

		protected void addEntry(Item item) {
			this.children().add(new ItemEntry(item.getDefaultInstance()));
		}

		protected void removeIfPresent(Item item) {
			this.children().remove(new ItemEntry(item.getDefaultInstance()));
		}

		protected List<Item> toList() {
			List<Item> list = Lists.newArrayList();
			
			for (ItemEntry entry : this.children()) {
				if (entry.itemStack != TITLE_STACK) {
					list.add(entry.itemStack.getItem());
				}
			}
			
			return list;
		}

		@OnlyIn(Dist.CLIENT)
		class ItemEntry extends ObjectSelectionList.Entry<EditSwitchingItemScreen.RegisteredItemList.ItemEntry> {
			private static final Set<Item> UNRENDERABLES = Sets.newHashSet();
			private final ItemStack itemStack;

			public ItemEntry(ItemStack itemStack) {
				this.itemStack = itemStack;
			}

			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
				try {
					if (!UNRENDERABLES.contains(this.itemStack.getItem())) {
						guiGraphics.renderItem(this.itemStack, left + 4, top + 1);
					}
				} catch (Exception e) {
					UNRENDERABLES.add(this.itemStack.getItem());
				}
				
				Component component = this.itemStack.getHoverName();
				guiGraphics.drawString(RegisteredItemList.this.minecraft.font, component, left + 30, top + 5, 16777215, false);
			}
			
			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					if (RegisteredItemList.this.getSelected() != null && RegisteredItemList.this.getSelected().equals(this)) {
						RegisteredItemList.this.removeEntry(this);
						return false;
					}
					
					RegisteredItemList.this.setSelected(this);
					return true;
				} else {
					return false;
				}
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof ItemEntry && !(this instanceof ButtonInEntry)) {
					return this.itemStack.equals(((ItemEntry)obj).itemStack);
				} else {
					return super.equals(obj);
				}
			}
			
			@Override
			public Component getNarration() {
				return  Component.translatable("narrator.select", this.itemStack.getHoverName());
			}
		}

		@OnlyIn(Dist.CLIENT)
		class ButtonInEntry extends ItemEntry {
			private final Button addItemButton;
			private final Button removeAllButton;
			private final Button automaticRegisterButton;

			public ButtonInEntry() {
				super(TITLE_STACK);
				
				this.addItemButton = Button.builder(Component.literal("+"), (button) -> {
					EditSwitchingItemScreen.RegisteredItemList thisList = EditSwitchingItemScreen.RegisteredItemList.this == EditSwitchingItemScreen.this.battleAutoSwitchItems ? EditSwitchingItemScreen.this.battleAutoSwitchItems : EditSwitchingItemScreen.this.miningAutoSwitchItems;
					EditSwitchingItemScreen.RegisteredItemList opponentList = EditSwitchingItemScreen.RegisteredItemList.this == EditSwitchingItemScreen.this.battleAutoSwitchItems ? EditSwitchingItemScreen.this.miningAutoSwitchItems : EditSwitchingItemScreen.this.battleAutoSwitchItems;
					RegisteredItemList.this.minecraft.setScreen(new EditItemListScreen(EditSwitchingItemScreen.this, thisList, opponentList));
				}).bounds(0, 0, 20, 20).build();
				
				this.removeAllButton = Button.builder(Component.translatable("epicfight.gui.delete_all"), (button) -> {
					RegisteredItemList.this.clearEntries();
					RegisteredItemList.this.addEntry(this);
				}).bounds(0, 0, 60, 20).build();
				
				this.automaticRegisterButton = Button.builder(Component.translatable("epicfight.gui.auto_add"), (button) -> {
					boolean isBattleTab = EditSwitchingItemScreen.RegisteredItemList.this == EditSwitchingItemScreen.this.battleAutoSwitchItems;
					
					if (isBattleTab) {
						for (Item item : ForgeRegistries.ITEMS.getValues()) {
							CapabilityItem itemCap = ItemCapabilityProvider.get(item);
							
							if (itemCap != null && itemCap instanceof WeaponCapability) {
								ItemEntry itemEntry = new ItemEntry(item.getDefaultInstance());
								
								if (!EditSwitchingItemScreen.this.battleAutoSwitchItems.children().contains(itemEntry)) {
									EditSwitchingItemScreen.this.battleAutoSwitchItems.addEntry(itemEntry);
								}
							}
						}
					} else {
						for (Item item : ForgeRegistries.ITEMS.getValues()) {
							ItemEntry itemEntry = new ItemEntry(item.getDefaultInstance());
							
							if (!EditSwitchingItemScreen.this.battleAutoSwitchItems.children().contains(itemEntry)) {
								if (!EditSwitchingItemScreen.this.miningAutoSwitchItems.children().contains(itemEntry)) {
									EditSwitchingItemScreen.this.miningAutoSwitchItems.addEntry(itemEntry);
								}
							}
						}
					}
				}).bounds(0, 0, 60, 20).tooltip(Tooltip.create(EditSwitchingItemScreen.RegisteredItemList.this == EditSwitchingItemScreen.this.battleAutoSwitchItems
						? Component.translatable("epicfight.gui.tooltip_battle") : Component.translatable("epicfight.gui.tooltip_mining"))).build();
			}
			
			@Override
			public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
				this.addItemButton.setX(left+25);
				this.addItemButton.setY(top-2);
				this.addItemButton.render(guiGraphics, mouseX, mouseY, partialTicks);
				
				this.removeAllButton.setX(left+47);
				this.removeAllButton.setY(top-2);
				this.removeAllButton.render(guiGraphics, mouseX, mouseY, partialTicks);
				
				this.automaticRegisterButton.setX(left+109);
				this.automaticRegisterButton.setY(top-2);
				this.automaticRegisterButton.render(guiGraphics, mouseX, mouseY, partialTicks);
			}
			
			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				if (button == 0) {
					if (this.addItemButton.isMouseOver(mouseX, mouseY)) {
						this.addItemButton.playDownSound(EditSwitchingItemScreen.this.minecraft.getSoundManager());
						this.addItemButton.onPress();
					}
					
					if (this.removeAllButton.isMouseOver(mouseX, mouseY)) {
						this.removeAllButton.playDownSound(EditSwitchingItemScreen.this.minecraft.getSoundManager());
						this.removeAllButton.onPress();
					}
					
					if (this.automaticRegisterButton.isMouseOver(mouseX, mouseY)) {
						this.automaticRegisterButton.playDownSound(EditSwitchingItemScreen.this.minecraft.getSoundManager());
						this.automaticRegisterButton.onPress();
					}
				}
				return false;
			}
		}
	}
}