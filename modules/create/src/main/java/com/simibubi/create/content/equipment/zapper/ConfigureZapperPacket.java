package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public abstract class ConfigureZapperPacket extends SimplePacketBase {

	protected InteractionHand hand;
	protected PlacementPatterns pattern;

	public ConfigureZapperPacket(InteractionHand hand, PlacementPatterns pattern) {
		this.hand = hand;
		this.pattern = pattern;
	}

	public ConfigureZapperPacket(RegistryFriendlyByteBuf buffer) {
		hand = buffer.readEnum(InteractionHand.class);
		pattern = buffer.readEnum(PlacementPatterns.class);
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeEnum(hand);
		buffer.writeEnum(pattern);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof ZapperItem) {
				configureZapper(stack);
			}
		});
		return true;
	}

	public abstract void configureZapper(ItemStack stack);

}
