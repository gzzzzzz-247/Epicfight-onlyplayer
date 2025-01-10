package yesman.epicfight.api.animation.types.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.model.Armature;

@OnlyIn(Dist.CLIENT)
public class FakeDashAttackAnimation extends FakeAttackAnimation {
	public FakeDashAttackAnimation(float convertTime, String path, Armature armature, ListTag phases) {
		super(convertTime, path, armature, phases);
	}
	
	public FakeDashAttackAnimation(float convertTime, String path, Armature armature, Phase... phases) {
		super(convertTime, path, armature, phases);
	}
	
	@Override
	public FakeAnimation buildAnimation(JsonArray rawAnimationJson) {
		FakeAnimation fakeAnimation = new FakeAnimation(this.registryName.toString(), this.armature, this.clip, rawAnimationJson);
		fakeAnimation.setAnimationClass(FakeAnimation.AnimationType.DASH_ATTACK);
		fakeAnimation.setParameter("convertTime", this.convertTime);
		fakeAnimation.setParameter("path", this.registryName.toString());
		fakeAnimation.setParameter("armature", this.armature);
		
		ListTag listTag = new ListTag();
		
		for (AttackAnimation.Phase phase : this.phases) {
			CompoundTag compTag = new CompoundTag();
			
			compTag.putFloat("antic", phase.antic);
			compTag.putFloat("preDelay", phase.preDelay);
			compTag.putFloat("contact", phase.contact);
			compTag.putFloat("recovery", phase.recovery);
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
