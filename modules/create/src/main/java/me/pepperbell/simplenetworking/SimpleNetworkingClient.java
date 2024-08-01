package me.pepperbell.simplenetworking;

import com.simibubi.create.Create;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class SimpleNetworkingClient implements ClientModInitializer {
	
	private final static ResourceLocation GLOBAL_S2C = ResourceLocation.fromNamespaceAndPath("simple_networking", "global_type_s2c");

	private S2CHandler s2cHandler;
	
	@Override
	public void onInitializeClient() {
		
		initClientListener();
	}
	
	@Environment(EnvType.CLIENT)
	public void initClientListener() {
		s2cHandler = new S2CHandler();
		ClientPlayNetworking.registerGlobalReceiver(SimplePayloadS2C.PACKET_ID, s2cHandler);
	}
	
	@Environment(EnvType.CLIENT)
	private class S2CHandler implements ClientPlayNetworking.PlayPayloadHandler<SimplePayloadS2C> {

		@Override
		public void receive(SimplePayloadS2C payload, ClientPlayNetworking.Context context) {
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), Create.getRegistryAccess());
			buf.writeBytes(payload.data);
			buf.readerIndex(0);
			if(!SimpleNetworking.channels.containsKey(payload.chanel)) 
				throw new IllegalStateException("Invalid channel name in SimpleNetworking: "+payload.chanel+"; channels: "+SimpleNetworking.channels);
			SimpleNetworking.channels.get(payload.chanel).receiveS2C(buf, context);
		}
	}
	
	public record SimplePayloadS2C(ResourceLocation chanel, byte[] data) implements CustomPacketPayload{
		
		public static final Type<SimplePayloadS2C> PACKET_ID = new Type<>(GLOBAL_S2C);
	    public static final StreamCodec<RegistryFriendlyByteBuf, SimplePayloadS2C> STREAM_CODEC = StreamCodec.composite(
	    		ResourceLocation.STREAM_CODEC, SimplePayloadS2C::chanel,
	    		ByteBufCodecs.BYTE_ARRAY, SimplePayloadS2C::data, 
	    		SimplePayloadS2C::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return PACKET_ID;
		}
		
	}
	
}
