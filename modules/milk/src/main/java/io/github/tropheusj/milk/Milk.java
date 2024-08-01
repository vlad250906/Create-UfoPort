package io.github.tropheusj.milk;

import static net.minecraft.world.item.Items.BUCKET;
import static net.minecraft.world.item.Items.DRAGON_BREATH;
import static net.minecraft.world.item.Items.GLASS_BOTTLE;
import static net.minecraft.world.item.Items.GUNPOWDER;
import static net.minecraft.world.item.Items.MILK_BUCKET;

import io.github.tropheusj.milk.mixin.BrewingRecipeRegistryAccessor;
import io.github.tropheusj.milk.potion.MilkAreaEffectCloudEntity;
import io.github.tropheusj.milk.potion.MilkPotionDispenserBehavior;
import io.github.tropheusj.milk.potion.bottle.LingeringMilkBottle;
import io.github.tropheusj.milk.potion.bottle.MilkBottle;
import io.github.tropheusj.milk.potion.bottle.SplashMilkBottle;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.fluid.CauldronFluidContent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

@SuppressWarnings("UnstableApiUsage")
public class Milk implements ModInitializer {
	public static final String MOD_ID = "milk";
	// fluids - if any are non-null, all are non-null.
	public static FlowingFluid STILL_MILK = null;
	public static FlowingFluid FLOWING_MILK = null;
	public static Block MILK_FLUID_BLOCK = null;

	// bottles - may be enabled individually.
	public static Item MILK_BOTTLE = null;
	public static Item SPLASH_MILK_BOTTLE = null;
	public static Item LINGERING_MILK_BOTTLE = null;

	// cauldron.
	public static Block MILK_CAULDRON = null;

	// if true, milk can be placed from buckets.
	public static boolean MILK_PLACING_ENABLED = false;
	// if true, milk can make infinite sources like water.
	public static boolean INFINITE_MILK_FLUID = true;

	public static EntityType<MilkAreaEffectCloudEntity> MILK_EFFECT_CLOUD_ENTITY_TYPE = null;

	// tags.
	// all milk fluids.
	public static final TagKey<Fluid> MILK_FLUID_TAG = ConventionalFluidTags.MILK;
	// all milk bottles.
	public static final TagKey<Item> MILK_BOTTLE_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "milk_bottles"));
	// all milk buckets.
	public static final TagKey<Item> MILK_BUCKET_TAG = ConventionalItemTags.MILK_BUCKETS;

	public static void enableMilkPlacing() {
		if (STILL_MILK == null) {
			throw new RuntimeException("to enable milk placing, you need to enable milk with enableMilkFluid()!");
		}
		MILK_PLACING_ENABLED = true;
	}

	public static void enableMilkFluid() {
		if (STILL_MILK == null) {
			// register
			STILL_MILK = Registry.register(
					BuiltInRegistries.FLUID,
					id("still_milk"),
					new MilkFluid.Still()
			);
			FLOWING_MILK = Registry.register(
					BuiltInRegistries.FLUID,
					id("flowing_milk"),
					new MilkFluid.Flowing()
			);
			MILK_FLUID_BLOCK = Registry.register(
					BuiltInRegistries.BLOCK,
					id("milk_fluid_block"),
					new MilkFluidBlock(STILL_MILK, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.TERRACOTTA_WHITE))
			);

			// transfer
			FluidStorage.combinedItemApiProvider(MILK_BUCKET).register(context ->
					new FullItemFluidStorage(context, bucket -> ItemVariant.of(BUCKET), FluidVariant.of(STILL_MILK), FluidConstants.BUCKET)
			);
			FluidStorage.combinedItemApiProvider(BUCKET).register(context ->
					new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(MILK_BUCKET), STILL_MILK, FluidConstants.BUCKET)
			);

			// extras
			if (MILK_CAULDRON != null) {
				CauldronFluidContent.registerCauldron(MILK_CAULDRON, STILL_MILK, FluidConstants.BOTTLE, LayeredCauldronBlock.LEVEL);
			}
			if (MILK_BOTTLE != null) {
				FluidStorage.combinedItemApiProvider(MILK_BOTTLE).register(context ->
						new FullItemFluidStorage(context, bottle -> ItemVariant.of(GLASS_BOTTLE), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
				);
				FluidStorage.combinedItemApiProvider(GLASS_BOTTLE).register(context ->
						new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(MILK_BOTTLE), STILL_MILK, FluidConstants.BOTTLE)
				);
			}
		}
	}

	public static void finiteMilkFluid() {
		INFINITE_MILK_FLUID = false;
	}

	public static void enableCauldron() {
		if (MILK_CAULDRON == null) {
			// register
			MILK_CAULDRON = Registry.register(
					BuiltInRegistries.BLOCK,
					id("milk_cauldron"),
					new MilkCauldron(FabricBlockSettings.copyOf(Blocks.CAULDRON))
			);
			CauldronInteraction.EMPTY.map().put(MILK_BUCKET, MilkCauldron.FILL_FROM_BUCKET);
			// transfer
			if (STILL_MILK != null) {
				CauldronFluidContent.registerCauldron(MILK_CAULDRON, STILL_MILK, FluidConstants.BOTTLE, LayeredCauldronBlock.LEVEL);
			}
			// cauldron interactions
			if (MILK_BOTTLE != null && !CauldronInteraction.EMPTY.map().containsKey(MILK_BOTTLE)) {
				CauldronInteraction fillFromMilkBottle = MilkCauldron.addInputToCauldronExchange(
						Milk.MILK_BOTTLE.getDefaultInstance(), Items.GLASS_BOTTLE.getDefaultInstance(), true);
				CauldronInteraction.EMPTY.map().put(MILK_BOTTLE, fillFromMilkBottle);
				MilkCauldron.MILK_CAULDRON_BEHAVIOR.map().put(Milk.MILK_BOTTLE, fillFromMilkBottle);

				CauldronInteraction emptyToBottle = MilkCauldron.addOutputToItemExchange(
						Items.GLASS_BOTTLE.getDefaultInstance(), Milk.MILK_BOTTLE.getDefaultInstance(), true);
				MilkCauldron.MILK_CAULDRON_BEHAVIOR.map().put(Items.GLASS_BOTTLE, emptyToBottle);
			}
		}
	}

	public static void enableMilkBottle() {
		if (MILK_BOTTLE == null) {
			// register
			MILK_BOTTLE = Registry.register(
					BuiltInRegistries.ITEM,
					id("milk_bottle"),
					new MilkBottle(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(1))
			);
			// potions
			
			// transfer
			if (STILL_MILK != null) {
				FluidStorage.combinedItemApiProvider(MILK_BOTTLE).register(context ->
						new FullItemFluidStorage(context, bottle -> ItemVariant.of(GLASS_BOTTLE), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
				);
				FluidStorage.combinedItemApiProvider(GLASS_BOTTLE).register(context ->
						new EmptyItemFluidStorage(context, bucket -> ItemVariant.of(MILK_BOTTLE), STILL_MILK, FluidConstants.BOTTLE)
				);
			}
			// cauldron interactions
			if (MILK_CAULDRON != null && !CauldronInteraction.EMPTY.map().containsKey(MILK_BOTTLE)) {
				CauldronInteraction fillFromMilkBottle = MilkCauldron.addInputToCauldronExchange(
						Milk.MILK_BOTTLE.getDefaultInstance(), Items.GLASS_BOTTLE.getDefaultInstance(), true);
				CauldronInteraction.EMPTY.map().put(MILK_BOTTLE, fillFromMilkBottle);
				MilkCauldron.MILK_CAULDRON_BEHAVIOR.map().put(Milk.MILK_BOTTLE, fillFromMilkBottle);

				CauldronInteraction emptyToBottle = MilkCauldron.addOutputToItemExchange(
						Items.GLASS_BOTTLE.getDefaultInstance(), Milk.MILK_BOTTLE.getDefaultInstance(), true);
				MilkCauldron.MILK_CAULDRON_BEHAVIOR.map().put(Items.GLASS_BOTTLE, emptyToBottle);
			}
		}
	}

	public static void enableSplashMilkBottle() {
		if (SPLASH_MILK_BOTTLE == null) {
			// register
			SPLASH_MILK_BOTTLE = Registry.register(
					BuiltInRegistries.ITEM,
					id("splash_milk_bottle"),
					new SplashMilkBottle(new Item.Properties().stacksTo(1))
			);
			// potions
			
			// transfer
			if (STILL_MILK != null) {
				FluidStorage.combinedItemApiProvider(SPLASH_MILK_BOTTLE).register(context ->
						new FullItemFluidStorage(context, bottle -> ItemVariant.of(GLASS_BOTTLE), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
				);
			}
			// dispenser interactions
			DispenserBlock.registerBehavior(SPLASH_MILK_BOTTLE, MilkPotionDispenserBehavior.INSTANCE);
		}
	}

	public static void enableLingeringMilkBottle() {
		if (LINGERING_MILK_BOTTLE == null) {
			// register
			LINGERING_MILK_BOTTLE = Registry.register(
					BuiltInRegistries.ITEM,
					id("lingering_milk_bottle"),
					new LingeringMilkBottle(new Item.Properties().stacksTo(1))
			);
			// potions
			
			// lingering effect
			MILK_EFFECT_CLOUD_ENTITY_TYPE = Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					id("milk_area_effect_cloud"),
					FabricEntityTypeBuilder.<MilkAreaEffectCloudEntity>create()
							.fireImmune()
							.dimensions(EntityDimensions.fixed(6.0F, 0.5F))
							.trackRangeChunks(10)
							.trackedUpdateRate(Integer.MAX_VALUE)
							.build()
			);
			// potions
			
			// transfer
			if (STILL_MILK != null) {
				FluidStorage.combinedItemApiProvider(LINGERING_MILK_BOTTLE).register(context ->
						new FullItemFluidStorage(context, bottle -> ItemVariant.of(GLASS_BOTTLE), FluidVariant.of(STILL_MILK), FluidConstants.BOTTLE)
				);
			}
			// dispenser interactions
			DispenserBlock.registerBehavior(LINGERING_MILK_BOTTLE, MilkPotionDispenserBehavior.INSTANCE);
		}
	}

	public static void enableAllMilkBottles(BrewingRecipeRegistryAccessor access) {
		if(LINGERING_MILK_BOTTLE != null) {
			access.milk$addContainer(LINGERING_MILK_BOTTLE);
		}
		if (SPLASH_MILK_BOTTLE != null) {
			access.milk$addContainerRecipe(SPLASH_MILK_BOTTLE, DRAGON_BREATH, LINGERING_MILK_BOTTLE);
			access.milk$addContainer(SPLASH_MILK_BOTTLE);
		}
		if (MILK_BOTTLE != null) {
			access.milk$addContainerRecipe(MILK_BOTTLE, GUNPOWDER, SPLASH_MILK_BOTTLE);
			access.milk$addContainer(MILK_BOTTLE);
		}
	}

	public static boolean isMilk(BlockState state) {
		return isMilk(state.getFluidState());
	}

	public static boolean isMilk(FluidState state) {
		return (STILL_MILK != null && state.is(STILL_MILK)) || (FLOWING_MILK != null && state.is(FLOWING_MILK));
	}

	public static boolean isMilkBottle(Item item) {
		return item == Milk.MILK_BOTTLE || item == Milk.SPLASH_MILK_BOTTLE || item == Milk.LINGERING_MILK_BOTTLE;
	}

	public static boolean tryRemoveRandomEffect(LivingEntity user) {
		if (user.getActiveEffects().size() > 0) {
			int indexOfEffectToRemove = user.level().random.nextInt(user.getActiveEffects().size());
			MobEffectInstance effectToRemove = (MobEffectInstance) user.getActiveEffects().toArray()[indexOfEffectToRemove];
			user.removeEffect(effectToRemove.getEffect());
			return true;
		}
		return false;
	}

	@Override
	public void onInitialize() {
		
		enableMilkFluid();
		enableMilkPlacing();
		enableCauldron();
		enableMilkBottle();
		enableSplashMilkBottle();
		enableLingeringMilkBottle();
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			if (MILK_BOTTLE != null)
				entries.accept(MILK_BOTTLE);
			if (SPLASH_MILK_BOTTLE != null)
				entries.accept(SPLASH_MILK_BOTTLE);
			if (LINGERING_MILK_BOTTLE != null)
				entries.accept(LINGERING_MILK_BOTTLE);
		});
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
