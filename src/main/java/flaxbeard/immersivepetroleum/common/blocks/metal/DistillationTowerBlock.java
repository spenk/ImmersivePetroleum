package flaxbeard.immersivepetroleum.common.blocks.metal;

import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.blocks.IPMetalMultiblock;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.DistillationTowerTileEntity;
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

public class DistillationTowerBlock extends IPMetalMultiblock<DistillationTowerTileEntity>{
	public DistillationTowerBlock(){
		super("distillationtower", () -> IPTileTypes.TOWER.get());
	}
	
	@Override
	public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit){
		if(!player.getItemInHand(hand).isEmpty()){
			BlockEntity te = world.getBlockEntity(pos);
			if(te instanceof DistillationTowerTileEntity){
				DistillationTowerTileEntity tower = (DistillationTowerTileEntity) te;
				BlockPos tPos = tower.posInMultiblock;
				Direction facing = tower.getFacing();
				
				// Locations that don't require sneaking to avoid the GUI
				
				// Power input
				if(DistillationTowerTileEntity.Energy_IN.contains(tPos) && hit.getDirection() == Direction.UP){
					return InteractionResult.FAIL;
				}
				
				// Redstone controller input
				if(DistillationTowerTileEntity.Redstone_IN.contains(tPos) && (tower.getIsMirrored() ? hit.getDirection() == facing.getClockWise() : hit.getDirection() == facing.getCounterClockWise())){
					return InteractionResult.FAIL;
				}
				
				// Fluid I/O Ports
				if((tPos.equals(DistillationTowerTileEntity.Fluid_IN) && (tower.getIsMirrored() ? hit.getDirection() == facing.getCounterClockWise() : hit.getDirection() == facing.getClockWise()))
				|| (tPos.equals(DistillationTowerTileEntity.Fluid_OUT) && hit.getDirection() == facing.getOpposite())){
					return InteractionResult.FAIL;
				}
				
				// Item output port
				if(tPos.equals(DistillationTowerTileEntity.Item_OUT) && (tower.getIsMirrored() ? hit.getDirection() == facing.getClockWise() : hit.getDirection() == facing.getCounterClockWise())){
					return InteractionResult.FAIL;
				}
			}
		}
		return super.use(state, world, pos, player, hand, hit);
	}
	
	@Override
	public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity){
		BlockEntity te = world.getBlockEntity(pos);
		if(te instanceof DistillationTowerTileEntity){
			return ((DistillationTowerTileEntity) te).isLadder();
		}
		return false;
	}
}
