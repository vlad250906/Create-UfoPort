package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.minecraft.network.chat.MutableComponent;

public class TrainStatusDisplaySource extends SingleLineDisplaySource {

	@Override
	protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof StationBlockEntity observerBE))
			return EMPTY_LINE;
		GlobalStation observer = observerBE.getStation();
		if (observer == null)
			return EMPTY_LINE;
		Train currentTrain = observer.getPresentTrain();
		if (currentTrain == null)
			return EMPTY_LINE;

		ScheduleRuntime runtime = currentTrain.runtime;
		Schedule schedule = runtime.getSchedule();
		if (schedule == null)
			return EMPTY_LINE;
		if (runtime.paused)
			return EMPTY_LINE;
		if (runtime.state != State.POST_TRANSIT)
			return EMPTY_LINE;
		if (runtime.currentEntry == schedule.entries.size() - 1 && !schedule.cyclic)
			return EMPTY_LINE;

		return runtime.getWaitingStatus(context.level());
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return false;
	}

	@Override
	protected String getTranslationKey() {
		return "train_status";
	}

}
