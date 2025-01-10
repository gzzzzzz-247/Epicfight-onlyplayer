package yesman.epicfight.client.world.capabilites.entitypatch.player;

import javax.annotation.Nonnull;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent;
import yesman.epicfight.api.client.forgeevent.UpdatePlayerMotionEvent;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;

@OnlyIn(Dist.CLIENT)
public class AbstractClientPlayerPatch<T extends AbstractClientPlayer> extends PlayerPatch<T> {
	private Item prevHeldItem;
	private Item prevHeldItemOffHand;
	
	@Override
	public void onJoinWorld(T entityIn, EntityJoinLevelEvent event) {
		super.onJoinWorld(entityIn, event);
		this.prevHeldItem = Items.AIR;
		this.prevHeldItemOffHand = Items.AIR;
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		if (this.original.getHealth() <= 0.0F) {
			currentLivingMotion = LivingMotions.DEATH;
		} else if (!this.state.updateLivingMotion() && considerInaction) {
			currentLivingMotion = LivingMotions.INACTION;
		} else {
			ClientAnimator animator = this.getClientAnimator();
			
			if (original.isFallFlying() || original.isAutoSpinAttack()) {
				currentLivingMotion = LivingMotions.FLY;
			} else if (original.getVehicle() != null) {
				if (original.getVehicle() instanceof PlayerRideableJumping)
					currentLivingMotion = LivingMotions.MOUNT;
				else
					currentLivingMotion = LivingMotions.SIT;
			} else if (original.isVisuallySwimming()) {
				currentLivingMotion = LivingMotions.SWIM;
			} else if (original.isSleeping()) {
				currentLivingMotion = LivingMotions.SLEEP;
			} else if (!original.onGround() && original.onClimbable()) {
				currentLivingMotion = LivingMotions.CLIMB;
				double y = original.yCloak - original.yCloakO;
				
				if (Math.abs(y) < 0.04D) {
					animator.baseLayer.pause();
				} else {
					animator.baseLayer.resume();

					animator.baseLayer.animationPlayer.setReversed(y < 0);
				}
			} else if (!original.getAbilities().flying) {
				if (original.isUnderWater() && (original.yCloak - original.yCloakO) < -0.005)
					currentLivingMotion = LivingMotions.FLOAT;
				else if (original.yCloak - original.yCloakO < -0.4F || this.isAirborneState())
					currentLivingMotion = LivingMotions.FALL;
				else if (this.isMoving()) {
					if (original.isCrouching())
						currentLivingMotion = LivingMotions.SNEAK;
					else if (original.isSprinting())
						currentLivingMotion = LivingMotions.RUN;
					else
						currentLivingMotion = LivingMotions.WALK;

					animator.baseLayer.animationPlayer.setReversed(this.dz < 0);
					
				} else {
					animator.baseLayer.animationPlayer.setReversed(false);
					
					if (original.isCrouching())
						currentLivingMotion = LivingMotions.KNEEL;
					else
						currentLivingMotion = LivingMotions.IDLE;
				}
			} else {
				if (this.isMoving())
					currentLivingMotion = LivingMotions.CREATIVE_FLY;
				else
					currentLivingMotion = LivingMotions.CREATIVE_IDLE;
			}
		}
		
		MinecraftForge.EVENT_BUS.post(new UpdatePlayerMotionEvent.BaseLayer(this, this.currentLivingMotion));
		CapabilityItem activeItemCap = this.getHoldingItemCapability(this.original.getUsedItemHand());
		
		if (this.original.isUsingItem()) {
			UseAnim useAnim = this.original.getUseItem().getUseAnimation();
			UseAnim capUseAnim = activeItemCap.getUseAnimation(this);
			
			if (useAnim == UseAnim.BLOCK || capUseAnim == UseAnim.BLOCK)
				if (activeItemCap.getWeaponCategory() == WeaponCategories.SHIELD)
					currentCompositeMotion = LivingMotions.BLOCK_SHIELD;
				else
					currentCompositeMotion = LivingMotions.BLOCK;
			else if (useAnim == UseAnim.BOW || useAnim == UseAnim.SPEAR)
				currentCompositeMotion = LivingMotions.AIM;
			else if (useAnim == UseAnim.CROSSBOW)
				currentCompositeMotion = LivingMotions.RELOAD;
			else if (useAnim == UseAnim.DRINK)
				currentCompositeMotion = LivingMotions.DRINK;
			else if (useAnim == UseAnim.EAT)
				currentCompositeMotion = LivingMotions.EAT;
			else if (useAnim == UseAnim.SPYGLASS)
				currentCompositeMotion = LivingMotions.SPECTATE;
			else
				currentCompositeMotion = currentLivingMotion;
		} else {
			if (this.original.getMainHandItem().getItem() instanceof ProjectileWeaponItem && CrossbowItem.isCharged(this.original.getMainHandItem()))
				currentCompositeMotion = LivingMotions.AIM;
			else if (this.getClientAnimator().getCompositeLayer(Layer.Priority.MIDDLE).animationPlayer.getAnimation().isReboundAnimation())
				currentCompositeMotion = LivingMotions.NONE;
			else if (this.original.swinging && this.original.getSleepingPos().isEmpty())
				currentCompositeMotion = LivingMotions.DIGGING;
			else
				currentCompositeMotion = currentLivingMotion;
			
			if (this.getClientAnimator().isAiming() && currentCompositeMotion != LivingMotions.AIM && activeItemCap instanceof RangedWeaponCapability) {
				this.playReboundAnimation();
			}
		}
		
		MinecraftForge.EVENT_BUS.post(new UpdatePlayerMotionEvent.CompositeLayer(this, this.currentCompositeMotion));
	}
	
	@Override
	protected void clientTick(LivingEvent.LivingTickEvent event) {
		super.clientTick(event);
		
		if (!this.getEntityState().updateLivingMotion()) {
			this.original.yBodyRot = this.original.getYRot();
		}
		
		boolean isMainHandChanged = this.prevHeldItem != this.original.getInventory().getSelected().getItem();
		boolean isOffHandChanged = this.prevHeldItemOffHand != this.original.getInventory().offhand.get(0).getItem();

		if (isMainHandChanged || isOffHandChanged) {
			this.updateHeldItem(this.getHoldingItemCapability(InteractionHand.MAIN_HAND), this.getHoldingItemCapability(InteractionHand.OFF_HAND));
			
			if (isMainHandChanged) {
				this.prevHeldItem = this.original.getInventory().getSelected().getItem();
			}
			
			if (isOffHandChanged) {
				this.prevHeldItemOffHand = this.original.getInventory().offhand.get(0).getItem();
			}
		}
		
		/** {@link LivingDeathEvent} never fired for client players **/
		if (this.original.deathTime == 1) {
			this.getClientAnimator().playDeathAnimation();
		}
	}
	
	protected boolean isMoving() {
		return Math.abs(this.dx) > 0.01F || Math.abs(this.dz) > 0.01F;
	}
	
	public void updateHeldItem(CapabilityItem mainHandCap, CapabilityItem offHandCap) {
		this.cancelAnyAction();
	}
	
	@Override
	public void reserveAnimation(StaticAnimation animation) {
		this.animator.reserveAnimation(animation);
	}
	
	@Override
	public void playAnimationSynchronized(StaticAnimation animation, float convertTimeModifier, AnimationPacketProvider packetProvider) {
	}
	
	@Override
	public boolean overrideRender() {
		boolean originalShouldRender = this.isBattleMode() || !EpicFightMod.CLIENT_CONFIGS.filterAnimation.getValue();
		
		RenderEpicFightPlayerEvent renderepicfightplayerevent = new RenderEpicFightPlayerEvent(this, originalShouldRender);
		MinecraftForge.EVENT_BUS.post(renderepicfightplayerevent);
		
		return renderepicfightplayerevent.getShouldRender();
	}
	
	@Override
	public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
		return false;
	}
	
	@Override
	public void poseTick(DynamicAnimation animation, Pose pose, float elapsedTime, float partialTicks) {
		if (pose.getJointTransformData().containsKey("Head")) {
			if (animation.doesHeadRotFollowEntityHead()) {
				float headRotO = this.modelYRotO - this.original.yHeadRotO;
				float headRot = this.modelYRot - this.original.yHeadRot;
				float partialHeadRot = MathUtils.lerpBetween(headRotO, headRot, partialTicks);
				OpenMatrix4f toOriginalRotation = this.armature.getBindedTransformFor(pose, this.armature.searchJointByName("Head")).removeScale().removeTranslation().invert();
				Vec3f xAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.X_AXIS, null);
				Vec3f yAxis = OpenMatrix4f.transform3v(toOriginalRotation, Vec3f.Y_AXIS, null);
				OpenMatrix4f headRotation = OpenMatrix4f.createRotatorDeg(-this.original.getXRot(), xAxis).mulFront(OpenMatrix4f.createRotatorDeg(partialHeadRot, yAxis));
				pose.getOrDefaultTransform("Head").frontResult(JointTransform.fromMatrix(headRotation), OpenMatrix4f::mul);
			}
		}
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTick) {
		Direction direction;
		
		if (this.original.isAutoSpinAttack()) {
			OpenMatrix4f mat = MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
			float yRot = MathUtils.lerpBetween(this.original.yRotO, this.original.getYRot(), partialTick);
			float xRot = MathUtils.lerpBetween(this.original.xRotO, this.original.getXRot(), partialTick);
			
			mat.rotateDeg(-yRot, Vec3f.Y_AXIS)
			   .rotateDeg(-xRot, Vec3f.X_AXIS)
			   .rotateDeg((this.original.tickCount + partialTick) * -55.0F, Vec3f.Z_AXIS)
			   .translate(0F, -0.39F, 0F);
			
			return mat;
		} else if (this.original.isFallFlying()) {
			OpenMatrix4f mat = MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            float f1 = (float)this.original.getFallFlyingTicks() + partialTick;
            float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
            
            mat.rotateDeg(-Mth.rotLerp(partialTick, this.original.yBodyRotO, this.original.yBodyRot), Vec3f.Y_AXIS).rotateDeg(f2 * (-this.original.getXRot()), Vec3f.X_AXIS);
            
            Vec3 vec3d = this.original.getViewVector(partialTick);
            Vec3 vec3d1 = this.original.getDeltaMovement();
            double d0 = vec3d1.horizontalDistanceSqr();
            double d1 = vec3d.horizontalDistanceSqr();
            
			if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (vec3d1.x * vec3d.x + vec3d1.z * vec3d.z) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = vec3d1.x * vec3d.z - vec3d1.z * vec3d.x;
                mat.rotate((float)-((Math.signum(d3) * Math.acos(d2))), Vec3f.Z_AXIS);
            }
			
			return mat;
		} else if (this.original.isSleeping()) {
			BlockState blockstate = this.original.getFeetBlockState();
			float yRot = 0.0F;
			
			if (blockstate.isBed(this.original.level(), this.original.getSleepingPos().orElse(null), this.original)) {
				if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            		switch(blockstate.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            		case EAST:
        				yRot = 90.0F;
        				break;
        			case WEST:
        				yRot = -90.0F;
        				break;
        			case SOUTH:
        				yRot = 180.0F;
        				break;
        			default:
        				break;
            		}
            	}
			}
			
			return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, yRot, yRot, 0, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
		} else if ((direction = this.getLadderDirection(this.original.getFeetBlockState(), this.original.level(), this.original.blockPosition(), this.original)) != Direction.UP) {
			float yRot = 0.0F;
			
			switch(direction) {
			case EAST:
				yRot = 90.0F;
				break;
			case WEST:
				yRot = -90.0F;
				break;
			case SOUTH:
				yRot = 180.0F;
				break;
			default:
				break;
			}
			
			return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, yRot, yRot, 0.0F, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
		} else {
			float yRotO;
			float yRot;
			float xRotO = 0;
			float xRot = 0;
			
			if (this.original.getVehicle() instanceof LivingEntity ridingEntity) {
				yRotO = ridingEntity.yBodyRotO;
				yRot = ridingEntity.yBodyRot;
			} else {
				yRotO = this.modelYRotO;
				yRot = this.modelYRot;
			}
			
			if (!this.getEntityState().inaction() && this.original.getPose() == net.minecraft.world.entity.Pose.SWIMMING) {
				float f = this.original.getSwimAmount(partialTick);
				float f3 = this.original.isInWater() ? this.original.getXRot() : 0;
		        float f4 = Mth.lerp(f, 0.0F, f3);
		        xRotO = f4;
		        xRot = f4;
			}
			
			return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, xRotO, xRot, yRotO, yRot, partialTick, PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
		}
	}
	
	public Direction getLadderDirection(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull LivingEntity entity) {
		boolean isSpectator = (entity instanceof Player && entity.isSpectator());
        if (isSpectator || this.original.onGround() || !this.original.isAlive()) {
        	return Direction.UP;
        }
        
		if (ForgeConfig.SERVER.fullBoundingBoxLadders.get()) {
            if (state.isLadder(world, pos, entity)) {
            	if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            		return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            	}
            	
            	if (state.hasProperty(BlockStateProperties.UP) && state.getValue(BlockStateProperties.UP)) {
            		return Direction.UP;
            	} else if (state.hasProperty(BlockStateProperties.NORTH) && state.getValue(BlockStateProperties.NORTH)) {
            		return Direction.SOUTH;
            	} else if (state.hasProperty(BlockStateProperties.WEST) && state.getValue(BlockStateProperties.WEST)) {
            		return Direction.EAST;
            	} else if (state.hasProperty(BlockStateProperties.SOUTH) && state.getValue(BlockStateProperties.SOUTH)) {
            		return Direction.NORTH;
            	} else if (state.hasProperty(BlockStateProperties.EAST) && state.getValue(BlockStateProperties.EAST)) {
            		return Direction.WEST;
            	}
            }
		} else {
            AABB bb = entity.getBoundingBox();
            int mX = Mth.floor(bb.minX);
            int mY = Mth.floor(bb.minY);
            int mZ = Mth.floor(bb.minZ);
            
			for (int y2 = mY; y2 < bb.maxY; y2++) {
				for (int x2 = mX; x2 < bb.maxX; x2++) {
					for (int z2 = mZ; z2 < bb.maxZ; z2++) {
                        BlockPos tmp = new BlockPos(x2, y2, z2);
                        state = world.getBlockState(tmp);
						if (state.isLadder(world, tmp, entity)) {
							if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			            		return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			            	}
			            	if (state.hasProperty(BlockStateProperties.UP) && state.getValue(BlockStateProperties.UP)) {
			            		return Direction.UP;
			            	} else if (state.hasProperty(BlockStateProperties.NORTH) && state.getValue(BlockStateProperties.NORTH)) {
			            		return Direction.SOUTH;
			            	} else if (state.hasProperty(BlockStateProperties.WEST) && state.getValue(BlockStateProperties.WEST)) {
			            		return Direction.EAST;
			            	} else if (state.hasProperty(BlockStateProperties.SOUTH) && state.getValue(BlockStateProperties.SOUTH)) {
			            		return Direction.NORTH;
			            	} else if (state.hasProperty(BlockStateProperties.EAST) && state.getValue(BlockStateProperties.EAST)) {
			            		return Direction.WEST;
			            	}
                        }
                    }
                }
            }
        }
		
		return Direction.UP;
	}
}