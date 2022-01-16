package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import blusunrize.immersiveengineering.api.IEEnums.IOSideConfig;
import blusunrize.immersiveengineering.api.utils.shapes.CachedShapesWithTransform;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessInMachine;
import blusunrize.immersiveengineering.common.register.IEContainerTypes;
import blusunrize.immersiveengineering.common.util.orientation.RelativeBlockFace;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import flaxbeard.immersivepetroleum.api.crafting.SulfurRecoveryRecipe;
import flaxbeard.immersivepetroleum.common.multiblocks.HydroTreaterMultiblock;
import flaxbeard.immersivepetroleum.common.util.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HydrotreaterTileEntity extends PoweredMultiblockBlockEntity<HydrotreaterTileEntity, SulfurRecoveryRecipe> implements IInteractionObjectIE<HydrotreaterTileEntity>, IBlockBounds{
	/** Primary Fluid Input Tank<br> */
	public static final int TANK_INPUT_A = 0;
	
	/** Secondary Fluid Input Tank<br> */
	public static final int TANK_INPUT_B = 1;
	
	/** Output Fluid Tank<br> */
	public static final int TANK_OUTPUT = 2;

	/** Template-Location of the Fluid Input Port. (1 0 3)<br> */
	public static final BlockPos Fluid_IN_A = new BlockPos(1, 0, 3);
	
	/** Template-Location of the Fluid Input Port. (2 2 1)<br> */
	public static final BlockPos Fluid_IN_B = new BlockPos(2, 2, 1);
	
	/** Template-Location of the Fluid Output Port. (0 1 2)<br> */
	public static final BlockPos Fluid_OUT = new BlockPos(0, 1, 2);
	
	/** Template-Location of the Item Output Port. (0 0 2)<br> */
	public static final BlockPos Item_OUT = new BlockPos(0, 0, 2);
	
	/** Template-Location of the Energy Input Ports. (2 2 3)<br> */
	public static final Set<MultiblockFace> Energy_IN = ImmutableSet.of(new MultiblockFace(2, 2, 3, RelativeBlockFace.UP));
	
	/** Template-Location of the Redstone Input Port. (0 1 3)<br> */
	public static final Set<BlockPos> Redstone_IN = ImmutableSet.of(new BlockPos(0, 1, 3));
	
	
	public final FluidTank[] tanks = new FluidTank[]{new FluidTank(12000), new FluidTank(12000), new FluidTank(12000)};
	public HydrotreaterTileEntity(BlockEntityType<HydrotreaterTileEntity> type, BlockPos pos, BlockState state){
		super(HydroTreaterMultiblock.INSTANCE, 8000, true, type, pos, state);
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket){
		super.readCustomNBT(nbt, descPacket);

		this.tanks[TANK_INPUT_A].readFromNBT(nbt.getCompound("tank0"));
		this.tanks[TANK_INPUT_B].readFromNBT(nbt.getCompound("tank1"));
		this.tanks[TANK_OUTPUT].readFromNBT(nbt.getCompound("tank2"));
	}
	
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket){
		super.writeCustomNBT(nbt, descPacket);

		nbt.put("tank0", this.tanks[TANK_INPUT_A].writeToNBT(new CompoundTag()));
		nbt.put("tank1", this.tanks[TANK_INPUT_B].writeToNBT(new CompoundTag()));
		nbt.put("tank2", this.tanks[TANK_OUTPUT].writeToNBT(new CompoundTag()));
	}

	@Override
	protected SulfurRecoveryRecipe getRecipeForId(ResourceLocation id) {
		return null;
	}

	@Override
	public NonNullList<ItemStack> getInventory(){
		return null;
	}
	
	@Override
	public boolean isStackValid(int slot, ItemStack stack){
		return false;
	}
	
	@Override
	public int getSlotLimit(int slot){
		return 0;
	}
	
	@Override
	public void doGraphicalUpdates(){
		this.setChanged();
		this.markContainingBlockForUpdate(null);
	}
	
	@Override
	public Set<MultiblockFace> getEnergyPos(){
		return Energy_IN;
	}
	
	@Override
	public Set<BlockPos> getRedstonePos(){
		return Redstone_IN;
	}
	
	@Override
	public IOSideConfig getEnergySideConfig(Direction facing){
		if(this.formed && this.isEnergyPos() && (facing == null || facing == Direction.UP))
			return IOSideConfig.INPUT;
		
		return IOSideConfig.NONE;
	}
	
	@Override
	public IFluidTank[] getInternalTanks(){
		return this.tanks;
	}
	
	@Override
	public SulfurRecoveryRecipe findRecipeForInsertion(ItemStack inserting){
		return null;
	}
	
	@Override
	public int[] getOutputSlots(){
		return null;
	}
	
	@Override
	public int[] getOutputTanks(){
		return new int[]{TANK_OUTPUT};
	}

	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<SulfurRecoveryRecipe> process){
		int outputAmount = 0;
		for(FluidStack outputFluid:process.recipe.getFluidOutputs()){
			outputAmount += outputFluid.getAmount();
		}
		
		return this.tanks[TANK_OUTPUT].getCapacity() >= (this.tanks[TANK_OUTPUT].getFluidAmount() + outputAmount);
	}
	
	@Override
	public void doProcessOutput(ItemStack output){
		Direction outputdir = (getIsMirrored() ? getFacing().getClockWise() : getFacing().getCounterClockWise());
		BlockPos outputpos = getBlockPosForPos(Item_OUT).offset(outputdir.getNormal());
		
		BlockEntity te = level.getBlockEntity(outputpos);
		if(te != null){
			IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, outputdir.getOpposite()).orElse(null);
			if(handler != null){
				output = ItemHandlerHelper.insertItem(handler, output, false);
			}
		}
		
		if(!output.isEmpty()){
			double x = outputpos.getX() + 0.5;
			double y = outputpos.getY() + 0.25;
			double z = outputpos.getZ() + 0.5;
			
			Direction facing = getIsMirrored() ? getFacing().getOpposite() : getFacing();
			if(facing != Direction.EAST && facing != Direction.WEST){
				x = outputpos.getX() + (facing == Direction.SOUTH ? 0.15 : 0.85);
			}
			if(facing != Direction.NORTH && facing != Direction.SOUTH){
				z = outputpos.getZ() + (facing == Direction.WEST ? 0.15 : 0.85);
			}
			
			ItemEntity ei = new ItemEntity(level, x, y, z, output.copy());
			ei.setMotion(0.075 * outputdir.getXOffset(), 0.025, 0.075 * outputdir.getZOffset());
			level.addFreshEntity(ei);
		}
	}
	
	@Override
	public void doProcessFluidOutput(FluidStack output){
	}
	
	@Override
	public void onProcessFinish(MultiblockProcess<SulfurRecoveryRecipe> process){
	}
	
	@Override
	public void tick(){
		//checkForNeedlessTicking();
		
		if(!this.level.isClientSide || isDummy() || isRSDisabled()){
			return;
		}
		
		boolean update = false;
		
		if(this.energyStorage.getEnergyStored() > 0 && this.processQueue.size() < getProcessQueueMaxLength()){
			if(this.tanks[TANK_INPUT_A].getFluidAmount() > 0 || this.tanks[TANK_INPUT_B].getFluidAmount() > 0){
				SulfurRecoveryRecipe recipe = SulfurRecoveryRecipe.findRecipe(this.tanks[TANK_INPUT_A].getFluid(), this.tanks[TANK_INPUT_B].getFluid());
				
				if(recipe != null && this.energyStorage.getEnergyStored() >= recipe.getTotalProcessEnergy()){
					if(this.tanks[TANK_INPUT_A].getFluidAmount() >= recipe.getInputFluid().getAmount() && (recipe.getSecondaryInputFluid() == null || (this.tanks[TANK_INPUT_B].getFluidAmount() >= recipe.getSecondaryInputFluid().getAmount()))){
						int[] inputs, inputAmounts;
						
						if(recipe.getSecondaryInputFluid() != null){
							inputs = new int[]{TANK_INPUT_A, TANK_INPUT_B};
							inputAmounts = new int[]{recipe.getInputFluid().getAmount(), recipe.getSecondaryInputFluid().getAmount()};
						}else{
							inputs = new int[]{TANK_INPUT_A};
							inputAmounts = new int[]{recipe.getInputFluid().getAmount()};
						}
						
						MultiblockProcessInMachine<SulfurRecoveryRecipe> process = new MultiblockProcessInMachine<SulfurRecoveryRecipe>(recipe)
								.setInputTanks(inputs)
								.setInputAmounts(inputAmounts);
						if(addProcessToQueue(process, true)){
							addProcessToQueue(process, false);
							update = true;
						}
					}
				}
			}
		}
		
		if(!this.processQueue.isEmpty()){
			update = true;
		}
		
		super.tick();
		
		if(this.tanks[TANK_OUTPUT].getFluidAmount() > 0){
			BlockPos outPos = getBlockPosForPos(Fluid_OUT).above();
			update |= FluidUtil.getFluidHandler(this.level, outPos, Direction.DOWN).map(output -> {
				boolean ret = false;
				FluidStack target = this.tanks[TANK_OUTPUT].getFluid();
				target = FluidHelper.copyFluid(target, Math.min(target.getAmount(), 1000));
				
				int accepted = output.fill(target, FluidAction.SIMULATE);
				if(accepted > 0){
					int drained = output.fill(FluidHelper.copyFluid(target, Math.min(target.getAmount(), accepted)), FluidAction.EXECUTE);
					
					this.tanks[TANK_OUTPUT].drain(new FluidStack(target.getFluid(), drained), FluidAction.EXECUTE);
					ret |= true;
				}
				
				return ret;
			}).orElse(false);
		}
		
		if(update){
			updateMasterBlock(null, true);
		}
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
	public float getMinProcessDistance(MultiblockProcess<SulfurRecoveryRecipe> process){
		return 1.0F;
	}
	
	@Override
	public boolean isInWorldProcessingMachine(){
		return false;
	}
	
	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side){
		HydrotreaterTileEntity master = master();
		if(master != null){
			if(this.posInMultiblock.equals(Fluid_IN_A) && (side == null || side == getFacing().getOpposite())){
				return new IFluidTank[]{master.tanks[TANK_INPUT_A]};
			}
			if(this.posInMultiblock.equals(Fluid_IN_B) && (side == null || side == Direction.UP)){
				return new IFluidTank[]{master.tanks[TANK_INPUT_B]};
			}
			if(this.posInMultiblock.equals(Fluid_OUT) && (side == null || side == Direction.UP)){
				return new IFluidTank[]{master.tanks[TANK_OUTPUT]};
			}
		}
		return new IFluidTank[0];
	}
	
	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource){
		if(this.posInMultiblock.equals(Fluid_IN_A) && (side == null || side == getFacing().getOpposite())){
			HydrotreaterTileEntity master = master();
			
			if(master != null && master.tanks[TANK_INPUT_A].getFluidAmount() < master.tanks[TANK_INPUT_A].getCapacity()){
				if(master.tanks[TANK_INPUT_A].isEmpty()){
					return SulfurRecoveryRecipe.hasRecipeWithInput(resource, true);
				}else{
					return resource.isFluidEqual(master.tanks[TANK_INPUT_A].getFluid());
				}
			}
		}
		if(this.posInMultiblock.equals(Fluid_IN_B) && (side == null || side == Direction.UP)){
			HydrotreaterTileEntity master = master();
			
			if(master != null && master.tanks[TANK_INPUT_B].getFluidAmount() < master.tanks[TANK_INPUT_B].getCapacity()){
				if(master.tanks[TANK_INPUT_B].isEmpty()){
					return SulfurRecoveryRecipe.hasRecipeWithSecondaryInput(resource, true);
				}else{
					return resource.isFluidEqual(master.tanks[TANK_INPUT_B].getFluid());
				}
			}
		}
		return false;
	}
	
	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side){
		return false;
	}
	
	@Override
	public HydrotreaterTileEntity getGuiMaster(){
		return master();
	}

	@Override
	public IEContainerTypes.BEContainer getContainerType() {
		return null;
	}

	@Override
	public boolean canUseGui(Player player){
		return this.formed;
	}
	
	private static CachedShapesWithTransform<BlockPos, Pair<Direction, Boolean>> SHAPES = CachedShapesWithTransform.createForMultiblock(HydrotreaterTileEntity::getShape);
	
	@Override
	public VoxelShape getBlockBounds(CollisionContext ctx){
		return SHAPES.get(this.posInMultiblock, Pair.of(getFacing(), getIsMirrored()));
	}
	
	private static List<AABB> getShape(BlockPos posInMultiblock){
		int x = posInMultiblock.getX();
		int y = posInMultiblock.getY();
		int z = posInMultiblock.getZ();
		
		List<AABB> main = new ArrayList<>();
		
		// Baseplate
		if(y == 0 && !(x == 0 && z == 2) && !(z == 3 && (x == 1 || x == 2))){
			main.add(new AABB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
		}
		
		// Redstone Controller
		if(y == 0 && x == 0 && z == 3){
			main.add(new AABB(0.75, 0.5, 0.625, 0.875, 1.0, 0.875));
			main.add(new AABB(0.125, 0.5, 0.625, 0.25, 1.0, 0.875));
		}else if(y == 1 && x == 0 && z == 3){
			main.add(new AABB(0.0, 0.0, 0.5, 1.0, 1.0, 1.0));
		}
		
		// Small Tank
		if(x == 0){
			// Bottom half
			if(y == 0){
				if(z == 0){
					main.add(new AABB(0.125, 0.75, 0.5, 1.0, 1.0, 1.0));
					main.add(new AABB(0.25, 0.5, 0.75, 0.875, 0.75, 1.0));
				}
				if(z == 1){
					main.add(new AABB(0.125, 0.75, 0.0, 1.0, 1.0, 1.0));
				}
				if(z == 3){
					main.add(new AABB(0.125, 0.75, 0.0, 1.0, 1.0, 0.25));
				}
				
			}
			
			// Top half
			if(y == 1){
				if(z == 0){
					main.add(new AABB(0.125, 0.0, 0.5, 1.0, 0.75, 1.0));
				}
				if(z == 1){
					main.add(new AABB(0.125, 0.0, 0.0, 1.0, 0.75, 1.0));
				}
				if(z == 3){
					main.add(new AABB(0.125, 0.0, 0.0, 1.0, 0.75, 0.25));
				}
			}
		}
		
		// Big tank
		{
			// Support legs
			if(y == 0){
				if(z == 0){
					if(x == 1){
						main.add(new AABB(0.125, 0.3125, 0.0625, 0.375, 1.0, 0.3125));
					}
					if(x == 2){
						main.add(new AABB(0.625, 0.3125, 0.0625, 0.875, 1.0, 0.3125));
					}
				}
				if(z == 1){
					if(x == 1){
						main.add(new AABB(0.125, 0.3125, 0.875, 0.375, 1.0, 1.0));
					}
					if(x == 2){
						main.add(new AABB(0.625, 0.3125, 0.875, 0.875, 1.0, 1.0));
					}
				}
				if(z == 2 && x == 2){
					main.add(new AABB(0.625, 0.3125, 0.0, 0.875, 1.0, 0.125));
				}
			}
		}
		
		// Use default cube shape if nessesary
		if(main.isEmpty()){
			main.add(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
		}
		return main;
	}
}
