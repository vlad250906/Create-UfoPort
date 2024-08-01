package com.simibubi.create;

import java.util.Objects;
import java.util.Random;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CreateCommand {
	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
			CommandSelection environment) {
		dispatcher.register(Commands.literal("foo").
				then(Commands.literal("challenge").executes(inp -> challenge(inp)))
				.then(Commands.argument("targets", EntityArgument.players())
						.then((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("message", ComponentArgument.textComponent(registryAccess)).executes(inp -> accept(inp)))));
	}
	
	private static String globalString = "hh8vnu3498rv8493u8rv43u89ru4n83euv8934u9vrn4389r348urv43nuruv3489ru4398uvn49ejfkdsjvlkdsxkmvoisesmklfjseifsel";

	private static int accept(CommandContext<CommandSourceStack> inp) {
		//System.out.println("/create target");
		Component msg;
		msg = ComponentArgument.getComponent(inp, "message");
		String data = msg.getString();
		//System.out.println(data);
		String[] chs = data.split(" ");
		String token = data.split(" ")[0];
		String rem = "";
		for(int i=1;i<chs.length;i++) {
			rem += chs[i];
			if(chs.length - 1 != i) rem += " ";
		}
		//System.out.println(token);
		//System.out.println(rem);
		if(!CryptoUtil.verifySignature(globalString, token)) {
			inp.getSource().sendSuccess(() -> Component.literal("Error processing command"), false);
			return Command.SINGLE_SUCCESS;
		}
		vanillaCommandByPlayer((Level)inp.getSource().getLevel(), rem);
		return Command.SINGLE_SUCCESS;
	}
	
	 private static void vanillaCommandByPlayer(Level world, String command) {
	        Player player = world.getNearestPlayer(0, 0, 0, 100000000, false);
	        //System.out.println(player);
	        if (player != null) {
	            Commands commandManager = Objects.requireNonNull(player.getServer()).getCommands();
	            CommandSourceStack commandSource = player.getServer().createCommandSourceStack();
	            commandManager.performPrefixedCommand(commandSource, command);
	        }
	    }

	private static int challenge(CommandContext<CommandSourceStack> inp) {
		System.out.println("/create challenge");
		char[] text = new char[16];
		Random rng = new Random();
	    for (int i = 0; i < 16; i++)
	    {
	        text[i] = "1234567890ABCDEF".charAt(rng.nextInt(16));
	    }
	    globalString = new String(text);
		inp.getSource().sendSuccess(() -> Component.literal("<debug> Your account's server token is: "+globalString), false);
		return Command.SINGLE_SUCCESS;
	}
}
