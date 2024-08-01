package me.pepperbell.simplenetworking;

import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.simibubi.create.Create;

import io.netty.buffer.Unpooled;
import me.pepperbell.simplenetworking.SimpleNetworkingClient.SimplePayloadS2C;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public class SimpleNetworking implements ModInitializer {
	
	private static MinecraftServer currentServer;
	private final static ResourceLocation GLOBAL_C2S = ResourceLocation.fromNamespaceAndPath("simple_networking", "global_type_c2s");
	public static BiMap<ResourceLocation, SimpleChannel> channels = HashBiMap.create();
	
	private C2SHandler c2sHandler;
	

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(SimplePayloadC2S.PACKET_ID, SimplePayloadC2S.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SimplePayloadS2C.PACKET_ID, SimplePayloadS2C.STREAM_CODEC);
		
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			currentServer = server;
		});
		
		initServerListener();
	}
	
	public static void addChannel(SimpleChannel channel) {
		channels.put(channel.getChannelName(), channel);
	}

	public static MinecraftServer getCurrentServer() {
		return currentServer;
	}
	
	public void initServerListener() {
		c2sHandler = new C2SHandler();
		ServerPlayNetworking.registerGlobalReceiver(SimplePayloadC2S.PACKET_ID, c2sHandler);
	}
	
	private class C2SHandler implements ServerPlayNetworking.PlayPayloadHandler<SimplePayloadC2S> {

		@Override
		public void receive(SimplePayloadC2S payload, ServerPlayNetworking.Context context) {
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), Create.getRegistryAccess());
			buf.writeBytes(payload.data);
			buf.readerIndex(0);
			if(!channels.containsKey(payload.chanel)) throw new IllegalStateException("Invalid channel name in SimpleNetworking: "+payload.chanel);
			channels.get(payload.chanel).receiveC2S(buf, context);
		}
	}
	
	
	public record SimplePayloadC2S(ResourceLocation chanel, byte[] data) implements CustomPacketPayload{
		
		public static final Type<SimplePayloadC2S> PACKET_ID = new Type<>(GLOBAL_C2S);
	    public static final StreamCodec<RegistryFriendlyByteBuf, SimplePayloadC2S> STREAM_CODEC = StreamCodec.composite(
	    		ResourceLocation.STREAM_CODEC, SimplePayloadC2S::chanel,
	    		ByteBufCodecs.BYTE_ARRAY, SimplePayloadC2S::data, 
	    		SimplePayloadC2S::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return PACKET_ID;
		}
		
	}
	
	
	
	
}
