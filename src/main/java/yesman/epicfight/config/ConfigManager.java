package yesman.epicfight.config;

import java.io.File;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class ConfigManager {
	public static final ForgeConfigSpec COMMON_CONFIG;
	public static final ForgeConfigSpec CLIENT_CONFIG;
	public static final ClientConfig INGAME_CONFIG;
	public static final ForgeConfigSpec.BooleanValue DO_VANILLA_ATTACK;
	public static final ForgeConfigSpec.BooleanValue GLOBAL_STUN;
	public static final ForgeConfigSpec.BooleanValue KEEP_SKILLS;
	public static final ForgeConfigSpec.BooleanValue HAS_FALL_ANIMATION;
	public static final ForgeConfigSpec.BooleanValue DISABLE_ENTITY_UI;
	public static final ForgeConfigSpec.BooleanValue CAN_SWITCH_COMBAT;
	public static final ForgeConfigSpec.BooleanValue STIFF_COMBO_ATTACKS;
	public static final ForgeConfigSpec.BooleanValue NO_MOBS_IN_BOSSFIGHT;
	public static final ForgeConfigSpec.BooleanValue EPIC_DROP;
	public static final ForgeConfigSpec.EnumValue<PlayerPatch.PlayerMode> INITIAL_PLAYER_MODE;
	
	public static final ForgeConfigSpec.IntValue WEIGHT_PENALTY;
	public static final ForgeConfigSpec.IntValue SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER;
	public static final ForgeConfigSpec.IntValue SKILL_BOOK_CHEST_LOOT_MODIFYER;
	
	static {
		CommentedFileConfig file = CommentedFileConfig.builder(new File(FMLPaths.CONFIGDIR.get().resolve(EpicFightMod.CONFIG_FILE_PATH).toString())).sync().autosave().writingMode(WritingMode.REPLACE).build();
		file.load();
		ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
		
		DO_VANILLA_ATTACK = server.define("default_gamerule.doVanillaAttack", true);
		GLOBAL_STUN = server.define("default_gamerule.globalStun", true);
		KEEP_SKILLS = server.define("default_gamerule.keepSkills", true);
		HAS_FALL_ANIMATION = server.define("default_gamerule.hasFallAnimation", true);
		DISABLE_ENTITY_UI = server.define("default_gamerule.disapleEntityUI", false);
		STIFF_COMBO_ATTACKS = server.define("default_gamerule.stiffComboAttacks", true);
		NO_MOBS_IN_BOSSFIGHT = server.define("default_gamerule.noMobsInBossfight", true);
		EPIC_DROP = server.define("default_gamerule.epicDrop", true);
		WEIGHT_PENALTY = server.defineInRange("default_gamerule.weightPenalty", 100, 0, 100);
		SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER = server.defineInRange("loot.skill_book_mob_drop_chance_modifier", 0, -100, 100);
		SKILL_BOOK_CHEST_LOOT_MODIFYER = server.defineInRange("loot.skill_book_chest_drop_chance_modifier", 0, -100, 100);
		CAN_SWITCH_COMBAT = server.define("default_gamerule.canSwitchCombat", true);
		INITIAL_PLAYER_MODE = server.defineEnum("default_gamerule.initialPlayerMode", PlayerPatch.PlayerMode.MINING);
		
		INGAME_CONFIG = new ClientConfig(client);
		CLIENT_CONFIG = client.build();
		COMMON_CONFIG = server.build();
	}
	
	public static void loadConfig(ForgeConfigSpec config, String path) {
		CommentedFileConfig file = CommentedFileConfig.builder(new File(path)).sync().autosave().writingMode(WritingMode.REPLACE).build();
		file.load();
		config.setConfig(file);
	}
}