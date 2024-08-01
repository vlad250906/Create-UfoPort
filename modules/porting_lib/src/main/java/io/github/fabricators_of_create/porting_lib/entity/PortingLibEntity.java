package io.github.fabricators_of_create.porting_lib.entity;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingAttackEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

public class PortingLibEntity implements ModInitializer {
	public static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ExtraDataPacket.PACKET_ID, ExtraDataPacket.STREAM_CODEC);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof MultiPartEntity partEntity && partEntity.isMultipartEntity()) {
				PartEntity<?>[] parts = partEntity.getParts();
				if (parts != null) {
					for (PartEntity<?> part : parts) {
						world.getPartEntityMap().put(part.getId(), part);
					}
				}
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
			if (entity instanceof MultiPartEntity partEntity && partEntity.isMultipartEntity()) {
				PartEntity<?>[] parts = partEntity.getParts();
				if (parts != null) {
					for (PartEntity<?> part : parts) {
						world.getPartEntityMap().remove(part.getId());
					}
				}
			}
		});
		ServerEntityEvents.EQUIPMENT_CHANGE.register((livingEntity, equipmentSlot, previousStack, currentStack) -> {
			LivingEntityEvents.EQUIPMENT_CHANGE.invoker().onEquipmentChange(livingEntity, equipmentSlot, previousStack, currentStack);
		});
		LivingEntityEvents.LivingJumpEvent.JUMP.register(event -> {
			LivingEntityEvents.JUMP.invoker().onLivingEntityJump(event.getEntity());
		});
		LivingEntityEvents.LivingVisibilityEvent.VISIBILITY.register(event -> {
			LivingEntityEvents.VISIBILITY.invoker().getEntityVisibilityMultiplier(event.getEntity(), event.getLookingEntity(), event.getVisibilityModifier());
		});
		LivingEntityEvents.LivingTickEvent.TICK.register(event -> {
			if (!event.isCanceled())
				LivingEntityEvents.TICK.invoker().onLivingEntityTick(event.getEntity());
		});
		LivingAttackEvent.ATTACK.register(event -> {
			LivingEntityEvents.ATTACK.invoker().onAttack(event.getEntity(), event.getSource(), event.getAmount());
		});
	}

	public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity, ServerEntity server) {
		return getEntitySpawningPacket(entity, new ClientboundAddEntityPacket(entity, server));
	}

	@ApiStatus.Internal
	public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity, Packet<ClientGamePacketListener> base) {
		if (entity instanceof IEntityAdditionalSpawnData extra) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(entity.getId());
			extra.writeSpawnData(buf);
			Packet<ClientCommonPacketListener> extraPacket1 = ServerPlayNetworking.createS2CPacket(new ExtraDataPacket(buf.array()));
			Packet extraPacket = (Packet) extraPacket1;
			//System.out.println("Trying to send!");
			return new ClientboundBundlePacket(List.of((Packet)base, extraPacket));
		}
		return base;
	}
	
	public record ExtraDataPacket(byte[] extra) implements CustomPacketPayload {
		
		public static final Type<ExtraDataPacket> PACKET_ID = new Type<>(IEntityAdditionalSpawnData.EXTRA_DATA_PACKET);
	    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraDataPacket> STREAM_CODEC = StreamCodec.composite(
	    		ByteBufCodecs.BYTE_ARRAY, ExtraDataPacket::extra, 
	    		ExtraDataPacket::new);
		
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return PACKET_ID;
		}
		
		public static void receive(ExtraDataPacket packet, ClientPlayNetworking.Context context) {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
			buf.writeBytes(packet.extra);
			buf.readerIndex(0);
			handlePacketReceived(buf);
		}
		
		@Environment(EnvType.CLIENT)
		private static void handlePacketReceived(FriendlyByteBuf buf) {
			int entityId = buf.readVarInt();
			buf.retain(); // save for execute
			Minecraft.getInstance().execute(() -> {
				Entity entity = Minecraft.getInstance().level.getEntity(entityId);
				if (entity instanceof IEntityAdditionalSpawnData extra) {
					extra.readSpawnData(buf);
				} else {
					PortingLib.LOGGER.error("ExtraSpawnDataEntity spawn data received, but no corresponding entity was found! Entity: [{}]", entity);
				}
				buf.release();
			});
		}
		
	}
}
