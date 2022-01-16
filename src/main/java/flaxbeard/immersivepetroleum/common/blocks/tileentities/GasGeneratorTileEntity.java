package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEEnums.IOSideConfig;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.immersiveflux.FluxStorage;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.impl.ImmersiveConnectableBlockEntity;
import blusunrize.immersiveengineering.api.wires.localhandlers.EnergyTransferHandler;
import blusunrize.immersiveengineering.api.wires.utils.WireUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.PlacementLimitation;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
import blusunrize.immersiveengineering.common.util.EnergyHelper.IEForgeEnergyWrapper;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableList;
import flaxbeard.immersivepetroleum.api.energy.FuelHandler;
import flaxbeard.immersivepetroleum.common.IPTileTypes;
import net.minecraft.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GasGeneratorTileEntity extends ImmersiveConnectableBlockEntity implements IEBlockInterfaces.IDirectionalBE, IEBlockInterfaces.IPlayerInteraction, IEBlockInterfaces.IBlockOverlayText, IEBlockInterfaces.IBlockEntityDrop, IEBlockInterfaces.ISoundBE, EnergyTransferHandler.EnergyConnector{
	public static final int FLUX_CAPACITY = 8000;
	
	protected WireType wireType;
	protected boolean isActive = false;
	protected Direction facing = Direction.NORTH;
	protected FluxStorage energyStorage = new FluxStorage(getMaxStorage(), Integer.MAX_VALUE, getMaxOutput());
	protected FluidTank tank = new FluidTank(FLUX_CAPACITY, fluid -> (fluid != null && fluid != FluidStack.EMPTY && FuelHandler.isValidFuel(fluid.getFluid())));
	
	public GasGeneratorTileEntity(BlockPos pos, BlockState state){
		super(IPTileTypes.GENERATOR.get(), pos, state);
	}
	
	public int getMaxOutput(){
		return IEServerConfig.MACHINES.lvCapConfig.output.getAsInt();
	}
	
	private int getMaxStorage(){
		return IEServerConfig.MACHINES.lvCapConfig.storage.getAsInt();
	}
	
	@Override
	public void read(BlockState state, CompoundTag nbt){
		super.read(state, nbt);
		
		this.isActive = nbt.getBoolean("isActive");
		this.tank.readFromNBT(nbt.getCompound("tank"));
		this.energyStorage.readFromNBT(nbt.getCompound("buffer"));
		this.wireType = nbt.contains("wiretype") ? WireUtils.getWireTypeFromNBT(nbt, "wiretype") : null;
	}
	
	@Override
	public CompoundTag write(CompoundTag compound){
		CompoundTag nbt = super.write(compound);
		
		nbt.putBoolean("isActive", this.isActive);
		nbt.put("tank", this.tank.writeToNBT(new CompoundTag()));
		nbt.put("buffer", this.energyStorage.writeToNBT(new CompoundTag()));
		
		if(this.wireType != null){
			nbt.putString("wiretype", this.wireType.getUniqueName());
		}
		
		return nbt;
	}
	
	@Override
	public SUpdateTileEntityPacket getUpdatePacket(){
		return new SUpdateTileEntityPacket(this.pos, 3, getUpdateTag());
	}
	
	@Override
	public void handleUpdateTag(BlockState state, CompoundTag tag){
		read(state, tag);
	}
	
	@Override
	public CompoundTag getUpdateTag(){
		return write(new CompoundTag());
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt){
		read(getBlockState(), pkt.getNbtCompound());
	}
	
	@Override
	public void readOnPlacement(LivingEntity placer, ItemStack stack){
		if(stack.hasTag()){
			CompoundTag nbt = stack.getOrCreateTag();
			
			this.tank.readFromNBT(nbt.getCompound("tank"));
			this.energyStorage.readFromNBT(nbt.getCompound("energy"));
		}
	}
	
	@Override
	public void setChanged(){
		super.setChanged();
		
		BlockState state = level.getBlockState(getPosition());
		level.notifyBlockUpdate(getPosition(), state, state, 3);
		level.notifyNeighborsOfStateChange(getPosition(), state.getBlock());
	}
	
	@Override
	public List<ItemStack> getTileDrops(LootContext context){
		ItemStack stack;
		if(context != null){
			stack = new ItemStack(context.get(LootContextParams.BLOCK_STATE).getBlock());
		}else{
			stack = new ItemStack(getBlockState().getBlock());
		}
		
		CompoundTag nbt = new CompoundTag();
		
		if(this.tank.getFluidAmount() > 0){
			CompoundTag tankNbt = this.tank.writeToNBT(new CompoundTag());
			nbt.put("tank", tankNbt);
		}
		
		if(this.energyStorage.getEnergyStored() > 0){
			CompoundTag energyNbt = this.energyStorage.writeToNBT(new CompoundTag());
			nbt.put("energy", energyNbt);
		}
		
		if(!nbt.isEmpty())
			stack.setTag(nbt);
		return ImmutableList.of(stack);
	}
	
	@Override
	public int getAvailableEnergy(){
		return Math.min(getMaxOutput(), this.energyStorage.getEnergyStored());
	}
	
	@Override
	public void extractEnergy(int amount){
		this.energyStorage.extractEnergy(amount, false);
	}
	
	@Override
	public boolean isSource(ConnectionPoint cp){
		return true;
	}
	
	@Override
	public boolean isSink(ConnectionPoint cp){
		return false;
	}
	
	@Override
	public boolean shouldPlaySound(String sound){
		return this.isActive;
	}
	
	@Override
	public FluxStorage getFluxStorage(){
		return this.energyStorage;
	}
	
	@Override
	public IOSideConfig getEnergySideConfig(Direction facing){
		return IOSideConfig.OUTPUT;
	}
	
	IEForgeEnergyWrapper energyWrapper;
	@Override
	public IEForgeEnergyWrapper getCapabilityWrapper(Direction facing){
		if(facing != this.facing)
			return null;
		
		if(this.energyWrapper == null || this.energyWrapper.side != this.facing)
			this.energyWrapper = new IEForgeEnergyWrapper(this, this.facing);
		
		return this.energyWrapper;
	}
	
	private LazyOptional<IFluidHandler> fluidHandler;
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side){
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && (side == null || side == Direction.UP)){
			if(this.fluidHandler == null){
				fluidHandler = LazyOptional.of(() -> this.tank);
			}
			return this.fluidHandler.cast();
		}
		return super.getCapability(cap, side);
	}
	
	@Override
	protected void invalidateCaps(){
		super.invalidateCaps();
		if(this.fluidHandler != null)
			this.fluidHandler.invalidate();
	}
	
	@Override
	public void remove(){
		super.remove();
		if(this.fluidHandler != null)
			this.fluidHandler.invalidate();
	}
	
	@Override
	public Component[] getOverlayText(Player player, HitResult mop, boolean hammer){
		if(Utils.isFluidRelatedItemStack(player.getItemInHand(InteractionHand.MAIN_HAND))){
			Component s = null;
			if(tank.getFluid().getAmount() > 0)
				s = ((MutableComponent) tank.getFluid().getDisplayName()).append(": " + tank.getFluidAmount() + "mB");
			else
				s = new TranslatableComponent(Lib.GUI + "empty");
			return new Component[]{s};
		}
		return null;
	}
	
	@Override
	public boolean useNixieFont(Player player, HitResult mop){
		return false;
	}
	
	@Override
	public boolean interact(Direction side, Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ){
		if(FluidUtil.interactWithFluidHandler(player, hand, tank)){
			setChanged();
			return true;
		}else if(player.isShiftKeyDown()){
			boolean added = false;
			if(player.inventory.getCurrentItem().isEmpty()){
				added = true;
				player.inventory.setInventorySlotContents(player.inventory.currentItem, getTileDrops(null).get(0));
			}else{
				added = player.inventory.addItemStackToInventory(getTileDrops(null).get(0));
			}
			
			if(added){
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
			return true;
		}
		return false;
	}
	
	@Override
	public Direction getFacing(){
		return this.facing;
	}
	
	@Override
	public void setFacing(Direction facing){
		this.facing = facing;
	}
	
	@Override
	public PlacementLimitation getFacingLimitation(){
		return PlacementLimitation.HORIZONTAL;
	}
	
	@Override
	public boolean mirrorFacingOnPlacement(LivingEntity placer){
		return false;
	}
	
	@Override
	public boolean canHammerRotate(Direction side, Vector3d hit, LivingEntity entity){
		return true;
	}
	
	@Override
	public boolean canRotate(Direction axis){
		return true;
	}
	
	@Override
	public void tick(){
		if(this.level.isRemote){
			ImmersiveEngineering.proxy.handleTileSound(IESounds.dieselGenerator, this, this.isActive, .3f, .75f);
			if(this.isActive && this.level.getGameTime() % 4 == 0){
				Direction fl = this.facing;
				Direction fw = this.facing.getClockWise();
				
				Vector3i vec = fw.getOpposite().getDirectionVec();
				
				double x = this.pos.getX() + .5 + (fl.getXOffset() * 2 / 16F) + (-fw.getXOffset() * .6125f);
				double y = this.pos.getY() + .4;
				double z = this.pos.getZ() + .5 + (fl.getZOffset() * 2 / 16F) + (-fw.getZOffset() * .6125f);
				
				this.level.addParticle(this.level.rand.nextInt(10) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, x, y, z, vec.getX() * 0.025, 0, vec.getZ() * 0.025);
			}
		}else{
			boolean lastActive = this.isActive;
			this.isActive = false;
			if(!this.level.isBlockPowered(this.pos) && this.tank.getFluid() != null){
				Fluid fluid = this.tank.getFluid().getFluid();
				int amount = FuelHandler.getFuelUsedPerTick(fluid);
				if(amount > 0 && this.tank.getFluidAmount() >= amount && this.energyStorage.receiveEnergy(FuelHandler.getFluxGeneratedPerTick(fluid), false) > 0){
					this.tank.drain(new FluidStack(fluid, amount), FluidAction.EXECUTE);
					this.isActive = true;
				}
			}
			
			if(lastActive != this.isActive || (!this.level.isRemote && this.isActive)){
				setChanged();
			}
		}
	}
	
	@Override
	public void connectCable(WireType cableType, ConnectionPoint target, IImmersiveConnectable other, ConnectionPoint otherTarget){
		this.wireType = cableType;
		setChanged();
	}
	
	@Override
	public void removeCable(@Nullable Connection connection, ConnectionPoint attachedPoint){
		this.wireType = null;
		setChanged();
	}
	
	@Override
	public boolean canConnect(){
		return true;
	}
	
	@Override
	public boolean canConnectCable(WireType cableType, ConnectionPoint target, Vec3i offset){
		if(level.getBlockState(target.position()).getBlock() != level.getBlockState(getBlockPos()).getBlock()){
			return false;
		}
		
		return this.wireType == null && (cableType.getCategory().equals(WireType.LV_CATEGORY) || cableType.getCategory().equals(WireType.MV_CATEGORY));
	}
	
	@Override
	public BlockPos getConnectionMaster(@Nullable WireType cableType, TargetingInfo target){
		return getBlockPos();
	}
	
	@Override
	public ConnectionPoint getTargetedPoint(TargetingInfo info, Vec3i offset){
		return new ConnectionPoint(getBlockPos(), 0);
	}
	
	@Override
	public Collection<ConnectionPoint> getConnectionPoints(){
		return Arrays.asList(new ConnectionPoint(pos, 0));
	}
	
	@Override
	public Vec3 getConnectionOffset(ConnectionPoint here, ConnectionPoint other, WireType type){
		float xo = facing.getDirectionVec().getX() * .5f + .5f;
		float zo = facing.getDirectionVec().getZ() * .5f + .5f;
		return new Vec3(xo, .5f, zo);
	}
}
