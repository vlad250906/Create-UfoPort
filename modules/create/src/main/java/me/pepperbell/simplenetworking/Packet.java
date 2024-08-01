package me.pepperbell.simplenetworking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface Packet {
	void encode(RegistryFriendlyByteBuf buf);
}
