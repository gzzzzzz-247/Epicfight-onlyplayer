package yesman.epicfight.world.damagesource;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Sets;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Animations;

public class EpicFightDamageSource extends DamageSource {
	private final DamageSourceElements damageSourceElements = new DamageSourceElements();
	private final Set<TagKey<DamageType>> runtimeTags = Sets.newHashSet();
	private final Set<ResourceKey<DamageType>> runtimeTypes = Sets.newHashSet();
	
	private StaticAnimation animation;
	private Vec3 initialPosition;
	private boolean basicAttack;
	
	public EpicFightDamageSource(DamageSource damageSource) {
		this(damageSource.typeHolder(), damageSource.getDirectEntity(), damageSource.getEntity(), damageSource.getSourcePosition());
	}

	public EpicFightDamageSource(Holder<DamageType> damageType, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 initialPosition) {
		super(damageType, directEntity, causingEntity, initialPosition);
		this.initialPosition = initialPosition;
	}

	public DamageSourceElements getDamageSourceElements() {
		return damageSourceElements;
	}

	public EpicFightDamageSource setHurtItem(ItemStack hurtItem) {
		this.getDamageSourceElements().hurtItem = hurtItem;
		return this;
	}

	public ItemStack getHurtItem() {
		return this.getDamageSourceElements().hurtItem;
	}

	public EpicFightDamageSource setDamageModifier(ValueModifier damageModifier) {
		this.getDamageSourceElements().damageModifier = damageModifier;
		return this;
	}

	public ValueModifier getDamageModifier() {
		return this.getDamageSourceElements().damageModifier;
	}

	public EpicFightDamageSource setImpact(float f) {
		this.getDamageSourceElements().impact = f;
		return this;
	}

	public float getImpact() {
		return this.getDamageSourceElements().impact;
	}

	public EpicFightDamageSource setArmorNegation(float f) {
		this.getDamageSourceElements().armorNegation = f;
		return this;
	}

	public float getArmorNegation() {
		return this.getDamageSourceElements().armorNegation;
	}

	public EpicFightDamageSource setStunType(StunType stunType) {
		this.getDamageSourceElements().stunType = stunType;
		return this;
	}

	public StunType getStunType() {
		return this.getDamageSourceElements().stunType;
	}
	
	public EpicFightDamageSource addExtraDamage(ExtraDamageInstance extraDamage) {
		if (this.getDamageSourceElements().extraDamages == null) {
			this.getDamageSourceElements().extraDamages = Sets.newHashSet();
		}

		this.getDamageSourceElements().extraDamages.add(extraDamage);

		return this;
	}

	public Set<ExtraDamageInstance> getExtraDamages() {
		return this.getDamageSourceElements().extraDamages;
	}


	public EpicFightDamageSource setInitialPosition(Vec3 initialPosition) {
		this.initialPosition = initialPosition;
		return this;
	}

	public Vec3 getInitialPosition() {
		return initialPosition;
	}
	
	public void setBasicAttack(boolean basicAttack) {
		this.basicAttack = basicAttack;
	}
	
	public boolean isBasicAttack() {
		return basicAttack;
	}
	
	public EpicFightDamageSource setAnimation(StaticAnimation animation) {
		this.animation = animation;
		return this;
	}
	
	public StaticAnimation getAnimation() {
		return this.animation == null ? Animations.DUMMY_ANIMATION : this.animation;
	}
	
	@Override
	public boolean is(TagKey<DamageType> type) {
		return this.runtimeTags.contains(type) || super.is(type);
	}

	@Override
	public boolean is(ResourceKey<DamageType> type) {
		return this.runtimeTypes.contains(type) || super.is(type);
	}
	
	public EpicFightDamageSource addRuntimeTag(TagKey<DamageType> type) {
		this.runtimeTags.add(type);
		return this;
	}

	public EpicFightDamageSource addRuntimeTag(ResourceKey<DamageType> type) {
		this.runtimeTypes.add(type);
		return this;
	}
}