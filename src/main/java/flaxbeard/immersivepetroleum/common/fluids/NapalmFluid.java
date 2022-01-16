package flaxbeard.immersivepetroleum.common.fluids;

import flaxbeard.immersivepetroleum.common.CommonEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class NapalmFluid extends IPFluid {
	public NapalmFluid(){
		super("napalm", 1000, 4000);
	}
	
	@Override
	protected IPFluidBlock createFluidBlock() {
		return new IPFluidBlock(NapalmFluid.this.source, NapalmFluid.this.fluidName) {
			@Override
			public void onPlace(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving){
				for(Direction facing: Direction.values()){
					BlockPos notifyPos = pos.offset(facing.getNormal());
					if(worldIn.getBlockState(notifyPos).getBlock() instanceof FireBlock || worldIn.getBlockState(notifyPos).getMaterial() == Material.FIRE){
						worldIn.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
						break;
					}
				}
				super.onPlace(state, worldIn, pos, oldState, isMoving);
			}

			@Override
			public void neighborChanged(@NotNull BlockState state, Level worldIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving){
				if(worldIn.getBlockState(fromPos).getBlock() instanceof FireBlock || worldIn.getBlockState(fromPos).getMaterial() == Material.FIRE){
					ResourceLocation d = worldIn.dimension().getRegistryName();
					if(!CommonEventHandler.napalmPositions.containsKey(d) || !CommonEventHandler.napalmPositions.get(d).contains(fromPos)){
						processFire(worldIn, pos);
					}
				}

				super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
			}
		};
	}
	
	@Override
	public int getTickDelay(@NotNull LevelReader p_205569_1_){
		return 10;
	}
	
	public void processFire(Level world, BlockPos pos){
		ResourceLocation d = world.dimension().getRegistryName();
		if(!CommonEventHandler.napalmPositions.containsKey(d)){
			CommonEventHandler.napalmPositions.put(d, new ArrayList<>());
		}
		CommonEventHandler.napalmPositions.get(d).add(pos);
		
		world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
		
		for(Direction facing:Direction.values()){
			BlockPos notifyPos = pos.offset(facing.getNormal());
			Block block = world.getBlockState(notifyPos).getBlock();
			if(block == this.block){
				CommonEventHandler.napalmPositions.get(d).add(notifyPos);
			}
		}
	}
}
