package com.simibubi.create.api.event;

import com.simibubi.create.content.trains.graph.TrackGraph;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class TrackGraphMergeEvent {
	public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
		for (Callback callback : callbacks)
			callback.onMerge(event);
	});

	private TrackGraph mergedInto, mergedFrom;

	public TrackGraphMergeEvent(TrackGraph from, TrackGraph into) {
		mergedInto = into;
		mergedFrom = from;
	}

	public TrackGraph getGraphMergedInto() {
		return mergedInto;
	}

	public TrackGraph getGraphMergedFrom() {
		return mergedFrom;
	}

	public interface Callback {
		void onMerge(TrackGraphMergeEvent event);
	}
}
