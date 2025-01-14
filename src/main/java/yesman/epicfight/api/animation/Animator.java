package yesman.epicfight.api.animation;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;

import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap.TypeKey;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public abstract class Animator {
	protected final Map<LivingMotion, StaticAnimation> livingAnimations = Maps.newHashMap();
	protected final TypeFlexibleHashMap<TypeKey<?>> animationVariables = new TypeFlexibleHashMap<> (false);
	protected LivingEntityPatch<?> entitypatch;
	
	public Animator() {
		// Put default variables
		this.animationVariables.put(AttackAnimation.HIT_ENTITIES, Lists.newArrayList());
		this.animationVariables.put(AttackAnimation.HURT_ENTITIES, Lists.newArrayList());
	}
	
	public abstract void playAnimation(StaticAnimation nextAnimation, float convertTimeModifier);
	public abstract void playAnimationInstantly(StaticAnimation nextAnimation);
	public abstract void tick();
	/** Standby until the current animation is completely end. Mostly used for link two animations having the same last & first keyframe pose on {@link DynamicAnimation#end(LivingEntityPatch, boolean)} **/
	public abstract void reserveAnimation(StaticAnimation nextAnimation);
	public abstract EntityState getEntityState();
	/** Give a null value as a parameter to get an animation that is highest priority on client **/
	public abstract AnimationPlayer getPlayerFor(@Nullable DynamicAnimation playingAnimation);
	public abstract <T> Pair<AnimationPlayer, T> findFor(Class<T> animationType);
	public abstract Pose getPose(float partialTicks);
	
	public void init() {
		this.entitypatch.initAnimator(this);
	}
	
	public final void playAnimation(int id, float convertTimeModifier) {
		this.playAnimation(AnimationManager.getInstance().byId(id), convertTimeModifier);
	}
	
	public final void playAnimationInstantly(int id) {
		this.playAnimationInstantly(AnimationManager.getInstance().byId(id));
	}
	
	public boolean isReverse() {
		return false;
	}
	
	public void playDeathAnimation() {
		this.playAnimation(this.livingAnimations.getOrDefault(LivingMotions.DEATH, Animations.BIPED_DEATH), 0);
	}
	
	public void addLivingAnimation(LivingMotion livingMotion, StaticAnimation animation) {
		this.livingAnimations.put(livingMotion, animation);
	}
	
	public StaticAnimation getLivingAnimation(LivingMotion livingMotion, StaticAnimation defaultGetter) {
		return this.livingAnimations.getOrDefault(livingMotion, defaultGetter);
	}
	
	public Map<LivingMotion, StaticAnimation> getLivingAnimations() {
		return ImmutableMap.copyOf(this.livingAnimations);
	}
	
	public void removeAnimationVariables(TypeKey<?> typeKey) {
		this.animationVariables.remove(typeKey);
	}
	
	public <T> void putAnimationVariable(TypeKey<T> typeKey, T value) {
		if (this.animationVariables.containsKey(typeKey)) {
			this.animationVariables.replace(typeKey, value);
		} else {
			this.animationVariables.put(typeKey, value);
		}
	}
	
	public <T> T getAnimationVariables(TypeKey<T> key) {
		return this.animationVariables.get(key);
	}
	
	public void resetLivingAnimations() {
		this.livingAnimations.clear();
	}
}