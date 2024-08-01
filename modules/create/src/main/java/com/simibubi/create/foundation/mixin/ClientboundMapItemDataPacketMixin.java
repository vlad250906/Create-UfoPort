package com.simibubi.create.foundation.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.station.StationMarker;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

// random priority to prevent networking conflicts
@Mixin(value = ClientboundMapItemDataPacket.class, priority = 426)
public class ClientboundMapItemDataPacketMixin {
	@Shadow
	@Final
	private Optional<List<MapDecoration>> decorations;

	@Unique
	private int[] create$stationIndices;

	@Inject(method = "<init>(Lnet/minecraft/world/level/saveddata/maps/MapId;BZLjava/util/Collection;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData$MapPatch;)V", at = @At("RETURN"))
	private void create$onInit(MapId mapId, byte scale, boolean locked, @Nullable Collection<MapDecoration> decorations, @Nullable MapItemSavedData.MapPatch colorPatch, CallbackInfo ci) {
		create$stationIndices = create$getStationIndices(this.decorations.isEmpty() ? null : this.decorations.get());
	}

	@Unique
	private static int[] create$getStationIndices(List<MapDecoration> decorations) {
		if (decorations == null) {
			return new int[0];
		}

		IntList indices = new IntArrayList();
		for (int i = 0; i < decorations.size(); i++) {
			MapDecoration decoration = decorations.get(i);
			if (decoration instanceof StationMarker.Decoration) {
				indices.add(i);
			}
		}
		return indices.toIntArray();
	}
	
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void create$clinit(CallbackInfo ci) {
	    
		 StreamCodec<ByteBuf, int[]> INT_ARRAY = new StreamCodec<ByteBuf, int[]>(){

		        @Override
		        public int[] decode(ByteBuf buffer) {
		        	 int i = VarInt.read(buffer);
		             if (i > buffer.readableBytes()) {
		                 throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + buffer.readableBytes());
		             }
		             int[] bs = new int[i];
		             for(int j=0;j<i;j++) {
		            	 bs[j] = VarInt.read(buffer);
		             }
		             return bs;
		        }

		        @Override
		        public void encode(ByteBuf buffer, int[] value) {
		        	VarInt.write(buffer, value.length);
		        	for(int i=0;i<value.length;i++) {
		        		VarInt.write(buffer, value[i]);
		        	}
		        }
		};
		
		StreamCodec<RegistryFriendlyByteBuf, ClientboundMapItemDataPacket> codec = StreamCodec.composite(
				INT_ARRAY, obj -> ((ClientboundMapItemDataPacketMixin)(Object)obj).create$stationIndices,
				MapId.STREAM_CODEC, ClientboundMapItemDataPacket::mapId, 
				ByteBufCodecs.BYTE, ClientboundMapItemDataPacket::scale, 
				ByteBufCodecs.BOOL, ClientboundMapItemDataPacket::locked, 
				MapDecoration.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs::optional), ClientboundMapItemDataPacket::decorations,
				MapItemSavedData.MapPatch.STREAM_CODEC, ClientboundMapItemDataPacket::colorPatch, 
				(a1, a2, a3, a4, a5, a6) -> {
					ClientboundMapItemDataPacket pack = new ClientboundMapItemDataPacket(a2, a3, a4, a5, a6);
					((ClientboundMapItemDataPacketMixin)(Object)pack).create$stationIndices = a1;
					return pack;
				}
		);
		
		ClientboundMapItemDataPacket.STREAM_CODEC = codec;
		
	}

//	@Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
//	private void create$onInit(FriendlyByteBuf buf, CallbackInfo ci) {
//		create$stationIndices = buf.readVarIntArray();
//
//		if (decorations.isPresent()) {
//			for (int i : create$stationIndices) {
//				if (i >= 0 && i < decorations.get().size()) {
//					MapDecoration decoration = decorations.get().get(i);
//					decorations.get().set(i, StationMarker.Decoration.from(decoration));
//				}
//			}
//		}
//	}
//
//	@Inject(method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
//	private void create$onWrite(FriendlyByteBuf buf, CallbackInfo ci) {
//		buf.writeVarIntArray(create$stationIndices);
//	}
}
