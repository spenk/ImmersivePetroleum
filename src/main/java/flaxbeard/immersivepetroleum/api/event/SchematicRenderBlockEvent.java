package flaxbeard.immersivepetroleum.api.event;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * @deprecated Use
 *             {@link flaxbeard.immersivepetroleum.api.event.ProjectorEvent.RenderBlock}
 */
@Deprecated
@Cancelable
public class SchematicRenderBlockEvent extends Event {
	
	@Deprecated
	public SchematicRenderBlockEvent(IMultiblock multiblock, Level world, BlockPos worldPos, BlockPos templatePos, BlockState state, CompoundTag nbt, Rotation rotation){
	}

	@Deprecated
	public void setState(BlockState state){
	}
	
	@Deprecated
	public void setBlock(Block block){
	}
	
	@Deprecated
	public Level getWorld(){
		return null;
	}
	
	@Deprecated
	public IMultiblock getMultiblock(){
		return null;
	}
	
	@Deprecated
	public Rotation getRotate(){
		return null;
	}
	
	@Deprecated
	public BlockPos getWorldPos(){
		return null;
	}
	
	@Deprecated
	public BlockPos getTemplatePos(){
		return null;
	}
	
	@Deprecated
	public Block getBlock(){
		return null;
	}
	
	@Deprecated
	public BlockState getState(){
		return null;
	}
	
	@Deprecated
	public CompoundTag getNBT(){
		return null;
	}
	
	/** Replaced by {@link SchematicRenderBlockEvent#setState(BlockState)} */
	@Deprecated
	public void setItemStack(ItemStack itemStack){
	}
	
	@Deprecated
	public ItemStack getItemStack(){
		return null;
	}
	
	@Deprecated
	public int getIndex(){
		return 0;
	}
	
	@Deprecated
	public int getL(){
		return 0;
	}
	
	@Deprecated
	public int getH(){
		return 0;
	}
	
	@Deprecated
	public int getW(){
		return 0;
	}
}
