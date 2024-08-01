package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class PlaceExtendedCurvePacket extends SimplePacketBase {

	boolean mainHand;
	boolean ctrlDown;

	public PlaceExtendedCurvePacket(boolean mainHand, boolean ctrlDown) {
		this.mainHand = mainHand;
		this.ctrlDown = ctrlDown;
	}

	public PlaceExtendedCurvePacket(RegistryFriendlyByteBuf buffer) {
		mainHand = buffer.readBoolean();
		ctrlDown = buffer.readBoolean();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBoolean(mainHand);
		buffer.writeBoolean(ctrlDown);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			ItemStack stack = sender.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
			if (!AllTags.AllBlockTags.TRACKS.matches(stack) || !stack.has(AllDataComponents.TRACK_ITEM))
				return;
			CompoundTag tag = stack.get(AllDataComponents.TRACK_ITEM);
			tag.putBoolean("ExtendCurve", true);
		});
		return true;
	}

}
