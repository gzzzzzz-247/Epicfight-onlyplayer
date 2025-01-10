package yesman.epicfight.api.data.reloader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.EpicFightSkills;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.server.SPDatapackSyncSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public class SkillManager extends SimpleJsonResourceReloadListener {
	public static final ResourceKey<Registry<Skill>> SKILL_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(EpicFightMod.MODID, "skill"));
	private static final List<CompoundTag> SKILL_PARAMS = Lists.newArrayList();
	private static final Gson GSON = (new GsonBuilder()).create();
	private static Set<String> namespaces;
	
	public static List<CompoundTag> getSkillParams() {
		return Collections.unmodifiableList(SKILL_PARAMS);
	}
	
	public static void createSkillRegistry(NewRegistryEvent event) {
		event.create(RegistryBuilder.of(new ResourceLocation(EpicFightMod.MODID, "skill")).addCallback(SkillRegistryCallbacks.INSTANCE));
	}
	
	public static void registerSkills(RegisterEvent event) {
		if (event.getRegistryKey().equals(SKILL_REGISTRY_KEY)) {
			final SkillBuildEvent skillBuildEvnet = new SkillBuildEvent();
			ModLoader.get().postEvent(skillBuildEvnet);
			
			namespaces = ImmutableSet.copyOf(skillBuildEvnet.getNamespaces());
			
			event.register(SKILL_REGISTRY_KEY, (helper) -> {
				skillBuildEvnet.getAllSkills().forEach((skill) -> {
					helper.register(skill.getRegistryName(), skill);
				});
			});
		}
	}
	
	public static Skill getSkill(String name) {
		IForgeRegistry<Skill> skillRegistry = getSkillRegistry();
		ResourceLocation rl;
		
		if (name.indexOf(':') >= 0) {
			rl = new ResourceLocation(name);
		} else {
			rl = new ResourceLocation(EpicFightMod.MODID, name);
		}
		
		if (skillRegistry.containsKey(rl)) {
			return skillRegistry.getValue(rl);
		} else {
			return null;
		}
	}
	
	public static Collection<Skill> getSkills(Predicate<Skill> predicate) {
		return getSkillRegistry().getValues().stream().filter(skill -> predicate.test(skill)).toList();
	}
	
	public static Stream<ResourceLocation> getSkillNames(Predicate<Skill> predicate) {
		return getSkillRegistry().getValues().stream().filter(skill -> predicate.test(skill)).map(skill -> skill.getRegistryName());
	}
	
	public static Set<String> getNamespaces() {
		return namespaces;
	}
	
	public static void reloadAllSkillsAnimations() {
		IForgeRegistry<Skill> skillRegistry = getSkillRegistry();
		skillRegistry.getValues().forEach((skill) -> skill.registerPropertiesToAnimation());
	}
	
	public static IForgeRegistry<Skill> getSkillRegistry() {
		return RegistryManager.ACTIVE.getRegistry(SKILL_REGISTRY_KEY);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void processServerPacket(SPDatapackSyncSkill packet) {
		IForgeRegistry<Skill> skillRegistry = getSkillRegistry();
		
		for (CompoundTag tag : packet.getTags()) {
			if (!skillRegistry.containsKey(new ResourceLocation(tag.getString("id")))) {
				EpicFightMod.LOGGER.warn("Failed to syncronize Datapack for skill: " + tag.getString("id"));
				continue;
			}
			
			skillRegistry.getValue(new ResourceLocation(tag.getString("id"))).setParams(tag);
		}
		
		LocalPlayerPatch localplayerpatch = ClientEngine.getInstance().getPlayerPatch();
		
		if (localplayerpatch != null) {
			CapabilitySkill skillCapability = localplayerpatch.getSkillCapability();
			
			for (String skillName : packet.getLearnedSkills()) {
				skillCapability.addLearnedSkill(getSkill(skillName));
			}
			
			for (SkillContainer skill : skillCapability.skillContainers) {
				if (skill.getSkill() != null) {
					// Reload skill
					skill.setSkill(getSkill(skill.getSkill().toString()), true);
				}
			}
			
			skillCapability.skillContainers[SkillCategories.BASIC_ATTACK.universalOrdinal()].setSkill(EpicFightSkills.BASIC_ATTACK);
			skillCapability.skillContainers[SkillCategories.AIR_ATTACK.universalOrdinal()].setSkill(EpicFightSkills.AIR_ATTACK);
			skillCapability.skillContainers[SkillCategories.KNOCKDOWN_WAKEUP.universalOrdinal()].setSkill(EpicFightSkills.KNOCKDOWN_WAKEUP);
		}
	}
	
	private static Pair<ResourceLocation, CompoundTag> parseParameters(Map.Entry<ResourceLocation, JsonElement> entry) {
		try {
			CompoundTag tag = TagParser.parseTag(entry.getValue().toString());
			tag.putString("id", entry.getKey().toString());
			SKILL_PARAMS.add(tag);
			
			return Pair.of(entry.getKey(), tag);
		} catch (CommandSyntaxException e) {
			EpicFightMod.LOGGER.warn("Can't parse skill parameter for " + entry.getKey() + " because of " + e.getMessage());
			e.printStackTrace();
			
			return Pair.of(entry.getKey(), new CompoundTag());
		}
	}
	
	private static final SkillManager INSTANCE = new SkillManager();
	
	public static SkillManager getInstance() {
		return INSTANCE;
	}
	
	public SkillManager() {
		super(GSON, "skill_parameters");
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManager, ProfilerFiller profileFiller) {
		IForgeRegistry<Skill> skillRegistry = getSkillRegistry();
		SKILL_PARAMS.clear();
		
		objectIn.entrySet().stream().filter((entry) -> {
			if (!skillRegistry.containsKey(entry.getKey())) {
				EpicFightMod.LOGGER.warn("Skill " + entry.getKey() + " doesn't exist in the registry.");
				return false;
			}
			
			return true;
		}).map(SkillManager::parseParameters).forEach((pair) -> skillRegistry.getValue(pair.getFirst()).setParams(pair.getSecond()));
	}
	
	private static class SkillRegistryCallbacks implements IForgeRegistry.BakeCallback<Skill>, IForgeRegistry.CreateCallback<Skill>, IForgeRegistry.ClearCallback<Skill> {
		private static final ResourceLocation LEARNABLE_SKILLS = new ResourceLocation(EpicFightMod.MODID, "learnableskills");
		private static final SkillRegistryCallbacks INSTANCE = new SkillRegistryCallbacks();
		
		@Override
		@SuppressWarnings("unchecked")
        public void onBake(IForgeRegistryInternal<Skill> owner, RegistryManager stage) {
			final Map<ResourceLocation, Skill> learnableSkills = owner.getSlaveMap(LEARNABLE_SKILLS, Map.class);
			owner.getEntries().stream().filter((entry) -> entry.getValue().getCategory().learnable()).forEach((entry) -> learnableSkills.put(entry.getKey().location(), entry.getValue()));
        }
		
		@Override
		public void onCreate(IForgeRegistryInternal<Skill> owner, RegistryManager stage) {
			owner.setSlaveMap(LEARNABLE_SKILLS, Maps.newHashMap());
		}
		
		@Override
        public void onClear(IForgeRegistryInternal<Skill> owner, RegistryManager stage) {
			owner.getSlaveMap(LEARNABLE_SKILLS, Map.class).clear();
        }
	}
}