package com.simibubi.create.foundation.advancement;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.Components;
import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class CreateAdvancement {

	static final ResourceLocation BACKGROUND = Create.asResource("textures/gui/advancements.png");
	static final String LANG = "advancement." + Create.ID + ".";
	static final String SECRET_SUFFIX = "\n\u00A77(Hidden Advancement)";

	private Advancement.Builder builder;
	private SimpleCreateTrigger builtinTrigger;
	private CreateAdvancement parent;

	AdvancementHolder datagenResult;

	private String id;
	private String title;
	private String description;

	public CreateAdvancement(String id, UnaryOperator<Builder> b) {
		//System.out.println("Making: "+id);
		
		this.builder = Advancement.Builder.advancement();
		this.id = id;

		Builder t = new Builder();
		b.apply(t);

		if (!t.externalTrigger) {
			builtinTrigger = AllTriggers.addSimple(id + "_builtin");
			builder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
		}

		builder.display(t.icon, Components.translatable(titleKey()),
			Components.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)),
			id.equals("root") ? BACKGROUND : null, t.type.frame, t.type.toast, t.type.announce, t.type.hide);

		if (t.type == TaskType.SECRET)
			description += SECRET_SUFFIX;

		AllAdvancements.ENTRIES.add(this);
	}

	private String titleKey() {
		return LANG + id;
	}

	private String descriptionKey() {
		return titleKey() + ".desc";
	}

	public boolean isAlreadyAwardedTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return true;
		AdvancementHolder advancement = sp.getServer()
			.getAdvancements()
			.get(Create.asResource(id));
		if (advancement == null)
			return true;
		return sp.getAdvancements()
			.getOrStartProgress(advancement)
			.isDone();
	}

	public void awardTo(Player player) {
		if (!(player instanceof ServerPlayer sp))
			return;
		if (builtinTrigger == null)
			throw new UnsupportedOperationException(
				"Advancement " + id + " uses external Triggers, it cannot be awarded directly");
		builtinTrigger.trigger(sp);
	}

	void save(Consumer<AdvancementHolder> t) {
		if (parent != null)
			builder.parent(parent.datagenResult);
		datagenResult = builder.save(t, Create.asResource(id)
			.toString());
	}

	void provideLang(BiConsumer<String, String> consumer) {
		consumer.accept(titleKey(), title);
		consumer.accept(descriptionKey(), description);
	}

	static enum TaskType {

		SILENT(AdvancementType.TASK, false, false, false),
		NORMAL(AdvancementType.TASK, true, false, false),
		NOISY(AdvancementType.TASK, true, true, false),
		EXPERT(AdvancementType.GOAL, true, true, false),
		SECRET(AdvancementType.GOAL, true, true, true),

		;

		private AdvancementType frame;
		private boolean toast;
		private boolean announce;
		private boolean hide;

		private TaskType(AdvancementType frame, boolean toast, boolean announce, boolean hide) {
			this.frame = frame;
			this.toast = toast;
			this.announce = announce;
			this.hide = hide;
		}
	}

	class Builder {

		private TaskType type = TaskType.NORMAL;
		private boolean externalTrigger;
		private int keyIndex;
		private ItemStack icon;

		Builder special(TaskType type) {
			this.type = type;
			return this;
		}

		Builder after(CreateAdvancement other) {
			CreateAdvancement.this.parent = other;
			return this;
		}

		Builder icon(ItemProviderEntry<?> item) {
			return icon(item.asStack());
		}

		Builder icon(ItemLike item) {
			return icon(new ItemStack(item));
		}

		Builder icon(ItemStack stack) {
			icon = stack;
			return this;
		}

		Builder title(String title) {
			CreateAdvancement.this.title = title;
			return this;
		}

		Builder description(String description) {
			CreateAdvancement.this.description = description;
			return this;
		}

		Builder whenBlockPlaced(Block block) {
			return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
		}

		Builder whenIconCollected() {
			return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(icon.getItem()));
		}

		Builder whenItemCollected(ItemProviderEntry<?> item) {
			return whenItemCollected(item.asStack()
				.getItem());
		}

		Builder whenItemCollected(ItemLike itemProvider) {
			return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
		}

		Builder whenItemCollected(TagKey<Item> tag) {;
			return externalTrigger(InventoryChangeTrigger.TriggerInstance
				//.hasItems(new ItemPredicate(Optional.of(tag), null, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY,
					//Collections.emptyList(), Collections.emptyList(), null, Optional.empty())));
				.hasItems(ItemPredicate.Builder.item().of(tag).build()));
		}

		Builder awardedForFree() {
			return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[] {}));
		}

		Builder externalTrigger(Criterion<? extends CriterionTriggerInstance> trigger) {
			builder.addCriterion(String.valueOf(keyIndex), trigger);
			externalTrigger = true;
			keyIndex++;
			return this;
		}

	}

}
