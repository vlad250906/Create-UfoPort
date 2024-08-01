package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ConfigureSymmetryWandPacket extends SimplePacketBase {

	protected InteractionHand hand;
	protected SymmetryMirror mirror;

	public ConfigureSymmetryWandPacket(InteractionHand hand, SymmetryMirror mirror) {
		this.hand = hand;
		this.mirror = mirror;
	}

	public ConfigureSymmetryWandPacket(RegistryFriendlyByteBuf buffer) {
		hand = buffer.readEnum(InteractionHand.class);
		mirror = SymmetryMirror.fromNBT(buffer.readNbt());
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(hand);
		buffer.writeNbt(mirror.writeToNbt());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof SymmetryWandItem) {
				SymmetryWandItem.configureSettings(stack, mirror);
			}
		});
		return true;
	}

}
