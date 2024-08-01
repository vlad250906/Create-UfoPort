package com.simibubi.create.foundation.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public class ItemHelper {

	public static boolean sameItem(ItemStack stack, ItemStack otherStack) {
		return !otherStack.isEmpty() && stack.is(otherStack.getItem());
	}

	public static Predicate<ItemStack> sameItemPredicate(ItemStack stack) {
		return s -> sameItem(stack, s);
	}

	public static void dropContents(Level world, BlockPos pos, Storage<ItemVariant> inv) {
		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : inv.nonEmptyViews()) {
				ItemStack stack = view.getResource().toStack((int) view.getAmount());
				Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}
	}

	public static List<ItemStack> multipliedOutput(ItemStack in, ItemStack out) {
		List<ItemStack> stacks = new ArrayList<>();
		ItemStack result = out.copy();
		result.setCount(in.getCount() * out.getCount());

		while (result.getCount() > result.getMaxStackSize()) {
			stacks.add(result.split(result.getMaxStackSize()));
		}

		stacks.add(result);
		return stacks;
	}

	public static void addToList(ItemStack stack, List<ItemStack> stacks) {
		for (ItemStack s : stacks) {
			if (!ItemHandlerHelper.canItemStacksStack(stack, s))
				continue;
			int transferred = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
			s.grow(transferred);
			stack.shrink(transferred);
		}
		if (stack.getCount() > 0)
			stacks.add(stack);
	}
	
	public static <T> T getOrCreateComponent(ItemStack stack, DataComponentType<T> comp, T newone){
		if(stack.has(comp)) return stack.get(comp);
		stack.set(comp, newone);
		return newone;
	}
	
	public static void clearComponents(ItemStack is) {
		is.getComponents().stream().forEach(obj -> is.remove(obj.type()));
	}

//	public static boolean isSameInventory(Storage<ItemVariant> h1, Storage<ItemVariant> h2) {
//		if (h1 == null || h2 == null)
//			return false;
//		if (h1.getSlotCount() != h2.getSlotCount())
//			return false;
//		for (int slot = 0; slot < h1.getSlotCount(); slot++) {
//			if (h1.getStackInSlot(slot) != h2.getStackInSlot(slot))
//				return false;
//		}
//		return true;
//	}

	public static int calcRedstoneFromInventory(@Nullable Storage<ItemVariant> inv) {
		if (inv == null)
			return 0;
		int i = 0;
		float f = 0.0F;
		int totalSlots = 0;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : inv) {
				long slotLimit = view.getCapacity();
				if (slotLimit == 0) {
					continue;
				}
				totalSlots++;
				if (!view.isResourceBlank()) {
					f += (float) view.getAmount()
							/ (float) Math.min(slotLimit, view.getResource().getItem().getDefaultMaxStackSize());
					++i;
				}
			}
		}

		if (totalSlots == 0)
			return 0;

		f = f / totalSlots;
		return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
	}

	public static List<Pair<Ingredient, MutableInt>> condenseIngredients(NonNullList<Ingredient> recipeIngredients) {
		List<Pair<Ingredient, MutableInt>> actualIngredients = new ArrayList<>();
		Ingredients: for (Ingredient igd : recipeIngredients) {
			for (Pair<Ingredient, MutableInt> pair : actualIngredients) {
				ItemStack[] stacks1 = pair.getFirst().getItems();
				ItemStack[] stacks2 = igd.getItems();
				if (stacks1.length != stacks2.length)
					continue;
				for (int i = 0; i <= stacks1.length; i++) {
					if (i == stacks1.length) {
						pair.getSecond().increment();
						continue Ingredients;
					}
					if (!ItemStack.matches(stacks1[i], stacks2[i]))
						break;
				}
			}
			actualIngredients.add(Pair.of(igd, new MutableInt(1)));
		}
		return actualIngredients;
	}

	public static boolean matchIngredients(Ingredient i1, Ingredient i2) {
		if (i1 == i2)
			return true;
		ItemStack[] stacks1 = i1.getItems();
		ItemStack[] stacks2 = i2.getItems();
		if (stacks1 == stacks2)
			return true;
		if (stacks1.length == stacks2.length) {
			for (int i = 0; i < stacks1.length; i++)
				if (!ItemStack.isSameItem(stacks1[i], stacks2[i]))
					return false;
			return true;
		}
		return false;
	}

	public static boolean matchAllIngredients(NonNullList<Ingredient> ingredients) {
		if (ingredients.size() <= 1)
			return true;
		Ingredient firstIngredient = ingredients.get(0);
		for (int i = 1; i < ingredients.size(); i++)
			if (!matchIngredients(firstIngredient, ingredients.get(i)))
				return false;
		return true;
	}

	public static enum ExtractionCountMode {
		EXACTLY, UPTO
	}

	public static ItemStack extract(Storage<ItemVariant> inv, Predicate<ItemStack> test, boolean simulate) {
		return extract(inv, test, ExtractionCountMode.UPTO, 64, simulate);
	}

	public static ItemStack extract(Storage<ItemVariant> inv, Predicate<ItemStack> test, int exactAmount,
			boolean simulate) {
		return extract(inv, test, ExtractionCountMode.EXACTLY, exactAmount, simulate);
	}

	public static ItemStack extract(Storage<ItemVariant> inv, Predicate<ItemStack> test, ExtractionCountMode mode,
			int amount, boolean simulate) {
		int extracted = 0;
		ItemVariant extracting = null;
		List<ItemVariant> otherTargets = null;

		if (inv.supportsExtraction()) {
			try (Transaction t = TransferUtil.getTransaction()) {
				for (StorageView<ItemVariant> view : inv.nonEmptyViews()) {
					ItemVariant contained = view.getResource();
					int maxStackSize = contained.getItem().getDefaultMaxStackSize();
					// amount stored, amount needed, or max size, whichever is lowest.
					int amountToExtractFromThisSlot = Math.min(truncateLong(view.getAmount()),
							Math.min(amount - extracted, maxStackSize));
					if (!test.test(contained.toStack(amountToExtractFromThisSlot)))
						continue;
					if (extracting == null) {
						extracting = contained; // we found a target
					}
					boolean sameType = extracting.equals(contained);
					if (sameType && maxStackSize == extracted) {
						// stack is maxed out, skip
						continue;
					}
					if (!sameType) {
						// multiple types passed the test
						if (otherTargets == null) {
							otherTargets = new ArrayList<>();
						}
						otherTargets.add(contained);
						continue;
					}
					ItemVariant toExtract = extracting;
					long actualExtracted = view.extract(toExtract, amountToExtractFromThisSlot, t);
					if (actualExtracted == 0)
						continue;
					extracted += actualExtracted;
					if (extracted == amount) {
						if (!simulate)
							t.commit();
						return toExtract.toStack(extracted);
					}
				}

				// if the code reaches this point, we've extracted as much as possible, and it
				// isn't enough.
				if (mode == ExtractionCountMode.UPTO) { // we don't need to get exactly the amount requested
					if (extracting != null && extracted != 0) {
						if (!simulate)
							t.commit();
						return extracting.toStack(extracted);
					}
				} else {
					// let's try a different target
					if (otherTargets != null) {
						t.abort();
						try (Transaction nested = TransferUtil.getTransaction()) {
							for (ItemVariant target : otherTargets) {
								// try again, but now only match the existing matches we've found
								ItemStack successfulExtraction = extract(inv, target::matches, mode, amount, simulate);
								if (!successfulExtraction.isEmpty()) {
									if (!simulate)
										nested.commit();
									return successfulExtraction;
								}
							}
						}
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack extract(Storage<ItemVariant> inv, Predicate<ItemStack> test,
			Function<ItemStack, Integer> amountFunction, boolean simulate) {
		ItemStack extracting = ItemStack.EMPTY;
		int maxExtractionCount = 64;

		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : inv.nonEmptyViews()) {
				ItemVariant var = view.getResource();
				ItemStack stackInSlot = var.toStack();
				if (!test.test(stackInSlot))
					continue;
				if (extracting.isEmpty()) {
					int maxExtractionCountForItem = amountFunction.apply(stackInSlot);
					if (maxExtractionCountForItem == 0)
						continue;
					maxExtractionCount = Math.min(maxExtractionCount, maxExtractionCountForItem);
				}

				try (Transaction nested = t.openNested()) {
					long extracted = view.extract(var, maxExtractionCount - extracting.getCount(), nested);
					ItemStack stack = var.toStack((int) extracted);

					if (!test.test(stack))
						continue;
					if (!extracting.isEmpty() && !canItemStackAmountsStack(stack, extracting))
						continue;
					nested.commit();
					if (extracting.isEmpty())
						extracting = stack.copy();
					else
						extracting.grow(stack.getCount());

					if (extracting.getCount() >= maxExtractionCount)
						break;
				}
			}
			if (!simulate)
				t.commit();
		}

		return extracting;
	}

	public static boolean canItemStackAmountsStack(ItemStack a, ItemStack b) {
		return ItemHandlerHelper.canItemStacksStack(a, b) && a.getCount() + b.getCount() <= a.getMaxStackSize();
	}

	public static int truncateLong(long l) {
		if (l > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else if (l < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		} else {
			return (int) l;
		}
	}

//	public static ItemStack findFirstMatch(Storage<ItemVariant> inv, Predicate<ItemStack> test) {
//		int slot = findFirstMatchingSlotIndex(inv, test);
//		if (slot == -1)
//			return ItemStack.EMPTY;
//		else
//			return inv.getStackInSlot(slot);
//	}
}
