package yesman.epicfight.events;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.data.loot.EpicFightLootTables;
import yesman.epicfight.data.loot.SkillBookLootModifier;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeGamerule;
import yesman.epicfight.network.server.SPChangeGamerule.SynchronizedGameRules;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.network.server.SPDatapackSyncSkill;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

@Mod.EventBusSubscriber(modid = EpicFightMod.MODID)
public class WorldEvents {
	@SubscribeEvent
	public static void onLootTableRegistry(final LootTableLoadEvent event) {
		EpicFightLootTables.modifyVanillaLootPools(event);
		SkillBookLootModifier.createSkillLootTable();
	}

	@SubscribeEvent
	public static void onDatapackSync(final OnDatapackSyncEvent event) {
		if (event.getPlayer() != null) {
			for (SynchronizedGameRules syncRule : SPChangeGamerule.SynchronizedGameRules.values()) {
				EpicFightNetworkManager.sendToPlayer(
						new SPChangeGamerule(syncRule, syncRule.getRule.apply(event.getPlayer().level())),
						event.getPlayer());
			}

			if (!event.getPlayer().getServer().isSingleplayerOwner(event.getPlayer().getGameProfile())) {
				synchronizeWorldData(event.getPlayer());
			} else {
				ServerPlayerPatch serverplayerpatch = EpicFightCapabilities.getEntityPatch(event.getPlayer(),
						ServerPlayerPatch.class);
				CapabilitySkill skillCapability = serverplayerpatch.getSkillCapability();

				for (SkillContainer skill : skillCapability.skillContainers) {
					if (skill.getSkill() != null) {
						// Reload skill
						skill.setSkill(SkillManager.getSkill(skill.getSkill().toString()), true);
					}
				}
			}
		} else {
			event.getPlayerList().getPlayers().forEach(WorldEvents::synchronizeWorldData);
		}
	}

	public static void synchronizeWorldData(ServerPlayer player) {
		ServerPlayerPatch serverplayerpatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
		CapabilitySkill skillCapability = serverplayerpatch.getSkillCapability();

		for (SkillContainer skill : skillCapability.skillContainers) {
			if (skill.getSkill() != null) {
				// Reload skill
				skill.setSkill(SkillManager.getSkill(skill.getSkill().toString()), true);
			}
		}

		List<CompoundTag> skillParams = SkillManager.getSkillParams();
		SPDatapackSyncSkill skillParamsPacket = new SPDatapackSyncSkill(skillParams.size(),
				SPDatapackSync.Type.SKILL_PARAMS);

		for (SkillCategory category : SkillCategory.ENUM_MANAGER.universalValues()) {
			if (skillCapability.hasCategory(category)) {
				skillParamsPacket.addLearnedSkill(Lists.newArrayList(skillCapability.getLearnedSkills(category).stream()
						.map((skill) -> skill.toString()).iterator()));
			}
		}

		skillParams.forEach(skillParamsPacket::write);
		EpicFightNetworkManager.sendToPlayer(skillParamsPacket, player);

		SPDatapackSync animationPacket = new SPDatapackSync(AnimationManager.getInstance().getUserAnimationsCount(),
				player.getServer().isResourcePackRequired() ? SPDatapackSync.Type.ANIMATION_IN_MANDATORY_RESOURCE_PACK
						: SPDatapackSync.Type.ANIMATION_IN_RESOURCE_PACK);
		SPDatapackSync armorPacket = new SPDatapackSync(ItemCapabilityReloadListener.armorCount(),
				SPDatapackSync.Type.ARMOR);
		SPDatapackSync weaponPacket = new SPDatapackSync(ItemCapabilityReloadListener.weaponCount(),
				SPDatapackSync.Type.WEAPON);
		SPDatapackSync weaponTypePacket = new SPDatapackSync(WeaponTypeReloadListener.getTagCount(),
				SPDatapackSync.Type.WEAPON_TYPE);

		AnimationManager.getInstance().getUserAnimationStream().forEach(animationPacket::write);
		ItemCapabilityReloadListener.getArmorDataStream().forEach(armorPacket::write);
		ItemCapabilityReloadListener.getWeaponDataStream().forEach(weaponPacket::write);
		WeaponTypeReloadListener.getWeaponTypeDataStream().forEach(weaponTypePacket::write);

		EpicFightNetworkManager.sendToPlayer(animationPacket, player);
		EpicFightNetworkManager.sendToPlayer(weaponTypePacket, player);
		EpicFightNetworkManager.sendToPlayer(armorPacket, player);
		EpicFightNetworkManager.sendToPlayer(weaponPacket, player);
	}
}