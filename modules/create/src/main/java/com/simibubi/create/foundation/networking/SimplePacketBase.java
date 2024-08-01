package com.simibubi.create.foundation.networking;

import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.simplenetworking.C2SPacket;
import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class SimplePacketBase implements C2SPacket, S2CPacket {

	public abstract void write(RegistryFriendlyByteBuf buffer);

	public abstract boolean handle(Context context);

	@Override
	public final void encode(RegistryFriendlyByteBuf buffer) {
		write(buffer);
	}
	
	@Environment(value=EnvType.CLIENT)
	@Override
	public void handle(Minecraft client, PacketSender responseSender, LocalPlayer player, SimpleChannel channel) {
		handle(new Context(client, player));
	}

	@Override
	public void handle(MinecraftServer server, ServerPlayer player, PacketSender responseSender, SimpleChannel channel) {
		handle(new Context(server, player));
	}

	public enum NetworkDirection {
		PLAY_TO_CLIENT,
		PLAY_TO_SERVER
	}

	public record Context(Executor exec, @Nullable Player sender) {
		public void enqueueWork(Runnable runnable) {
			exec().execute(runnable);
		}

		@Nullable
		public ServerPlayer getSender() {
			return (ServerPlayer)sender();
		}

		public NetworkDirection getDirection() {
			return sender() == null ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
		}
	}
}
