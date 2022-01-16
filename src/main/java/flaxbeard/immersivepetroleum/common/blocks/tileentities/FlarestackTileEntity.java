package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import flaxbeard.immersivepetroleum.api.crafting.FlarestackHandler;
import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.particle.IPParticleTypes;
import flaxbeard.immersivepetroleum.common.util.sounds.IPSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class FlarestackTileEntity extends IPTileEntityBase implements IEBlockInterfaces.ISoundBE {
	static final DamageSource FLARESTACK = new DamageSource("ipFlarestack").bypassArmor().setIsFire();
	
	protected boolean isRedstoneInverted;
	protected boolean isActive;
	protected short drained;
	protected FluidTank tank = new FluidTank(250, fstack -> (fstack != FluidStack.EMPTY && FlarestackHandler.isBurnable(fstack)));
	
	public FlarestackTileEntity(BlockPos pos, BlockState state){
		super(IPTileTypes.FLARE.get(), pos, state);
	}
	
	public void invertRedstone(){
		this.isRedstoneInverted = !this.isRedstoneInverted;
		this.setChanged();
	}
	
	public boolean isRedstoneInverted(){
		return this.isRedstoneInverted;
	}
	
	public boolean isActive(){
		return this.isActive;
	}
	
	public short getFlow(){
		return this.drained;
	}
	
	@Override
	public void readCustom(CompoundTag nbt){
		this.isRedstoneInverted = nbt.getBoolean("inverted");
		this.isActive = nbt.getBoolean("active");
		this.drained = nbt.getShort("drained");
		this.tank.readFromNBT(nbt.getCompound("tank"));
	}
	
	@Override
	public void writeCustom(CompoundTag nbt){
		nbt.putBoolean("inverted", this.isRedstoneInverted);
		nbt.putBoolean("active", this.isActive);
		nbt.putShort("drained", this.drained);
		
		CompoundTag tank = this.tank.writeToNBT(new CompoundTag());
		nbt.put("tank", tank);
	}
	
	private LazyOptional<IFluidHandler> inputHandler;
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side){
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && (side == null || side == Direction.DOWN)){
			if(this.inputHandler == null){
				this.inputHandler = LazyOptional.of(() -> {
					BlockEntity te = this.level.getBlockEntity(getBlockPos());
					if(te != null && te instanceof FlarestackTileEntity){
						return ((FlarestackTileEntity) te).tank;
					}
					return null;
				});
			}
			return this.inputHandler.cast();
		}
		return super.getCapability(cap, side);
	}
	
	@Override
	public void remove(){
		super.remove();
		if(this.inputHandler != null){
			this.inputHandler.invalidate();
		}
	}
	
	@Override
	protected void invalidateCaps(){
		super.invalidateCaps();
		if(this.inputHandler != null){
			this.inputHandler.invalidate();
		}
	}
	
	@Override
	public void setChanged(){
		super.setChanged();
		
		BlockState state = this.level.getBlockState(this.getBlockPos());
		this.level.sendBlockUpdated(this.getBlockPos(), state, state, 3);
		this.level.updateNeighborsAt(this.getBlockPos(), state.getBlock());
	}
	
	@Override
	public void tick(){
		if(!this.level.isClientSide){
			ImmersiveEngineering.proxy.handleTileSound(IPSounds.FLARESTACK, this, this.isActive, 1.0F, 0.75F);
			if(this.isActive){
				if(this.level.getGameTime() % 2 == 0){
					float xPos = (this.getBlockPos().getX() + 0.50F) + (this.level.random.nextFloat() - 0.5F) * .4375F;
					float zPos = (this.getBlockPos().getZ() + 0.50F) + (this.level.random.nextFloat() - 0.5F) * .4375F;
					float yPos = (this.getBlockPos().getY() + 1.875F) + (0.2F * this.level.random.nextFloat());
					
					this.level.addParticle(IPParticleTypes.FLARE_FIRE.get(), xPos, yPos, zPos, 0.0, 0.0625F + (this.drained / (float) this.tank.getCapacity() * 0.125F), 0.0);
				}
				
			}else if(this.level.getGameTime() % 5 == 0){
				float xPos = this.getBlockPos().getX() + 0.50F + (this.level.random.nextFloat() - 0.5F) * .4375F;
				float zPos = this.getBlockPos().getZ() + 0.50F + (this.level.random.nextFloat() - 0.5F) * .4375F;
				float yPos = this.getBlockPos().getY() + 1.6F;
				float xa = (this.level.random.nextFloat() - .5F) * .00625F;
				float ya = (this.level.random.nextFloat() - .5F) * .00625F;
				
				this.level.addParticle(ParticleTypes.FLAME, xPos, yPos, zPos, xa, 0.025F, ya);
			}
		}else{
			boolean lastActive = this.isActive;
			this.isActive = false;
			
			int redstone = this.level.getBestNeighborSignal(this.getBlockPos());
			if(this.isRedstoneInverted()){
				redstone = 15 - redstone;
			}
			
			if(redstone > 0 && this.tank.getFluidAmount() > 0){
				float signal = redstone / 15F;
				FluidStack fs = this.tank.drain((int) (this.tank.getCapacity() * signal), FluidAction.SIMULATE);
				if(fs.getAmount() > 0){
					this.tank.drain(fs.getAmount(), FluidAction.EXECUTE);
					this.drained = (short) fs.getAmount();
					this.isActive = true;
				}
			}
			
			if(this.isActive && this.level.getGameTime() % 10 == 0){
				// Set *anything* ablaze that's in the danger zone
				BlockPos min = this.getBlockPos().offset(-1, 2, -1);
				BlockPos max = min.offset(3, 3, 3);
				List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, new AABB(min, max));
				if(!list.isEmpty()){
					list.forEach(e -> {
						if(!e.fireImmune()){
							e.setSecondsOnFire(15);
							e.hurt(FLARESTACK, 6.0F * (this.drained / (float) this.tank.getCapacity()));
						}
					});
				}
			}
			
			if(lastActive != this.isActive || (this.level.isClientSide && this.isActive)){
				setChanged();
			}
		}
	}
	
	@Override
	public boolean shouldPlaySound(String sound){
		return true;
	}
}
