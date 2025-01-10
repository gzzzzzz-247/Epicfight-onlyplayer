package yesman.epicfight.network.client;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPPlayAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class CPPlayAnimation {
	private final int animationId;
	private final float modifyTime;
	private boolean isClientSideAnimation;
	private final boolean resendToSender;
	
	public CPPlayAnimation() {
		this.animationId = 0;
		this.modifyTime = 0;
		this.resendToSender = false;
	}

	public CPPlayAnimation(StaticAnimation animation, float modifyTime, boolean clinetOnly, boolean resendToSender) {
		this(animation.getId(), modifyTime, clinetOnly, resendToSender);
	}

	public CPPlayAnimation(int animationId, float modifyTime, boolean clinetOnly, boolean resendToSender) {
		this.animationId = animationId;
		this.modifyTime = modifyTime;
		this.isClientSideAnimation = clinetOnly;
		this.resendToSender = resendToSender;
	}
	
	public static CPPlayAnimation fromBytes(FriendlyByteBuf buf) {
		return new CPPlayAnimation(buf.readInt(), buf.readFloat(), buf.readBoolean(), buf.readBoolean());
	}

	public static void toBytes(CPPlayAnimation msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.animationId);
		buf.writeFloat(msg.modifyTime);
		buf.writeBoolean(msg.isClientSideAnimation);
		buf.writeBoolean(msg.resendToSender);
	}
	
	public static void handle(CPPlayAnimation msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(()-> {
			ServerPlayer serverPlayer = ctx.get().getSender();
			ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(serverPlayer, ServerPlayerPatch.class);
			
			if (!msg.isClientSideAnimation) {
				playerpatch.getAnimator().playAnimation(msg.animationId, msg.modifyTime);
			}
			
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPPlayAnimation(msg.animationId, serverPlayer.getId(), msg.modifyTime), serverPlayer);
			
			if (msg.resendToSender) {
				EpicFightNetworkManager.sendToPlayer(new SPPlayAnimation(msg.animationId, serverPlayer.getId(), msg.modifyTime), serverPlayer);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}