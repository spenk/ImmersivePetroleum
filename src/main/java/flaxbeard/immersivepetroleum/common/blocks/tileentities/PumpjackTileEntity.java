package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.ticking.IEClientTickableBE;
import blusunrize.immersiveengineering.common.register.IEBlocks.MetalDevices;
import blusunrize.immersiveengineering.common.util.MultiblockCapability;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.orientation.RelativeBlockFace;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler;
import flaxbeard.immersivepetroleum.common.cfg.IPServerConfig;
import flaxbeard.immersivepetroleum.common.multiblocks.PumpjackMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PumpjackTileEntity extends PoweredMultiblockBlockEntity<PumpjackTileEntity, MultiblockRecipe> implements IBlockBounds, IEClientTickableBE {
	/** Template-Location of the Energy Input Port. (0, 1, 5) */
	public static final Set<BlockPos> Redstone_IN = ImmutableSet.of(new BlockPos(0, 1, 5));
	
	/** Template-Location of the Redstone Input Port. (2, 1, 5) */
	public static final ImmutableSet<MultiblockFace> Energy_IN = ImmutableSet.of(new MultiblockFace(2, 1, 5, RelativeBlockFace.FRONT));

	/** Template-Location of the Eastern Fluid Output Port. (2, 0, 2) */
	public static final BlockPos East_Port = new BlockPos(2, 0, 2);

	/** Template-Location of the Western Fluid Output Port. (0, 0, 2) */
	public static final BlockPos West_Port = new BlockPos(0, 0, 2);
	
	/**
	 * Template-Location of the Bottom Fluid Output Port. (1, 0, 0) <b>(Also
	 * Master Block)</b>
	 */
	public static final BlockPos Down_Port = new BlockPos(1, 0, 0);

	public FluidTank fakeTank = new FluidTank(0);
	public boolean wasActive = false;
	public float activeTicks = 0;
	private int pipeTicks = 0;
	private boolean lastHadPipes = true;
	public PumpjackTileEntity(BlockEntityType<PumpjackTileEntity> type, BlockPos pos, BlockState state){
		super(PumpjackMultiblock.INSTANCE, 16000, true, type, pos, state);
	}
	
	public boolean canExtract(){
		return true;
	}
	
	public int getFluidAmount(){
		return PumpjackHandler.getFluidAmount(this.level, getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
	}
	
	public Fluid getFluidType(){
		return PumpjackHandler.getFluid(this.level, getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
	}
	
	public int getResidualFluid(){
		return PumpjackHandler.getResidualFluid(this.level, getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
	}
	
	public void extractFluid(int amount){
		PumpjackHandler.depleteFluid(this.level, getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4, amount);
	}
	
	private boolean hasPipes(){
		if(IPServerConfig.EXTRACTION.required_pipes.get()){
			BlockPos basePos = getBlockPosForPos(Down_Port);
			for(int y = basePos.getY() - 1;y > 0;y--){
				BlockPos pos = new BlockPos(basePos.getX(), y, basePos.getZ());
				BlockState state = this.level.getBlockState(pos);
				
				if(state.getBlock() == Blocks.BEDROCK)
					return true;
				
				if(state.getBlock() != MetalDevices.FLUID_PIPE.get()) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket){
		super.readCustomNBT(nbt, descPacket);
		boolean lastActive = this.wasActive;
		this.wasActive = nbt.getBoolean("wasActive");
		this.lastHadPipes = nbt.getBoolean("lastHadPipes");
		if(!wasActive && lastActive){
			this.activeTicks++;
		}
	}
	
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket){
		super.writeCustomNBT(nbt, descPacket);
		nbt.putBoolean("wasActive", this.wasActive);
		nbt.putBoolean("lastHadPipes", this.lastHadPipes);
	}

	@Override
	public void tickClient() {
		boolean active = false;

		int consumption = IPServerConfig.EXTRACTION.pumpjack_consumption.get();
		int extracted = this.energyStorage.extractEnergy(IPServerConfig.EXTRACTION.pumpjack_consumption.get(), true);

		if(extracted >= consumption && canExtract()){
			if((getBlockPos().getX() + getBlockPos().getZ()) % IPServerConfig.EXTRACTION.pipe_check_ticks.get() == this.pipeTicks){
				this.lastHadPipes = hasPipes();
			}

			if(!isRSDisabled() && this.lastHadPipes){
				int available = getFluidAmount();
				int residual = getResidualFluid();
				if(available > 0 || residual > 0){
					int oilAmnt = available <= 0 ? residual : available;

					FluidStack out = new FluidStack(getFluidType(), Math.min(IPServerConfig.EXTRACTION.pumpjack_speed.get(), oilAmnt));
					Direction facing = getIsMirrored() ? getFacing().getCounterClockWise() : getFacing().getClockWise();
					BlockPos outputPos = getBlockPosForPos(East_Port).offset(facing.getNormal());
					IFluidHandler output = FluidUtil.getFluidHandler(this.level, outputPos, facing.getOpposite()).orElse(null);
					if(output != null){
						int accepted = output.fill(out, FluidAction.SIMULATE);
						if(accepted > 0){
							int drained = output.fill(Utils.copyFluidStackWithAmount(out, Math.min(out.getAmount(), accepted), false), FluidAction.EXECUTE);
							extractFluid(drained);
							active = true;
							out = Utils.copyFluidStackWithAmount(out, out.getAmount() - drained, false);
						}
					}

					facing = getIsMirrored() ? getFacing().getClockWise() : getFacing().getCounterClockWise();
					outputPos = getBlockPosForPos(West_Port).offset(facing.getNormal());
					output = FluidUtil.getFluidHandler(this.level, outputPos, facing.getOpposite()).orElse(null);
					if(output != null){
						int accepted = output.fill(out, FluidAction.SIMULATE);
						if(accepted > 0){
							int drained = output.fill(Utils.copyFluidStackWithAmount(out, Math.min(out.getAmount(), accepted), false), FluidAction.EXECUTE);
							extractFluid(drained);
							active = true;
						}
					}

					if(active){
						this.energyStorage.extractEnergy(consumption, false);
					}
					this.activeTicks++;
				}
			}
			this.pipeTicks = (this.pipeTicks + 1) % IPServerConfig.EXTRACTION.pipe_check_ticks.get();
		}

		if(active != this.wasActive){
			this.setChanged();
			this.markContainingBlockForUpdate(null);
		}

		this.wasActive = active;
	}

	@Override
	public void tickServer(){
		if((!this.level.isClientSide || isDummy()) && this.wasActive){
			this.activeTicks++;
			return;
		}
	}
	
	@Override
	public Set<MultiblockFace> getEnergyPos(){
		return Energy_IN;
	}
	
/*	@Override
	public IOSideConfig getEnergySideConfig(Direction facing){
		if(this.formed && this.isEnergyPos(facing) && (facing == null || facing == Direction.UP))
			return IOSideConfig.INPUT;
		
		return IOSideConfig.NONE;
	}*/
	
	@Override
	public Set<BlockPos> getRedstonePos(){
		return Redstone_IN;
	}
	
	@Override
	public boolean isInWorldProcessingMachine(){
		return false;
	}
	
	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<MultiblockRecipe> process){
		return false;
	}

	@Override
	public void doProcessOutput(ItemStack output) {

	}

	@Override
	public void doProcessFluidOutput(FluidStack output) {

	}

	@Override
	public void onProcessFinish(MultiblockProcess<MultiblockRecipe> process) {

	}

	@Override
	public float getMinProcessDistance(MultiblockProcess<MultiblockRecipe> process){
		return 0;
	}
	
	@Override
	public int getMaxProcessPerTick(){
		return 1;
	}
	
	@Override
	public int getProcessQueueMaxLength(){
		return 1;
	}
	
	@Override
	public boolean isStackValid(int slot, ItemStack stack){
		return true;
	}
	
	@Override
	public int getSlotLimit(int slot){
		return 64;
	}
	
	@Override
	public int[] getOutputSlots(){
		return null;
	}
	
	@Override
	public int[] getOutputTanks(){
		return new int[]{1};
	}
	
	@Override
	public void doGraphicalUpdates(){
		this.setChanged();
		this.markContainingBlockForUpdate(null);
	}
	
	@Override
	public MultiblockRecipe findRecipeForInsertion(ItemStack inserting){
		return null;
	}
	
	@Override
	protected MultiblockRecipe getRecipeForId(ResourceLocation id){
		return null;
	}
	
	@Override
	public NonNullList<ItemStack> getInventory(){
		return null;
	}
	
	@Override
	public IFluidTank[] getInternalTanks(){
		return null;
	}

	private static final Set<MultiblockFace> outputOffset = ImmutableSet.of(
			new MultiblockFace(2, 0, 2, RelativeBlockFace.LEFT),
			new MultiblockFace(0, 0, 2, RelativeBlockFace.RIGHT),
			new MultiblockFace(1, 0, 0, RelativeBlockFace.DOWN)
	);
	private final MultiblockCapability<IFluidHandler> fluidOutput = MultiblockCapability.make(
			this, be -> be.fluidOutput, PumpjackTileEntity::master, registerFluidOutput(fakeTank)
	);
	private final MultiblockCapability<IFluidHandler> allFluids = MultiblockCapability.make(
			this, be -> be.allFluids, PumpjackTileEntity::master, registerFluidView(fakeTank)
	);

	@Nonnull
	@Override
	public <C> LazyOptional<C> getCapability(@Nonnull Capability<C> capability, @Nullable Direction side)
	{
		if(capability== CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
		{
			if(side==null) {
				return allFluids.getAndCast();
			}

			MultiblockFace relativeFace = asRelativeFace(side);
			if(outputOffset.contains(relativeFace)) {
				return fluidOutput.getAndCast();
			}
		}
		return super.getCapability(capability, side);
	}
	
	private static CachedShapesWithTransform<BlockPos, Pair<Direction, Boolean>> SHAPES = CachedShapesWithTransform.createForMultiblock(PumpjackTileEntity::getShape);

	@Override
	public VoxelShape getBlockBounds(CollisionContext ctx){
		return SHAPES.get(this.posInMultiblock, Pair.of(getFacing(), getIsMirrored()));
	}
	
	private static List<AABB> getShape(BlockPos posInMultiblock){
		final int bX = posInMultiblock.getX();
		final int bY = posInMultiblock.getY();
		final int bZ = posInMultiblock.getZ();
		
		// Most of the arm doesnt need collision. Dumb anyway.
		if((bY == 3 && bX == 1 && bZ != 2) || (bX == 1 && bY == 2 && bZ == 0)){
			return new ArrayList<>();
		}
		
		// Motor
		if(bY < 3 && bX == 1 && bZ == 4){
			List<AABB> list = new ArrayList<>();
			if(bY == 2){
				list.add(new AABB(0.25, 0.0, 0.0, 0.75, 0.25, 1.0));
			}else{
				list.add(new AABB(0.25, 0.0, 0.0, 0.75, 1.0, 1.0));
			}
			if(bY == 0){
				list.add(new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
			}
			return list;
		}
		
		// Support
		if(bZ == 2 && bY > 0){
			if(bX == 0){
				if(bY == 1){
					List<AABB> list = new ArrayList<>();
					list.add(new AABB(0.6875, 0.0, 0.0, 1.0, 1.0, 0.25));
					list.add(new AABB(0.6875, 0.0, 0.75, 1.0, 1.0, 1.0));
					return list;
				}
				if(bY == 2){
					List<AABB> list = new ArrayList<>();
					list.add(new AABB(0.8125, 0.0, 0.0, 1.0, 0.5, 1.0));
					list.add(new AABB(0.8125, 0.5, 0.25, 1.0, 1.0, 0.75));
					return list;
				}
				if(bY == 3){
					return Arrays.asList(new AABB(0.9375, 0.0, 0.375, 1.0, 0.125, 0.625));
				}
			}
			if(bX == 1 && bY == 3){
				return Arrays.asList(new AABB(0.0, -0.125, 0.375, 1.0, 0.125, 0.625));
			}
			if(bX == 2){
				if(bY == 1){
					List<AABB> list = new ArrayList<>();
					list.add(new AABB(0.0, 0.0, 0.0, 0.3125, 1.0, 0.25));
					list.add(new AABB(0.0, 0.0, 0.75, 0.3125, 1.0, 1.0));
					return list;
				}
				if(bY == 2){
					List<AABB> list = new ArrayList<>();
					list.add(new AABB(0.0, 0.0, 0.0, 0.1875, 0.5, 1.0));
					list.add(new AABB(0.0, 0.5, 0.25, 0.1875, 1.0, 0.75));
					return list;
				}
				if(bY == 3){
					return Arrays.asList(new AABB(0.0, 0.0, 0.375, 0.0625, 0.125, 0.625));
				}
			}
		}
		
		// Redstone Controller
		if(bX == 0 && bZ == 5){
			if(bY == 0){ // Bottom
				return Arrays.asList(
						new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
						new AABB(0.75, 0.0, 0.625, 0.875, 1.0, 0.875),
						new AABB(0.125, 0.0, 0.625, 0.25, 1.0, 0.875)
				);
			}
			if(bY == 1){ // Top
				return Arrays.asList(new AABB(0.0, 0.0, 0.5, 1.0, 1.0, 1.0));
			}
		}
		
		// Below the power-in block, base height
		if(bX == 2 && bY == 0 && bZ == 5){
			return Arrays.asList(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
		}
		
		// Misc
		if(bY == 0){
			
			// Legs Bottom Front
			if(bZ == 1 && (bX == 0 || bX == 2)){
				List<AABB> list = new ArrayList<>();
				
				list.add(new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
				
				if(bX == 0){
					list.add(new AABB(0.5, 0.5, 0.5, 1.0, 1.0, 1.0));
				}
				if(bX == 2){
					list.add(new AABB(0.0, 0.5, 0.5, 0.5, 1.0, 1.0));
				}
				
				return list;
			}
			
			// Legs Bottom Back
			if(bZ == 3 && (bX == 0 || bX == 2)){
				List<AABB> list = new ArrayList<>();
				
				list.add(new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
				
				if(bX == 0){
					list.add(new AABB(0.5, 0.5, 0.0, 1.0, 1.0, 0.5));
				}
				if(bX == 2){
					list.add(new AABB(0.0, 0.5, 0.0, 0.5, 1.0, 0.5));
				}
				
				return list;
			}
			
			// Fluid Outputs
			if(bZ == 2 && (bX == 0 || bX == 2)){
				return Arrays.asList(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
			}
			
			if(bX == 1){
				// Well
				if(bZ == 0){
					return Arrays.asList(new AABB(0.3125, 0.5, 0.8125, 0.6875, 0.875, 1.0), new AABB(0.1875, 0, 0.1875, 0.8125, 1.0, 0.8125));
				}
				
				// Pipes
				if(bZ == 1){
					return Arrays.asList(
							new AABB(0.3125, 0.5, 0.0, 0.6875, 0.875, 1.0),
							new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)
					);
				}
				if(bZ == 2){
					return Arrays.asList(
							new AABB(0.3125, 0.5, 0.0, 0.6875, 0.875, 0.6875),
							new AABB(0.0, 0.5, 0.3125, 1.0, 0.875, 0.6875),
							new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)
					);
				}
			}
			
			return Arrays.asList(new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
		}
		
		return Arrays.asList(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
	}
}
