package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.EventHandler;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.util.Utils;
import flaxbeard.immersivepetroleum.api.crafting.LubricantHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.blocks.metal.AutoLubricatorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Arrays;
import java.util.List;

public class AutoLubricatorTileEntity extends IPTileEntityBase implements IPlayerInteraction, IBlockOverlayText, IEBlockInterfaces.IBlockEntityDrop {
	public boolean isSlave;
	public boolean isActive;
	public boolean predictablyDraining = false;
	public Direction facing;
	public FluidTank tank = new FluidTank(8000, fluid -> (fluid != null && LubricantHandler.isValidLube(fluid.getFluid())));
	
	public AutoLubricatorTileEntity(BlockPos pos, BlockState state){
		super(IPTileTypes.AUTOLUBE.get(), pos, state);
	}
	
	@Override
	protected void readCustom(CompoundTag compound){
		this.isSlave = compound.getBoolean("slave");
		this.isActive = compound.getBoolean("active");
		this.predictablyDraining = compound.getBoolean("predictablyDraining");
		
		Direction facing = Direction.byName(compound.getString("facing"));
		if(facing.get2DDataValue() == -1)
			facing = Direction.NORTH;
		this.facing = facing;
		
		this.tank.readFromNBT(compound.getCompound("tank"));
	}
	
	@Override
	protected void writeCustom(CompoundTag compound){
		compound.putBoolean("slave", this.isSlave);
		compound.putBoolean("active", this.isActive);
		compound.putBoolean("predictablyDraining", this.predictablyDraining);
		compound.putString("facing", this.facing.getName());
		compound.putInt("count", this.count);
		
		CompoundTag tank = this.tank.writeToNBT(new CompoundTag());
		compound.put("tank", tank);
	}
	
	public void readTank(CompoundTag nbt){
		this.tank.readFromNBT(nbt.getCompound("tank"));
	}
	
	public void writeTank(CompoundTag nbt, boolean toItem){
		boolean write = this.tank.getFluidAmount() > 0;
		CompoundTag tankTag = this.tank.writeToNBT(new CompoundTag());
		if(!toItem || write)
			nbt.put("tank", tankTag);
	}
	
	public void readOnPlacement(LivingEntity placer, ItemStack stack){
		if(stack.hasTag()){
			readTank(stack.getTag());
		}
	}
	
	@Override
	public List<ItemStack> getBlockEntityDrop(LootContext context){
		BlockState state = context.getParam(LootContextParams.BLOCK_STATE);
		if(state.getValue(AutoLubricatorBlock.SLAVE)){
			return Arrays.asList(ItemStack.EMPTY);
		}
		
		ItemStack stack = new ItemStack(state.getBlock());
		
		BlockEntity te = context.getParam(LootContextParams.BLOCK_ENTITY);
		if(te instanceof AutoLubricatorTileEntity){
			AutoLubricatorTileEntity lube = (AutoLubricatorTileEntity) te;
			
			CompoundTag tag = new CompoundTag();
			lube.writeTank(tag, true);
			if(!tag.isEmpty()){
				stack.setTag(tag);
			}
		}
		
		return Arrays.asList(stack);
	}
	
	private LazyOptional<IFluidHandler> outputHandler;
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side){
		if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && this.isSlave && (side == null || side == Direction.UP)){
			if(this.outputHandler == null){
				this.outputHandler = LazyOptional.of(() -> {
					BlockEntity te = this.level.getBlockEntity(getBlockPos().relative(Direction.DOWN));
					if(te != null && te instanceof AutoLubricatorTileEntity){
						return ((AutoLubricatorTileEntity) te).tank;
					}
					return null;
				});
			}
			return this.outputHandler.cast();
		}
		
		return super.getCapability(cap, side);
	}
	
	@Override
	public void remove(){
		super.remove();
		if(this.outputHandler != null)
			this.outputHandler.invalidate();
	}
	
	@Override
	public void setChanged(){
		super.setChanged();
		
		BlockState state = level.getBlockState(getBlockPos());
		level.sendBlockUpdated(getBlockPos(), state, state, 3);
		level.updateNeighborsAt(getBlockPos(), state.getBlock());
	}
	
	@Override
	protected void invalidateCaps(){
		super.invalidateCaps();
		if(this.outputHandler != null)
			this.outputHandler.invalidate();
	}
	
	public Direction getFacing(){
		return this.facing;
	}
	
	public boolean isMaster(){
		return !this.isSlave;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public AABB getRenderBoundingBox(){
		BlockPos pos = getBlockPos();
		int size = 3;
		return new AABB(pos.getX() - size, pos.getY() - size, pos.getZ() - size, pos.getX() + size, pos.getY() + size, pos.getZ() + size);
	}
	
	@Override
	public Component[] getOverlayText(Player player, HitResult mop, boolean hammer){
		if(Utils.isFluidRelatedItemStack(player.getItemInHand(InteractionHand.MAIN_HAND))){
			BlockEntity master = this.level.getBlockEntity(getBlockPos().offset(0, this.isSlave ? -1 : 0, 0));
			if(master != null && master instanceof AutoLubricatorTileEntity){
				AutoLubricatorTileEntity lube = (AutoLubricatorTileEntity) master;
				MutableComponent s = null;
				if(!lube.tank.isEmpty()){
					s = ((MutableComponent) lube.tank.getFluid().getDisplayName()).append(": " + lube.tank.getFluidAmount() + "mB");
				}else{
					s = new TranslatableComponent(Lib.GUI + "empty");
				}
				return new Component[]{s};
			}
		}
		return null;
	}
	
	@Override
	public boolean useNixieFont(Player player, HitResult mop){
		return false;
	}
	
	@Override
	public boolean interact(Direction side, Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ){
		BlockEntity master = this.isSlave ? this.level.getBlockEntity(getBlockPos().offset(0, -1, 0)) : this;
		if(master != null && master instanceof AutoLubricatorTileEntity){
			if(getLevel().isClientSide && FluidUtil.interactWithFluidHandler(player, hand, ((AutoLubricatorTileEntity) master).tank)){
				setChanged();
			}
			return true;
		}
		return false;
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public double getMaxRenderDistanceSquared(){
		return 1024.0D;// super.getMaxRenderDistanceSquared();
	}
	
	int count = 0;
	int lastTank = 0;
	int lastTankUpdate = 0;
	int countClient = 0;
	
	@Override
	public void tick(){
		if(this.isSlave){
			// See ApiUtils.checkForNeedlessTicking(te);
			EventHandler.REMOVE_FROM_TICKING.add(this);
			return;
		}
		
		if(isMaster()){
			if((this.tank.getFluid() != null && this.tank.getFluid() != FluidStack.EMPTY) && this.tank.getFluidAmount() >= LubricantHandler.getLubeAmount(this.tank.getFluid().getFluid()) && LubricantHandler.isValidLube(this.tank.getFluid().getFluid())){
				BlockPos target = this.getBlockPos().offset(this.facing.getNormal());
				BlockEntity te = this.level.getBlockEntity(target);
				
				LubricatedHandler.ILubricationHandler<BlockEntity> handler = LubricatedHandler.getHandlerForTile(te);
				if(handler != null){
					BlockEntity master = handler.isPlacedCorrectly(this.level, this, this.facing);
					if(master != null && handler.isMachineEnabled(this.level, master)){
						this.count++;
						handler.lubricate(this.level, this.count, master);
						
						if(this.level.isClientSide && this.count % 4 == 0){
							this.tank.drain(LubricantHandler.getLubeAmount(this.tank.getFluid().getFluid()), FluidAction.EXECUTE);
							setChanged();
						}
						
						this.countClient++;
						if(this.countClient % 50 == 0){
							this.countClient = this.level.random.nextInt(40);
							handler.spawnLubricantParticles(this.level, this, this.facing, master);
						}
					}
				}
			}
			
			if(this.level.isClientSide && this.lastTank != this.tank.getFluidAmount()){
				if(this.predictablyDraining && !this.tank.isEmpty() && this.lastTank - this.tank.getFluidAmount() == LubricantHandler.getLubeAmount(this.tank.getFluid().getFluid())){
					this.lastTank = this.tank.getFluidAmount();
				}
				
				if(Math.abs(this.lastTankUpdate - this.tank.getFluidAmount()) > 25){
					this.predictablyDraining = !this.tank.isEmpty() && this.lastTank - this.tank.getFluidAmount() == LubricantHandler.getLubeAmount(this.tank.getFluid().getFluid());
					this.lastTankUpdate = this.tank.getFluidAmount();
				}
				setChanged();
			}
		}
	}
}
