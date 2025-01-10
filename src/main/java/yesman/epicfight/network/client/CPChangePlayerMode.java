package yesman.epicfight.network.client;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPModifyPlayerData;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class CPChangePlayerMode {
	private final PlayerPatch.PlayerMode mode;
	
	public CPChangePlayerMode(PlayerPatch.PlayerMode mode) {
		this.mode = mode;
	}

	public static CPChangePlayerMode fromBytes(FriendlyByteBuf buf) {
		return new CPChangePlayerMode(buf.readEnum(PlayerPatch.PlayerMode.class));
	}

	public static void toBytes(CPChangePlayerMode msg, FriendlyByteBuf buf) {
		buf.writeEnum(msg.mode);
	}
	
	public static void handle(CPChangePlayerMode msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			
			if (player != null) {
				ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
				
				if (playerpatch != null) {
					playerpatch.toMode(msg.mode, false);
					EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(SPModifyPlayerData.setPlayerMode(player.getId(), playerpatch.getPlayerMode()), player);
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}