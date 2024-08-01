package io.github.fabricators_of_create.porting_lib.util;

import java.util.function.Consumer;

import io.github.fabricators_of_create.porting_lib.PortingLibBase;
import io.github.fabricators_of_create.porting_lib.PortingLibClient;
import io.github.fabricators_of_create.porting_lib.core.PortingLib;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.ServerPlayerAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class NetworkHooks {
	public static final ResourceLocation OPEN_ID = PortingLib.id("open_screen");

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * @param player The player to open the GUI for
	 * @param containerSupplier A supplier creating a menu instance on the server.
	 */
	public static void openScreen(ServerPlayer player, MenuProvider containerSupplier) {
		openScreen(player, containerSupplier, buf -> {});
	}

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * @param player The player to open the GUI for
	 * @param containerProvider A supplier creating a menu instance on the server.
	 * @param pos A block pos, which will be encoded into the auxillary data for this request
	 */
	public static void openScreen(ServerPlayer player, MenuProvider containerProvider, BlockPos pos) {
		openScreen(player, containerProvider, buf -> buf.writeBlockPos(pos));
	}

	/**
	 * Request to open a GUI on the client, from the server
	 *
	 * @param player The player to open the GUI for
	 * @param factory A supplier creating a menu instance on the server.
	 * @param extraDataWriter Consumer to write any additional data the GUI needs
	 */
	public static void openScreen(ServerPlayer player, MenuProvider factory, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
		player.doCloseContainer();
		((ServerPlayerAccessor)player).callNextContainerCounter();
		int openContainerId = ((ServerPlayerAccessor)player).getContainerCounter();

		RegistryFriendlyByteBuf extraData = new RegistryFriendlyByteBuf(Unpooled.buffer(), PortingLibBase.getRegistryAccess());
		extraDataWriter.accept(extraData);
		extraData.readerIndex(0); // reset to beginning in case modders read for whatever reason
		byte[] data = extraData.array();
		AbstractContainerMenu menu = factory.createMenu(openContainerId, player.getInventory(), player);
		OpenScreenPacket packet = new OpenScreenPacket(BuiltInRegistries.MENU.getId(menu.getType()), 
				openContainerId, factory.getDisplayName(), data);
		//buf.writeVarInt();
		//buf.writeVarInt(openContainerId);
		//buf.writeComponent();
		//buf.writeVarInt(extraData.readableBytes());
		//buf.writeBytes(extraData);

		ServerPlayNetworking.send(player, packet);

		player.containerMenu = menu;
		((ServerPlayerAccessor)player).callInitMenu(menu);
	}
	
	public record OpenScreenPacket(int menuId, int containerId, Component name, byte[] extra) implements CustomPacketPayload {
		
		public static final Type<OpenScreenPacket> PACKET_ID = new Type<>(NetworkHooks.OPEN_ID);
	    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreenPacket> STREAM_CODEC = StreamCodec.composite(
	    		ByteBufCodecs.INT, OpenScreenPacket::menuId, 
	    		ByteBufCodecs.INT, OpenScreenPacket::containerId, 
	    		ComponentSerialization.STREAM_CODEC, OpenScreenPacket::name, 
	    		ByteBufCodecs.BYTE_ARRAY, OpenScreenPacket::extra, 
	    		OpenScreenPacket::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return PACKET_ID;
		}
		
		public static void receive(PortingLibClient this$, OpenScreenPacket packet, ClientPlayNetworking.Context context) {
			int typeId = packet.menuId;
			int syncId = packet.containerId;
			Component title = packet.name;
			RegistryFriendlyByteBuf extraData = new RegistryFriendlyByteBuf(Unpooled.buffer(), PortingLibBase.getRegistryAccess());
			extraData.writeBytes(packet.extra);
			extraData.readerIndex(0);

			Minecraft.getInstance().execute(() -> this$.openScreen(typeId, syncId, title, extraData));
		}
		
		
		
	}
}
