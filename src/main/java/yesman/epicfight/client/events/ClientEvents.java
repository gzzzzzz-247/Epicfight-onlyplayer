package yesman.epicfight.client.events;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;
import yesman.epicfight.world.capabilities.provider.ItemCapabilityProvider;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.RightClickItemEvent;
import yesman.epicfight.world.level.block.FractureBlockState;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
	private static final Pair<ResourceLocation, ResourceLocation> OFFHAND_TEXTURE = Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
	private static final Minecraft minecraft = Minecraft.getInstance();
	
	@SubscribeEvent
	public static void mouseClickEvent(ScreenEvent.MouseButtonPressed.Pre event) {
		if (event.getScreen() instanceof AbstractContainerScreen) {
			Slot slot = ((AbstractContainerScreen<?>)event.getScreen()).getSlotUnderMouse();
			
			if (slot != null) {
				CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(minecraft.player.containerMenu.getCarried());
				
				if (!cap.canBePlacedOffhand()) {
					if (slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE)) {
						event.setCanceled(true);
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void mouseReleaseEvent(ScreenEvent.MouseButtonReleased.Pre event) {
		if (event.getScreen() instanceof AbstractContainerScreen) {
			Slot slot = ((AbstractContainerScreen<?>)event.getScreen()).getSlotUnderMouse();
			
			if (slot != null) {
				CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(minecraft.player.containerMenu.getCarried());
				
				if (!cap.canBePlacedOffhand()) {
					if (slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE)) {
						event.setCanceled(true);
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void presssKeyInGui(ScreenEvent.KeyPressed.Pre event) {
		CapabilityItem itemCapability = CapabilityItem.EMPTY;
		
		if (event.getKeyCode() == minecraft.options.keySwapOffhand.getKey().getValue()) {
			if (event.getScreen() instanceof AbstractContainerScreen) {
				Slot slot = ((AbstractContainerScreen<?>)event.getScreen()).getSlotUnderMouse();
				
				if (slot != null && slot.hasItem()) {
					itemCapability = EpicFightCapabilities.getItemStackCapability(slot.getItem());
					
					if (!itemCapability.canBePlacedOffhand()) {
						event.setCanceled(true);
					}
				}
			}
		} else if (event.getKeyCode() >= 49 && event.getKeyCode() <= 57) {
			if (event.getScreen() instanceof AbstractContainerScreen) {
				Slot slot = ((AbstractContainerScreen<?>)event.getScreen()).getSlotUnderMouse();
				
				if (slot != null && slot.getNoItemIcon() != null && slot.getNoItemIcon().equals(OFFHAND_TEXTURE)) {
					itemCapability = EpicFightCapabilities.getItemStackCapability(minecraft.player.getInventory().getItem(event.getKeyCode() - 49));
					
					if (!itemCapability.canBePlacedOffhand()) {
						event.setCanceled(true);
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void rightClickItemClient(PlayerInteractEvent.RightClickItem event) {
		if (event.getSide() == LogicalSide.CLIENT) {
			LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LocalPlayerPatch.class);
			
			if (playerpatch != null && playerpatch.getOriginal().getOffhandItem().getUseAnimation() == UseAnim.NONE) {
				boolean canceled = playerpatch.getEventListener().triggerEvents(EventType.CLIENT_ITEM_USE_EVENT, new RightClickItemEvent<>(playerpatch));
				
				if (playerpatch.getEntityState().movementLocked()) {
					canceled = true;
				}
				
				event.setCanceled(canceled);
			}
		}
	}
	
	@SubscribeEvent
	public static void clientLoggingInEvent(ClientPlayerNetworkEvent.LoggingIn event) {
		LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(), LocalPlayerPatch.class);
		
		if (playerpatch != null) {
			ClientEngine.getInstance().controllEngine.setPlayerPatch(playerpatch);
		}
		
		ClientEngine.getInstance().renderEngine.battleModeUI.reset();
		ClientEngine.getInstance().renderEngine.versionNotifier.reset();
	}
	
	/**
	 * Bad code: should be fixed after Forge provides any parameters that can figure out if respawning caused by dimension changes
	 */
	@Deprecated
	public static ClientboundRespawnPacket packet;
	
	@SubscribeEvent
	public static void clientRespawnEvent(ClientPlayerNetworkEvent.Clone event) {
		LocalPlayerPatch oldCap = EpicFightCapabilities.getEntityPatch(event.getOldPlayer(), LocalPlayerPatch.class);
		LocalPlayerPatch newCap = EpicFightCapabilities.getEntityPatch(event.getNewPlayer(), LocalPlayerPatch.class);
		
		/**
		 * oldCap == null when a player revives after it disappears
		 */
		if (oldCap != null) {
			if (newCap != null) {
				if (packet != null && packet.shouldKeep((byte)3)) {
					newCap.copySkillsFrom(oldCap);
				}
				
				packet = null;
				newCap.onRespawnLocalPlayer(event);
				newCap.toMode(oldCap.getPlayerMode(), false);
			}
		}
		
		ClientEngine.getInstance().controllEngine.setPlayerPatch(newCap);
		ClientEngine.getInstance().renderEngine.battleModeUI.reset();
		ClientEngine.getInstance().renderEngine.versionNotifier.reset();
	}
	
	@SubscribeEvent
	public static void clientLogoutEvent(ClientPlayerNetworkEvent.LoggingOut event) {
		if (event.getPlayer() != null) {
			ItemCapabilityReloadListener.reset();
			ItemCapabilityProvider.clear();
			EntityPatchProvider.clear();
			WeaponTypeReloadListener.clear();
			
			ClientEngine.getInstance().renderEngine.zoomOut(0);
			ClientEngine.getInstance().renderEngine.battleModeUI.reset();
			// Reset renderers
			ClientEngine.getInstance().renderEngine.resetRenderers();
		}
		
		FractureBlockState.reset();
	}
}