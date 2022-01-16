package flaxbeard.immersivepetroleum.api.event;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * @deprecated Use
 *             {@link flaxbeard.immersivepetroleum.api.event.ProjectorEvent.PlaceBlock}
 */
@Deprecated
@Cancelable
public class SchematicPlaceBlockEvent extends Event {
	@Deprecated
	public SchematicPlaceBlockEvent(IMultiblock multiblock, Level world, BlockPos worldPos, BlockPos templatePos, BlockState state, CompoundTag nbt, Rotation rotation){
	}
	
	@Deprecated
	public void setBlockState(BlockState state){
	}
	
	@Deprecated
	public Level getLevel(){
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
	public BlockState getState(){
		return null;
	}
	
	@Deprecated
	public CompoundTag getNBT(){
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
