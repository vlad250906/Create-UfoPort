package com.simibubi.create.infrastructure.command;

import com.simibubi.create.AllPackets;

import net.minecraft.server.level.ServerPlayer;

public class ToggleDebugCommand extends ConfigureConfigCommand {

	public ToggleDebugCommand() {
		super("rainbowDebug");
	}

	@Override
	protected void sendPacket(ServerPlayer player, String option) {
		AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.rainbowDebug.name(), option), player);
	}
}
