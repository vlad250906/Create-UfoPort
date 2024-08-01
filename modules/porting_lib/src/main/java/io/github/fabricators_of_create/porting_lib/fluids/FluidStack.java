package io.github.fabricators_of_create.porting_lib.fluids;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.ibm.icu.impl.locale.LocaleDistance.Data;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

public class FluidStack implements DataComponentHolder{
	
	public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidStack::getFluid),
					Codec.LONG.fieldOf("amount").forGetter(FluidStack::getAmount),
					DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> stack.components.asPatch())
			).apply(instance, (fluid, amount, components) -> {
				FluidVariant fl = FluidVariant.of(fluid, components);
				FluidStack stack = new FluidStack(fl, amount);
				return stack;
			})
	);
	
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidStack>(){

        @Override
        public FluidStack decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            long i = registryFriendlyByteBuf.readVarLong();
            if (i <= 0) {
                return EMPTY;
            }
            FluidVariant holder = FluidVariant.PACKET_CODEC.decode(registryFriendlyByteBuf);
            return new FluidStack(holder, i);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, FluidStack stack) {
            if (stack.isEmpty()) {
                registryFriendlyByteBuf.writeVarLong(0);
                return;
            }
            registryFriendlyByteBuf.writeVarLong(stack.getAmount());
            FluidVariant.PACKET_CODEC.encode(registryFriendlyByteBuf, stack.getType());
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidStack>(){

        @Override
        public FluidStack decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
        	FluidStack itemStack = (FluidStack)OPTIONAL_STREAM_CODEC.decode(registryFriendlyByteBuf);
            if (itemStack.isEmpty()) {
                throw new DecoderException("Empty FluidStack not allowed");
            }
            return itemStack;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, FluidStack stack) {
            if (stack.isEmpty()) {
                throw new EncoderException("Empty FluidStack not allowed");
            }
            OPTIONAL_STREAM_CODEC.encode(registryFriendlyByteBuf, stack);
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
    public static final StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
   

	public static final FluidStack EMPTY = new FluidStack(FluidVariant.blank(), 0) {
		@Override
		public FluidStack setAmount(long amount) {
			return this;
		}

		@Override
		public void shrink(int amount) {
		}

		@Override
		public void shrink(long amount) {
		}

		@Override
		public void grow(long amount) {
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public FluidStack copy() {
			return this;
		}
		
	};
	
	private static final Logger LOGGER = LogUtils.getLogger();
	private final FluidVariant type;
	private long amount;
	private PatchedDataComponentMap components;

	public FluidStack(FluidVariant type, long amount) {
		if(amount < 0) throw new IllegalArgumentException("Negative liquid amount in FluidStack: "+amount);
		this.type = type;
		this.amount = amount;
		this.components = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, type.getComponents());
	}

	public FluidStack(FluidVariant type, long amount, PatchedDataComponentMap components) {
		this(type, amount);
		this.components = components;
	}

	public FluidStack(StorageView<FluidVariant> view) {
		this(view.getResource(), view.getAmount());
	}

	public FluidStack(ResourceAmount<FluidVariant> resource) {
		this(resource.resource(), resource.amount());
	}

	/**
	 * Avoid this constructor when possible, may result in NBT loss
	 */
	public FluidStack(Fluid type, long amount) {
		this(FluidVariant.of(type instanceof FlowingFluid flowing ? flowing.getSource() : type), amount);
	}

	public FluidStack(Fluid type, long amount, PatchedDataComponentMap components) {
		this(FluidVariant.of(type instanceof FlowingFluid flowing ? flowing.getSource() : type, components.asPatch()), amount);
		this.components = components;
	}

	public FluidStack(FluidStack copy, long amount) {
		this(copy.getType(), amount);
		components = copy.components;
	}

	public FluidStack setAmount(long amount) {
		this.amount = amount;
		return this;
	}

	public void grow(long amount) {
		setAmount(getAmount() + amount);
	}

	public FluidVariant getType() {
		return type;
	}

	public Fluid getFluid() {
		return getType().getFluid();
	}

	public long getAmount() {
		return amount;
	}

	public boolean isEmpty() {
		return amount <= 0 || getType().isBlank();
	}

	public void shrink(int amount) {
		setAmount(getAmount() - amount);
	}

	public void shrink(long amount) {
		setAmount(getAmount() - amount);
	}

	/**
	 * Determines if the FluidIDs and NBT Tags are equal. This does not check amounts.
	 *
	 * @param other
	 *            The FluidStack for comparison
	 * @return true if the Fluids (IDs and NBT Tags) are the same
	 */
	public boolean isFluidEqual(FluidStack other) {
		if (this == other) return true;
		return isFluidEqual(other.getType());
	}

	public boolean isFluidEqual(FluidVariant other) {
		return isFluidEqual(getType(), other);
	}

	public static boolean isFluidEqual(FluidVariant mine, FluidVariant other) {
		if (mine == other) return true;
		if (other == null) return false;

		boolean fluidsEqual = mine.isOf(other.getFluid());
		boolean tagsEqual = mine.componentsMatch(other.getComponents());

		return fluidsEqual && tagsEqual;
	}

	public boolean canFill(FluidVariant var) {
		return isEmpty() || var.isOf(getFluid()) && var.componentsMatch(this.components.asPatch());
	}

	
	
	
	

	
	public Tag save(HolderLookup.Provider levelRegistryAccess, Tag nbt) {
		if (this.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        }
        return CODEC.encode(this, levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE), nbt).getPartialOrThrow();
	}
	
	public Tag save(HolderLookup.Provider levelRegistryAccess) {
        if (this.isEmpty()) {
            throw new IllegalStateException("Cannot encode empty FluidStack");
        }
        return CODEC.encodeStart(levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE), this).getPartialOrThrow();
    }
	
	public Tag saveOptional(HolderLookup.Provider levelRegistryAccess, Tag nbt) {
        if (this.isEmpty()) {
            return nbt;
        }
        return this.save(levelRegistryAccess, nbt);
    }

    public Tag saveOptional(HolderLookup.Provider levelRegistryAccess) {
        if (this.isEmpty()) {
            return new CompoundTag();
        }
        return this.save(levelRegistryAccess, new CompoundTag());
    }
    
    public static Optional<FluidStack> parse(HolderLookup.Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial();
        //resultOrPartial(string -> LOGGER.error("Tried to load invalid fluid: '{}'", string))
    }
    
    public static FluidStack parseOptional(HolderLookup.Provider lookupProvider, CompoundTag tag) {
        if (tag.isEmpty()) {
            return EMPTY;
        }
        return FluidStack.parse(lookupProvider, tag).orElse(EMPTY);
    }
    

//	public static FluidStack loadFluidStackFromNBT(CompoundTag tag) {
//		FluidStack stack;
//		if (tag.contains("FluidName")) { // legacy forge loading
//			Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(tag.getString("FluidName")));
//			int amount = tag.getInt("Amount");
//			if (tag.contains("Tag")) {
//				stack = new FluidStack(fluid, amount, tag.getCompound("Tag"));
//			} else {
//				stack = new FluidStack(fluid, amount);
//			}
//		} else {
//			CompoundTag fluidTag = tag.getCompound("Variant");
//			FluidVariant fluid = FluidVariant.fromNbt(fluidTag);
//			stack = new FluidStack(fluid, tag.getLong("Amount"));
//			if(tag.contains("Tag", Tag.TAG_COMPOUND))
//				stack.tag = tag.getCompound("Tag");
//		}
//
//		return stack;
//	}

	public Component getDisplayName() {
		return FluidVariantAttributes.getName(this.type);
	}

//	public static FluidStack readFromPacket(FriendlyByteBuf buffer) {
//		FluidVariant fluid = FluidVariant.fromPacket(buffer);
//		long amount = buffer.readVarLong();
//		CompoundTag tag = buffer.readNbt();
//		if (fluid.isBlank()) return EMPTY;
//		return new FluidStack(fluid, amount, tag);
//	}
//
//	public FriendlyByteBuf writeToPacket(FriendlyByteBuf buffer) {
//		getType().toPacket(buffer);
//		buffer.writeVarLong(getAmount());
//		buffer.writeNbt(getTag());
//		return buffer;
//	}

	public FluidStack copy() {
		return new FluidStack(getType(), getAmount(), components);
	}

	private boolean isFluidStackTagEqual(FluidStack other) {
		return Objects.equals(other.components, this.components);
	}

	/**
	 * Determines if the NBT Tags are equal. Useful if the FluidIDs are known to be equal.
	 */
	public static boolean areFluidStackTagsEqual(@NotNull FluidStack stack1, @NotNull FluidStack stack2) {
		return stack1.isFluidStackTagEqual(stack2);
	}

	/**
	 * Determines if the Fluids are equal and this stack is larger.
	 *
	 * @return true if this FluidStack contains the other FluidStack (same fluid and >= amount)
	 */
	public boolean containsFluid(@NotNull FluidStack other) {
		return isFluidEqual(other) && amount >= other.amount;
	}

	/**
	 * Determines if the FluidIDs, Amounts, and NBT Tags are all equal.
	 *
	 * @param other
	 *            - the FluidStack for comparison
	 * @return true if the two FluidStacks are exactly the same
	 */
	public boolean isFluidStackIdentical(FluidStack other) {
		return isFluidEqual(other) && amount == other.amount;
	}

	/**
	 * Determines if the FluidIDs and NBT Tags are equal compared to a registered container
	 * ItemStack. This does not check amounts.
	 *
	 * @param other
	 *            The ItemStack for comparison
	 * @return true if the Fluids (IDs and NBT Tags) are the same
	 */
	public boolean isFluidEqual(@NotNull ItemStack other) {
		Storage<FluidVariant> storage = FluidStorage.ITEM.find(other, ContainerItemContext.withConstant(other));;
		if (storage == null)
			return false;
		return new FluidStack(StorageUtil.findExtractableContent(storage, null)).isFluidEqual(this);
	}

	@Override
	public final int hashCode() {
		long code = 1;
		code = 31 * code + getFluid().hashCode();
		code = 31 * code + amount;
		if (components != null)
			code = 31 * code + components.hashCode();
		return (int) code;
	}

	/**
	 * Default equality comparison for a FluidStack. Same functionality as isFluidEqual().
	 *
	 * This is included for use in data structures.
	 */
	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof FluidStack)) {
			return false;
		}
		return isFluidEqual((FluidStack) o);
	}

	@Override
	public DataComponentMap getComponents() {
		return components;
	}
	
	public <T> T remove(DataComponentType<? extends T> component) {
        return this.components.remove(component);
    }
	
    public <T> T set(DataComponentType<? super T> component, T value) {
        return this.components.set(component, value);
    }
    
    public <T> T getOrCreateComponent(DataComponentType<T> component, T def) {
    	if(!has(component))
    		set(component, def);
    	return get(component);
    }

	@Override
	public String toString() {
		return "FluidStack [type=" + type + ", amount=" + amount + ", components=" + components + "]";
	}
    
    
}
