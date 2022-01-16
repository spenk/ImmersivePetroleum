package flaxbeard.immersivepetroleum.common.fluids;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IPFluid extends FlowingFluid {
	public static final List<IPFluid> FLUIDS = new ArrayList<>();
	
	protected final String fluidName;
	protected final ResourceLocation stillTexture;
	protected final ResourceLocation flowingTexture;
	protected IPFluid source;
	protected IPFluid flowing;
	public Block block;
	protected Item bucket;
	@Nullable
	protected final Consumer<FluidAttributes.Builder> buildAttributes;

	public IPFluid(String name, int density, int viscosity){
		this(name,
				new ResourceLocation(ImmersivePetroleum.MODID, "block/fluid/" + name + "_still"),
				new ResourceLocation(ImmersivePetroleum.MODID, "block/fluid/" + name + "_flow"), IPFluid.createBuilder(density, viscosity));
	}
	
	protected IPFluid(String name, ResourceLocation stillTexture, ResourceLocation flowingTexture, @Nullable Consumer<FluidAttributes.Builder> buildAttributes){
		this(name, stillTexture, flowingTexture, buildAttributes, true);
	}
	
	protected IPFluid(String name, ResourceLocation stillTexture, ResourceLocation flowingTexture, @Nullable Consumer<FluidAttributes.Builder> buildAttributes, boolean isSource){
		this.fluidName = name;
		this.stillTexture = stillTexture;
		this.flowingTexture = flowingTexture;
		this.buildAttributes = buildAttributes;
		IPContent.registeredIPFluids.add(this);
		if(!isSource){
			flowing = this;
			setRegistryName(ImmersivePetroleum.MODID, this.fluidName + "_flowing");
		}else{
			this.source = this;
			this.block = createFluidBlock();
			this.bucket = createBucketItem();
			this.flowing = createFlowingFluid();
			
			setRegistryName(new ResourceLocation(ImmersivePetroleum.MODID, this.fluidName));
			
			FLUIDS.add(this);
			IPContent.registeredIPBlocks.add(this.block);
			IPContent.registeredIPItems.add(this.bucket);
		}
	}
	
	protected IPFluidFlowing createFlowingFluid(){
		return new IPFluidFlowing(this);
	}
	
	protected IPFluidBlock createFluidBlock(){
		return new IPFluidBlock(this.source, this.fluidName);
	}
	
	protected IPBucketItem createBucketItem(){
		return new IPBucketItem(this.source, this.fluidName);
	}
	
	@Override
	protected @NotNull FluidAttributes createAttributes(){
		FluidAttributes.Builder builder = FluidAttributes.builder(this.stillTexture, this.flowingTexture)
				.overlay(this.stillTexture)
				.sound(SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
		
		if(this.buildAttributes != null)
			this.buildAttributes.accept(builder);
		
		return builder.build(this);
	}
	
	@Override
	public @NotNull Fluid getFlowing(){
		return this.flowing;
	}

	@Override
	public @NotNull Fluid getSource() {
		return this.source;
	}

	@Override
	protected boolean canConvertToSource() {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(@NotNull LevelAccessor p_76002_, @NotNull BlockPos p_76003_, @NotNull BlockState p_76004_) {
	}
	
	@Override
	public @NotNull Item getBucket(){
		return this.bucket;
	}

	@Override
	protected boolean canBeReplacedWith(@NotNull FluidState p_76127_, @NotNull BlockGetter p_76128_, @NotNull BlockPos p_76129_, @NotNull Fluid p_76130_, @NotNull Direction p_76131_) {
		return p_76131_ == Direction.DOWN && !isSame(p_76130_);
	}

	@Override
	public int getTickDelay(@NotNull LevelReader p_76120_) {
		return 5;
	}
	
	@Override
	protected int getSlopeFindDistance(@NotNull LevelReader arg0){
		return 4;
	}

	@Override
	protected int getDropOff(@NotNull LevelReader p_76087_) {
		return 1;
	}

	@Override
	public int getAmount(@NotNull FluidState state) {
		return isSource(state) ? 8 : state.getValue(LEVEL);
	}

	@Override
	protected float getExplosionResistance(){
		return 100;
	}

	@Override
	protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
		return this.block.defaultBlockState().setValue(FlowingFluid.LEVEL, getLegacyLevel(state));
	}
	
	@Override
	public boolean isSource(FluidState state){
		return state.getType() == this.source;
	}
	
	@Override
	public boolean isSame(Fluid fluidIn){
		return fluidIn == this.source || fluidIn == this.flowing;
	}
	
	public static Consumer<FluidAttributes.Builder> createBuilder(int density, int viscosity){
		return builder -> builder.viscosity(viscosity).density(density);
	}
	
	// STATIC CLASSES
	
	public static class IPFluidBlock extends LiquidBlock {
		private static IPFluid tmp = null;
		
		private IPFluid fluid;
		public IPFluidBlock(IPFluid fluid, String fluidName){
			super(supplier(fluid), BlockBehaviour.Properties.of(Material.WATER));
			this.fluid = fluid;
			setRegistryName(new ResourceLocation(ImmersivePetroleum.MODID, fluidName + "_fluid_block"));
		}
		
		@Override
		protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder){
			super.createBlockStateDefinition(builder);
			IPFluid f = this.fluid != null ? this.fluid : tmp;
			builder.add(f.getStateDefinition().getProperties().toArray(new Property[0]));
		}
		
		@Override
		public @NotNull FluidState getFluidState(@NotNull BlockState state){
			FluidState baseState = super.getFluidState(state);
			for(Property<?> prop:this.fluid.getStateDefinition().getProperties())
				if(prop != LiquidBlock.LEVEL)
					baseState = withCopiedValue(prop, baseState, state);
			return baseState;
		}
		
		private <T extends StateHolder<?, T>, S extends Comparable<S>> T withCopiedValue(Property<S> prop, T oldState, StateHolder<?, ?> copyFrom){
			return oldState.setValue(prop, copyFrom.getValue(prop));
		}
		
		private static Supplier<IPFluid> supplier(IPFluid fluid){
			tmp = fluid;
			return () -> fluid;
		}
	}
	
	public static class IPBucketItem extends BucketItem {
		private static final Item.Properties PROPS = new Item.Properties().stacksTo(1).tab(ImmersivePetroleum.creativeTab);
		
		public IPBucketItem(IPFluid fluid, String fluidName){
			super(() -> fluid, PROPS);
			setRegistryName(new ResourceLocation(ImmersivePetroleum.MODID, fluidName + "_bucket"));
		}
		
		@Override
		public ItemStack getContainerItem(ItemStack itemStack){
			return new ItemStack(Items.BUCKET);
		}
		
		@Override
		public boolean hasContainerItem(ItemStack stack){
			return true;
		}
		
		@Override
		public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt){
			return new FluidBucketWrapper(stack);
		}
	}
	
	public static class IPFluidFlowing extends IPFluid {
		public IPFluidFlowing(IPFluid source){
			super(source.fluidName, source.stillTexture, source.flowingTexture, source.buildAttributes, false);
			this.source = source;
			this.bucket = source.bucket;
			this.block = source.block;
			registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 7));
		}
		
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder){
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}
	}
}
