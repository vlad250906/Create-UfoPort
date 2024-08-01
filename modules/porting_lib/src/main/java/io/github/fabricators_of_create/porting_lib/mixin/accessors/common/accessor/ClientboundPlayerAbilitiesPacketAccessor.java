package io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;

@Mixin(ClientboundPlayerAbilitiesPacket.class)
public interface ClientboundPlayerAbilitiesPacketAccessor {
	@Accessor("flyingSpeed")
	void port_lib$setFlyingSpeed(float speed);
}
