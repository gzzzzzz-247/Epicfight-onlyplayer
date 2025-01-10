package yesman.epicfight.events;

import java.io.File;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeGamerule;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.ItemUseEndEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.RightClickItemEvent;
import yesman.epicfight.world.gamerule.EpicFightGamerules;

@Mod.EventBusSubscriber(modid = EpicFightMod.MODID)
public class PlayerEvents {
	@SubscribeEvent
	public static void startTrackingEvent(StartTracking event) {
		Entity trackingTarget = event.getTarget();
		EntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(trackingTarget, EntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onStartTracking((ServerPlayer)event.getEntity());
		}
	}
	
	@SubscribeEvent
	public static void playerLoadEvent(PlayerEvent.LoadFromFile event) {
		ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), ServerPlayerPatch.class);
		File file = new File(event.getPlayerDirectory(), event.getPlayerUUID() +".dat");
		
		if (!file.exists()) {
			int initialMode = Math.min(event.getEntity().level().getGameRules().getInt(EpicFightGamerules.INITIAL_PLAYER_MODE), PlayerPatch.PlayerMode.values().length - 1);
			playerpatch.toMode(PlayerPatch.PlayerMode.values()[initialMode], true);
		}
	}
	
	@SubscribeEvent
	public static void rightClickItemServerEvent(RightClickItem event) {
		if (event.getSide() == LogicalSide.SERVER) {
			ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), ServerPlayerPatch.class);
			ItemStack itemstack = playerpatch.getOriginal().getOffhandItem();
			
			if (playerpatch != null && (itemstack.getUseAnimation() == UseAnim.NONE || !playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getStyle(playerpatch).canUseOffhand())) {
				boolean canceled = playerpatch.getEventListener().triggerEvents(EventType.SERVER_ITEM_USE_EVENT, new RightClickItemEvent<>(playerpatch));
				
				if (playerpatch.getEntityState().movementLocked()) {
					canceled = true;
				}
				
				event.setCanceled(canceled);
			}
		}
	}
	
	@SubscribeEvent
	public static void itemUseStartEvent(LivingEntityUseItemEvent.Start event) {
		if (event.getEntity() instanceof Player player) {
			PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), PlayerPatch.class);
			
			if (playerpatch == null) {
				return;
			}
			
			InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND).equals(event.getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			CapabilityItem itemCap = playerpatch.getHoldingItemCapability(hand);
			
			if (!playerpatch.getEntityState().canUseSkill()) {
				event.setCanceled(true);
			} else if (event.getItem() == player.getOffhandItem() && !playerpatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getStyle(playerpatch).canUseOffhand()) {
				event.setCanceled(true);
			}
			
			if (itemCap.getUseAnimation(playerpatch) == UseAnim.BLOCK) {
				event.setDuration(Integer.MAX_VALUE);
			}
		}
	}
	
	@SubscribeEvent
	public static void cloneEvent(PlayerEvent.Clone event) {
		event.getOriginal().reviveCaps();
		ServerPlayerPatch oldCap = EpicFightCapabilities.getEntityPatch(event.getOriginal(), ServerPlayerPatch.class);
		
		if (oldCap != null) {
			ServerPlayerPatch newCap = EpicFightCapabilities.getEntityPatch(event.getEntity(), ServerPlayerPatch.class);
			
			if ((!event.isWasDeath() || event.getOriginal().level().getGameRules().getBoolean(EpicFightGamerules.KEEP_SKILLS))) {
				newCap.copySkillsFrom(oldCap);
			}
			
			newCap.toMode(oldCap.getPlayerMode(), false);
		}
		
		event.getOriginal().invalidateCaps();
	}
	
	@SubscribeEvent
	public static void changeDimensionEvent(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();
		ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
		playerpatch.getAnimator().resetLivingAnimations();
		playerpatch.modifyLivingMotionByCurrentItem(true);
		
		EpicFightNetworkManager.sendToPlayer(new SPChangeGamerule(SPChangeGamerule.SynchronizedGameRules.WEIGHT_PENALTY, player.level().getGameRules().getInt(EpicFightGamerules.WEIGHT_PENALTY)), (ServerPlayer)player);
		EpicFightNetworkManager.sendToPlayer(new SPChangeGamerule(SPChangeGamerule.SynchronizedGameRules.DIABLE_ENTITY_UI, player.level().getGameRules().getBoolean(EpicFightGamerules.DISABLE_ENTITY_UI)), (ServerPlayer)player);
	}
	
	@SubscribeEvent
	public static void itemUseStopEvent(LivingEntityUseItemEvent.Stop event) {
		if (!event.getEntity().level().isClientSide()) {
			if (event.getEntity() instanceof ServerPlayer player) {
				ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
				
				if (playerpatch != null) {
					boolean canceled = playerpatch.getEventListener().triggerEvents(EventType.SERVER_ITEM_STOP_EVENT, new ItemUseEndEvent(playerpatch, event));
					event.setCanceled(canceled);
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void attackEntityEvent(AttackEntityEvent event) {
		boolean isLivingTarget = event.getTarget() instanceof LivingEntity livingEntity && livingEntity.attackable();
		PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), PlayerPatch.class);

		if (playerpatch != null) {
			if (!event.getEntity().level().getGameRules().getBoolean(EpicFightGamerules.DO_VANILLA_ATTACK) && isLivingTarget && playerpatch.getEpicFightDamageSource() == null && !fakePlayerCheck(event.getEntity())) {
				event.setCanceled(true);
			}
		}
	}
	
	// Fixed by Saithe6(github)
	public static boolean fakePlayerCheck(Player source) {
		return source instanceof FakePlayer;
	}
}
