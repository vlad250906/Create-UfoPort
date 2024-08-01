package com.simibubi.create.compat.sodium;

import java.lang.reflect.Method;
import java.util.function.Function;

import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.utility.Components;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Fixes the Mechanical Saw's sprite and lets players know when Indium isn't installed.
 */
public class SodiumCompat {
	public static final ResourceLocation SAW_TEXTURE = Create.asResource("block/saw_reversed");

	public static void init() {
		if (!Mods.INDIUM.isLoaded()) {
			ClientPlayConnectionEvents.JOIN.register(SodiumCompat::sendNoIndiumWarning);
		}
		ClientPlayConnectionEvents.JOIN.register(SodiumCompat::sendTrackerInfo);
		if (spriteUtilWorks()) {
			Minecraft mc = Minecraft.getInstance();
			WorldRenderEvents.START.register(ctx -> {
				Function<ResourceLocation, TextureAtlasSprite> atlas = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
				TextureAtlasSprite sawSprite = atlas.apply(SAW_TEXTURE);
				SpriteUtil.markSpriteActive(sawSprite);
			});
		}
	}
	
	public static void sendTrackerInfo(ClientPacketListener handler, PacketSender sender, Minecraft mc) {
		if (mc.player == null)
			return;

		MutableComponent text = ComponentUtils.wrapInSquareBrackets(Components.literal("INFO"))
				.withStyle(ChatFormatting.AQUA)
				.append(Components.literal(" You are using Create v-0.9.0-ALPHA, be ready for bugs. You should report them to server's admin")
				);

		mc.player.displayClientMessage(text, false);
	}

	public static void sendNoIndiumWarning(ClientPacketListener handler, PacketSender sender, Minecraft mc) {
		if (mc.player == null)
			return;

		MutableComponent text = ComponentUtils.wrapInSquareBrackets(Components.literal("WARN"))
				.withStyle(ChatFormatting.GOLD)
				.append(Components.literal(" Sodium is installed, but Indium is not. This will cause visual issues with Create!")
				)
				.withStyle(style -> style
						.withClickEvent(
								new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/indium")
						)
						.withHoverEvent(
								new HoverEvent(HoverEvent.Action.SHOW_TEXT, Components.literal("Click here to open Indium's mod page"))
						)
				);

		mc.player.displayClientMessage(text, false);
	}

	private static boolean spriteUtilWorks() {
		try {
			// make sure class and method still exist, sodium is unstable
			Method method = SpriteUtil.class.getMethod("markSpriteActive", TextureAtlasSprite.class); // throws if missing
			if (method.getReturnType() != Void.TYPE)
				throw new IllegalStateException("markSpriteActive's signature has changed");
			return true;
		} catch (Throwable t) {
			Create.LOGGER.error("Create's Sodium compat errored and has been partially disabled. Report this!", t);
		}
		return false;
	}
}
