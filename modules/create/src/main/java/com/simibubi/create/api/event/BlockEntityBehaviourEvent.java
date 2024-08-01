package com.simibubi.create.api.event;

import java.util.Map;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Event that is fired just before a SmartBlockEntity is being deserialized<br>
 * Also if a new one is placed<br>
 * Use it to attach a new {@link BlockEntityBehaviour} or replace existing ones
 * (with caution)<br>
 * <br>
 * Actual setup of the behaviours internal workings and data should be done in
 * BlockEntityBehaviour#read() and BlockEntityBehaviour#initialize()
 * respectively.<br>
 * <br>
 * Because of the earliness of this event, the added behaviours will have access
 * to the initial NBT read (unless the BE was placed, not loaded), thereby
 * allowing block entities to store and retrieve data for injected behaviours.
 */
public class BlockEntityBehaviourEvent {
	public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
		for (Callback callback : callbacks)
			callback.manageBehaviors(event);
	});

	private SmartBlockEntity smartBlockEntity;
	private Map<BehaviourType<?>, BlockEntityBehaviour> behaviours;

	public BlockEntityBehaviourEvent(SmartBlockEntity blockEntity,
			Map<BehaviourType<?>, BlockEntityBehaviour> behaviours) {
		smartBlockEntity = blockEntity;
		this.behaviours = behaviours;
	}

	public void attach(BlockEntityBehaviour behaviour) {
		behaviours.put(behaviour.getType(), behaviour);
	}

	public BlockEntityBehaviour remove(BehaviourType<?> type) {
		return behaviours.remove(type);
	}

	public SmartBlockEntity getBlockEntity() {
		return smartBlockEntity;
	}

	public BlockState getBlockState() {
		return smartBlockEntity.getBlockState();
	}

	public interface Callback {
		void manageBehaviors(BlockEntityBehaviourEvent event);
	}
}
