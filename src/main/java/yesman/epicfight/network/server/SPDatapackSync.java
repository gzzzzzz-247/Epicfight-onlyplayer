package yesman.epicfight.network.server;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.exception.DatapackException;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

public class SPDatapackSync {
	protected int count;
	protected int index;
	protected SPDatapackSync.Type type;
	protected CompoundTag[] tags;

	public SPDatapackSync() {
		this(0, SPDatapackSync.Type.WEAPON);
	}

	public SPDatapackSync(int count, SPDatapackSync.Type type) {
		this.count = count;
		this.index = 0;
		this.type = type;
		this.tags = new CompoundTag[count];
	}

	public void write(CompoundTag tag) {
		this.tags[this.index] = tag;
		this.index++;
	}

	public CompoundTag[] getTags() {
		return this.tags;
	}

	public SPDatapackSync.Type getType() {
		return this.type;
	}

	public static SPDatapackSync fromBytes(FriendlyByteBuf buf) {
		SPDatapackSync msg = new SPDatapackSync(buf.readInt(), SPDatapackSync.Type.values()[buf.readInt()]);

		for (int i = 0; i < msg.count; i++) {
			msg.tags[i] = buf.readNbt();
		}

		return msg;
	}

	public static void toBytes(SPDatapackSync msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.count);
		buf.writeInt(msg.type.ordinal());

		for (CompoundTag tag : msg.tags) {
			buf.writeNbt(tag);
		}
	}

	public static void handle(SPDatapackSync msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			try {
				switch (msg.getType()) {
					case SKILL_PARAMS -> {
						/** Processed on {@link SPDatapackSyncSkill} **/
					}
					case WEAPON -> ItemCapabilityReloadListener.processServerPacket(msg);
					case ARMOR -> ItemCapabilityReloadListener.processServerPacket(msg);
					case WEAPON_TYPE -> WeaponTypeReloadListener.processServerPacket(msg);
					case ANIMATION_IN_MANDATORY_RESOURCE_PACK, ANIMATION_IN_RESOURCE_PACK ->
						AnimationManager.getInstance().processServerPacket(msg,
								msg.getType() == Type.ANIMATION_IN_MANDATORY_RESOURCE_PACK);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new DatapackException(e.getMessage());
			}
		});

		ctx.get().setPacketHandled(true);
	}

	public enum Type {
		ARMOR, WEAPON, MOB, SKILL_PARAMS, WEAPON_TYPE, ANIMATION_IN_MANDATORY_RESOURCE_PACK, ANIMATION_IN_RESOURCE_PACK
	}
}