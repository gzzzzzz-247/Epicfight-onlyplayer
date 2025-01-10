package yesman.epicfight.world.entity.ai.attribute;

import java.util.UUID;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.entity.EpicFightEntities;
import yesman.epicfight.world.entity.WitherGhostClone;

@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class EpicFightAttributes {
	static {
		Attributes.ATTACK_DAMAGE.setSyncable(true);
	}

	public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES,
			EpicFightMod.MODID);

	public static final RegistryObject<Attribute> MAX_STAMINA = ATTRIBUTES.register("staminar",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".staminar", 15.0D, 0.0D, 1024.0D)
					.setSyncable(true));
	public static final RegistryObject<Attribute> STAMINA_REGEN = ATTRIBUTES.register("stamina_regen",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".stamina_regen", 1.0D, 0.0D, 1024.0D)
					.setSyncable(true));
	public static final RegistryObject<Attribute> STUN_ARMOR = ATTRIBUTES.register("stun_armor",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".stun_armor", 0.0D, 0.0D, 1024.0D)
					.setSyncable(true));
	public static final RegistryObject<Attribute> WEIGHT = ATTRIBUTES.register("weight",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".weight", 0.0D, 0.0D, 1024.0)
					.setSyncable(true));
	public static final RegistryObject<Attribute> MAX_STRIKES = ATTRIBUTES.register("max_strikes",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".max_strikes", 1.0D, 1.0D, 1024.0)
					.setSyncable(true));
	public static final RegistryObject<Attribute> ARMOR_NEGATION = ATTRIBUTES.register("armor_negation",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".armor_negation", 0.0D, 0.0D, 100.0D)
					.setSyncable(true));
	public static final RegistryObject<Attribute> IMPACT = ATTRIBUTES.register("impact",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".impact", 0.5D, 0.0D, 1024.0)
					.setSyncable(true));
	public static final RegistryObject<Attribute> EXECUTION_RESISTANCE = ATTRIBUTES.register("execution_resistance",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".execution_resistance", 0.0D, 0.0D,
					10.0D).setSyncable(true));

	public static final RegistryObject<Attribute> OFFHAND_ATTACK_SPEED = ATTRIBUTES.register("offhand_attack_speed",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".offhand_attack_speed", 4.0D, 0.0D,
					1024.0D).setSyncable(true));
	public static final RegistryObject<Attribute> OFFHAND_MAX_STRIKES = ATTRIBUTES.register("offhand_max_strikes",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".offhand_max_strikes", 1.0D, 1.0D,
					1024.0).setSyncable(true));
	public static final RegistryObject<Attribute> OFFHAND_ARMOR_NEGATION = ATTRIBUTES.register("offhand_armor_negation",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".offhand_armor_negation", 0.0D, 0.0D,
					100.0D).setSyncable(true));
	public static final RegistryObject<Attribute> OFFHAND_IMPACT = ATTRIBUTES.register("offhand_impact",
			() -> new RangedAttribute("attribute.name." + EpicFightMod.MODID + ".offhand_impact", 0.5D, 0.0D, 1024.0)
					.setSyncable(true));

	public static final UUID ARMOR_NEGATION_MODIFIER = UUID.fromString("b0a7436e-5734-11eb-ae93-0242ac130002");
	public static final UUID MAX_STRIKE_MODIFIER = UUID.fromString("b0a745b2-5734-11eb-ae93-0242ac130002");
	public static final UUID IMPACT_MODIFIER = UUID.fromString("b0a746ac-5734-11eb-ae93-0242ac130002");
	public static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("1c224694-19f3-11ec-9621-0242ac130002");
	public static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("1c2249f0-19f3-11ec-9621-0242ac130002");

	@SubscribeEvent
	public static void entityAttributeCreationEvent(EntityAttributeCreationEvent event) {
		event.put(EpicFightEntities.WITHER_SKELETON_MINION.get(), AbstractSkeleton.createAttributes().build());
		event.put(EpicFightEntities.WITHER_GHOST_CLONE.get(), WitherGhostClone.createAttributes().build());
		event.put(EpicFightEntities.DODGE_LEFT.get(), LivingEntity.createLivingAttributes().build());
	}

	@SubscribeEvent
	public static void entityAttributeModificationEvent(EntityAttributeModificationEvent event) {
		player(EntityType.PLAYER, event);
	}

	private static void common(EntityType<? extends LivingEntity> entityType, EntityAttributeModificationEvent event) {
		event.add(entityType, EpicFightAttributes.WEIGHT.get());
		event.add(entityType, EpicFightAttributes.ARMOR_NEGATION.get());
		event.add(entityType, EpicFightAttributes.IMPACT.get());
		event.add(entityType, EpicFightAttributes.MAX_STRIKES.get());
		event.add(entityType, EpicFightAttributes.STUN_ARMOR.get());
	}

	private static void humanoid(EntityType<? extends LivingEntity> entityType,
			EntityAttributeModificationEvent event) {
		common(entityType, event);
		event.add(entityType, EpicFightAttributes.OFFHAND_ATTACK_SPEED.get());
		event.add(entityType, EpicFightAttributes.OFFHAND_MAX_STRIKES.get());
		event.add(entityType, EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get());
		event.add(entityType, EpicFightAttributes.OFFHAND_IMPACT.get());
	}

	private static void player(EntityType<? extends LivingEntity> entityType, EntityAttributeModificationEvent event) {
		humanoid(entityType, event);
		event.add(entityType, EpicFightAttributes.MAX_STAMINA.get());
		event.add(entityType, EpicFightAttributes.STAMINA_REGEN.get());
	}

	public static AttributeModifier getArmorNegationModifier(double value) {
		return new AttributeModifier(EpicFightAttributes.ARMOR_NEGATION_MODIFIER,
				EpicFightMod.MODID + ":weapon_modifier", value, AttributeModifier.Operation.ADDITION);
	}

	public static AttributeModifier getMaxStrikesModifier(int value) {
		return new AttributeModifier(EpicFightAttributes.MAX_STRIKE_MODIFIER, EpicFightMod.MODID + ":weapon_modifier",
				value, AttributeModifier.Operation.ADDITION);
	}

	public static AttributeModifier getImpactModifier(double value) {
		return new AttributeModifier(EpicFightAttributes.IMPACT_MODIFIER, EpicFightMod.MODID + ":weapon_modifier",
				value, AttributeModifier.Operation.ADDITION);
	}

	public static AttributeModifier getDamageBonusModifier(double value) {
		return new AttributeModifier(ATTACK_DAMAGE_MODIFIER, EpicFightMod.MODID + ":weapon_modifier", value,
				AttributeModifier.Operation.ADDITION);
	}

	public static AttributeModifier getSpeedBonusModifier(double value) {
		return new AttributeModifier(ATTACK_SPEED_MODIFIER, EpicFightMod.MODID + ":weapon_modifier", value,
				AttributeModifier.Operation.ADDITION);
	}
}