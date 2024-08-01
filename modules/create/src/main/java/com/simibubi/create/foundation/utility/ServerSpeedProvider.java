package com.simibubi.create.foundation.utility;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

public class ServerSpeedProvider {

	static int clientTimer = 0;
	static int serverTimer = 0;
	static boolean initialized = false;
	static LerpedFloat modifier = LerpedFloat.linear();

	public static void serverTick(MinecraftServer server) {
		serverTimer++;
		if (serverTimer > getSyncInterval()) {
			AllPackets.getChannel().sendToClients(new Packet(), server.getPlayerList().getPlayers());
			serverTimer = 0;
		}
	}

	@Environment(EnvType.CLIENT)
	public static void clientTick() {
		if (Minecraft.getInstance()
			.hasSingleplayerServer()
			&& Minecraft.getInstance()
				.isPaused())
			return;
		modifier.tickChaser();
		clientTimer++;
	}

	public static Integer getSyncInterval() {
		return AllConfigs.server().tickrateSyncTimer.get();
	}

	public static float get() {
		return modifier.getValue();
	}

	public static class Packet extends SimplePacketBase {

		public Packet() {}

		public Packet(RegistryFriendlyByteBuf buffer) {}

		@Override
		public void write(RegistryFriendlyByteBuf buffer) {}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> {
				if (!initialized) {
					initialized = true;
					clientTimer = 0;
					return;
				}
				float target = ((float) getSyncInterval()) / Math.max(clientTimer, 1);
				modifier.chase(Math.min(target, 1), .25, Chaser.EXP);
				// Set this to -1 because packets are processed before ticks.
				// ServerSpeedProvider#clientTick will increment it to 0 at the end of this tick.
				// Setting it to 0 causes consistent desync, as the client ends up counting too many ticks.
				clientTimer = -1;
			});
			return true;
		}

	}

}
