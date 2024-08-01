package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class UpdateSymmetryWandPacket extends SimplePacketBase {

	protected InteractionHand hand;
	protected SymmetryMirror mirror;

	public UpdateSymmetryWandPacket(InteractionHand hand, SymmetryMirror mirror) {
		this.hand = hand;
		this.mirror = mirror;
	}

	public UpdateSymmetryWandPacket(RegistryFriendlyByteBuf buffer) {
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
			Player player = context.sender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof SymmetryWandItem) {
				ItemHelper.getOrCreateComponent(stack, AllDataComponents.SYM_WAND, new CompoundTag())
					.put(SymmetryWandItem.SYMMETRY, mirror.writeToNbt());
				ItemHelper.getOrCreateComponent(stack, AllDataComponents.SYM_WAND, new CompoundTag())
					.putBoolean(SymmetryWandItem.ENABLE, true);
			}
		});
		return true;
	}

}
