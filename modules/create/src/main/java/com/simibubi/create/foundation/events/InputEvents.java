package com.simibubi.create.foundation.events;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;

import io.github.fabricators_of_create.porting_lib.event.client.InteractEvents;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;

public class InputEvents {

	public static void onKeyInput(int key, int scancode, int action, int mods) {
		if (Minecraft.getInstance().screen != null)
			return;

		boolean pressed = !(action == 0);

		CreateClient.SCHEMATIC_HANDLER.onKeyInput(key, pressed);
		ToolboxHandlerClient.onKeyInput(key, pressed);
	}

	public static boolean onMouseScrolled(double deltaX, double delta /* Y */) {
		if (Minecraft.getInstance().screen != null)
			return false;

//		CollisionDebugger.onScroll(delta);
		boolean cancelled = CreateClient.SCHEMATIC_HANDLER.mouseScrolled(delta)
			|| CreateClient.SCHEMATIC_AND_QUILL_HANDLER.mouseScrolled(delta) || TrainHUD.onScroll(delta)
			|| ElevatorControlsHandler.onScroll(delta);
		return cancelled;
	}

	public static boolean onMouseInput(int button, int modifiers, Action action) {
		if (Minecraft.getInstance().screen != null)
			return false;

		boolean pressed = action == Action.PRESS;

		if (CreateClient.SCHEMATIC_HANDLER.onMouseInput(button, pressed))
			return true;
		else if (CreateClient.SCHEMATIC_AND_QUILL_HANDLER.onMouseInput(button, pressed))
			return true;
		return false;
	}

	// fabric: onClickInput split up
	public static InteractionResult onUse(Minecraft mc, HitResult hit, InteractionHand hand) {
		if (mc.screen != null)
			return InteractionResult.PASS;

		if (CurvedTrackInteraction.onClickInput(true, false)) {
			return InteractionResult.SUCCESS;
		}


		boolean glueCancelled = CreateClient.GLUE_HANDLER.onMouseInput(false);
		LinkedControllerClientHandler.deactivateInLectern();
		boolean relocatorCancelled = TrainRelocator.onClicked();

		return glueCancelled || relocatorCancelled
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}

	public static InteractionResult onAttack(Minecraft mc, HitResult hit) {
		if (mc.screen != null)
			return InteractionResult.PASS;

		if (CurvedTrackInteraction.onClickInput(false, true)) {
			return InteractionResult.SUCCESS;
		}

		return CreateClient.GLUE_HANDLER.onMouseInput(true)
				? InteractionResult.SUCCESS
				: InteractionResult.PASS;
	}

	public static boolean onPick(Minecraft mc, HitResult hit) {
		if (mc.screen != null)
			return false;

		return ToolboxHandlerClient.onPickItem();
	}

	public static void register() {
		KeyInputCallback.EVENT.register(InputEvents::onKeyInput);
		MouseInputEvents.BEFORE_SCROLL.register(InputEvents::onMouseScrolled);
		MouseInputEvents.BEFORE_BUTTON.register(InputEvents::onMouseInput);
		InteractEvents.USE.register(InputEvents::onUse);
		InteractEvents.ATTACK.register(InputEvents::onAttack);
		InteractEvents.PICK.register(InputEvents::onPick);
	}

}
