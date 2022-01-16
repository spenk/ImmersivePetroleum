package flaxbeard.immersivepetroleum.common.blocks.metal;

import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.blocks.IPMetalMultiblock;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.CokerUnitTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class CokerUnitBlock extends IPMetalMultiblock<CokerUnitTileEntity>{
	public CokerUnitBlock(){
		super(IPTileTypes.COKER.get(), "cokerunit");
	}
	
	@Override
	public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit){
		if(!player.getItemInHand(hand).isEmpty()){
			BlockEntity te = world.getBlockEntity(pos);
			if(te instanceof CokerUnitTileEntity){
				BlockPos tPos = ((CokerUnitTileEntity)te).posInMultiblock;
				Direction facing = ((CokerUnitTileEntity)te).getFacing();
				
				// Locations that don't require sneaking to avoid the GUI
				
				// Conveyor locations
				if(tPos.getY() == 0 && tPos.getZ() == 2 && hit.getDirection() == Direction.UP){
					return InteractionResult.FAIL;
				}
				
				// All power input sockets
				if(CokerUnitTileEntity.Energy_IN.contains(tPos) && hit.getDirection() == facing){
					return InteractionResult.FAIL;
				}
				
				// Redstone controller input
				if(CokerUnitTileEntity.Redstone_IN.contains(tPos) && hit.getDirection() == facing.getOpposite()){
					return InteractionResult.FAIL;
				}
				
				// Fluid I/O Ports
				if(tPos.equals(CokerUnitTileEntity.Fluid_IN) || tPos.equals(CokerUnitTileEntity.Fluid_OUT)){
					return InteractionResult.FAIL;
				}
				
				// Item input port
				if(tPos.equals(CokerUnitTileEntity.Item_IN)){
					return InteractionResult.FAIL;
				}
			}
		}
		return super.use(state, world, pos, player, hand, hit);
	}
	
	@Override
	public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity){
		BlockEntity te = world.getBlockEntity(pos);
		if(te instanceof CokerUnitTileEntity){
			return ((CokerUnitTileEntity) te).isLadder();
		}
		return false;
	}
}
