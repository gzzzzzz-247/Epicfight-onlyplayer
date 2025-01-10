package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;

@Mixin(value = EndCrystal.class)
public abstract class MixinEndCrystal {
	/*
	 * @Inject(at = @At(value = "HEAD"), method =
	 * "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", cancellable =
	 * true)
	 * private void epicfight_hurt(DamageSource damagesource, float amount,
	 * CallbackInfoReturnable<Boolean> info) {
	 * EndCrystal self = (EndCrystal)((Object)this);
	 * 
	 * if (!self.level().isClientSide()) {
	 * EnderDragonPatch dragonpatch = EnderDragonPatch.INSTANCE_SERVER;
	 * 
	 * if (dragonpatch != null) {
	 * DragonPhaseInstance currentPhase =
	 * dragonpatch.getOriginal().getPhaseManager().getCurrentPhase();
	 * 
	 * if (currentPhase.getPhase() == PatchedPhases.CRYSTAL_LINK) {
	 * DragonCrystalLinkPhase phase = (DragonCrystalLinkPhase)currentPhase;
	 * 
	 * if (phase.getLinkingCrystal() == self) {
	 * info.cancel();
	 * info.setReturnValue(false);
	 * }
	 * }
	 * }
	 * }
	 * }
	 */
}