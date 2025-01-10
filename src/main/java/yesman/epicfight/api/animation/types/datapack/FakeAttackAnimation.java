package yesman.epicfight.api.animation.types.datapack;

import java.util.Locale;
import java.util.NoSuchElementException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class FakeAttackAnimation extends AttackAnimation implements ClipHoldingAnimation {
	private static Phase[] convertListTagToPhases(ListTag listTag, Armature armature) {
		float start = 0.0F;
		Phase[] phases = new Phase[listTag.size()];
		
		int i = 0;
		
		for (Tag phaseTag : listTag) {
			CompoundTag phaseCompTag = (CompoundTag)phaseTag;
			
			if (!phaseCompTag.contains("antic")) {
				throw new NoSuchElementException("Phase" + i + ": Antic not specified");
			}
			
			if (!phaseCompTag.contains("preDelay")) {
				throw new NoSuchElementException("Phase" + i + ": Pre-Delay not specified");
			}
			
			if (!phaseCompTag.contains("contact")) {
				throw new NoSuchElementException("Phase" + i + ": Contact not specified");
			}
			
			if (!phaseCompTag.contains("recovery")) {
				throw new NoSuchElementException("Phase" + i + ": Recovery not specified");
			}
			
			if (!phaseCompTag.contains("hand")) {
				throw new NoSuchElementException("Phase" + i + ": Hand not specified");
			}
			
			if (!phaseCompTag.contains("joint")) {
				throw new NoSuchElementException("Phase" + i + ": Joint not specified");
			}
			
			float antic = phaseCompTag.getFloat("antic");
			float preDelay = phaseCompTag.getFloat("preDelay");
			float contact = phaseCompTag.getFloat("contact");
			float recovery = phaseCompTag.getFloat("recovery");
			InteractionHand hand = InteractionHand.valueOf(phaseCompTag.getString("hand").toUpperCase(Locale.ROOT));
			
			String armature$joint = phaseCompTag.getString("joint");
			String joinName = armature$joint.substring(armature$joint.lastIndexOf('.') + 1);
			Joint joint =  armature.searchJointByName(joinName);
			Collider collider = null;
			
			try {
				collider = ColliderPreset.deserializeSimpleCollider(phaseCompTag.getCompound("collider"));
			} catch (Exception e) {
			}
			
			phases[i] = new Phase(start, antic, preDelay, contact, recovery, recovery, hand, joint, collider);
			start = recovery;
			i++;
		}
		
		return phases;
	}
	
	protected AnimationClip clip;
	protected FakeAnimation fakeAnimation;
	
	public FakeAttackAnimation(float convertTime, String path, Armature armature, ListTag phases) {
		super(convertTime, path, armature, true, convertListTagToPhases(phases, armature));
	}
	
	public FakeAttackAnimation(float convertTime, String path, Armature armature, Phase... phases) {
		super(convertTime, path, armature, true, phases);
	}
	
	@Override
	public void setCreator(FakeAnimation fakeAnimation) {
		this.fakeAnimation = fakeAnimation;
	}
	
	@Override
	public FakeAnimation getCreator() {
		return this.fakeAnimation;
	}
	
	@Override
	public void setAnimationClip(AnimationClip clip) {
		this.clip = clip;
	}
	
	@Override
	public AnimationClip getAnimationClip() {
		return this.clip;
	}
	
	@Override
	public void putOnPlayer(AnimationPlayer animationPlayer, LivingEntityPatch<?> entitypatch) {
		animationPlayer.setPlayAnimation(this);
		animationPlayer.tick(entitypatch);
	}
	
	@Override
	public FakeAnimation buildAnimation(JsonArray rawAnimationJson) {
		FakeAnimation fakeAnimation = new FakeAnimation(this.registryName.toString(), this.armature, this.clip, rawAnimationJson);
		fakeAnimation.setAnimationClass(FakeAnimation.AnimationType.ATTACK);
		fakeAnimation.setParameter("convertTime", this.convertTime);
		fakeAnimation.setParameter("path", this.registryName.toString());
		fakeAnimation.setParameter("armature", this.armature);
		
		ListTag listTag = new ListTag();
		
		for (AttackAnimation.Phase phase : this.phases) {
			CompoundTag compTag = new CompoundTag();
			
			compTag.putFloat("start", phase.start);
			compTag.putFloat("antic", phase.antic);
			compTag.putFloat("preDelay", phase.preDelay);
			compTag.putFloat("contact", phase.contact);
			compTag.putFloat("recovery", phase.recovery);
			compTag.putFloat("end", phase.end);
			compTag.putString("hand", phase.hand.toString());
			
			if (phase.colliders[0].getSecond() != null) {
				compTag.put("collider", phase.colliders[0].getSecond().serialize(new CompoundTag()));
			}
			
			compTag.putString("joint", this.armature.toString() +"."+ phase.colliders[0].getFirst().getName());
			
			listTag.add(compTag);
		}
		
		fakeAnimation.setParameter("phases", listTag);
		
		this.getProperty(ClientAnimationProperties.TRAIL_EFFECT).ifPresent((trailInfos) -> {
			JsonArray trailArray = new JsonArray();
			
			for (TrailInfo trailInfo : trailInfos) {
				JsonObject trailObj = new JsonObject();
				trailObj.addProperty("start_time", trailInfo.startTime);
				trailObj.addProperty("end_time", trailInfo.endTime);
				trailObj.addProperty("joint", trailInfo.joint);
				trailObj.addProperty("item_skin_hand", trailInfo.hand.toString());
				trailArray.add(trailObj);
			}
			
			fakeAnimation.getPropertiesJson().add("trail_effects", trailArray);
			fakeAnimation.addProperty(ClientAnimationProperties.TRAIL_EFFECT, trailInfos);
		});
		
		this.fakeAnimation = fakeAnimation;
		
		return fakeAnimation;
	}
}
