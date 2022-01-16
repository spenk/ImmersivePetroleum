package flaxbeard.immersivepetroleum.common.blocks.metal;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IReadOnPlacement;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockBase;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockItemBase;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.AutoLubricatorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AutoLubricatorBlock extends IPBlockBase {
	private static final Material material = new Material(MaterialColor.METAL, false, false, true, true, false, false, PushReaction.BLOCK);
	
	public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
	public static final BooleanProperty SLAVE = BooleanProperty.create("slave");
	
	public AutoLubricatorBlock(String name){
		super(name, Block.Properties.create(material)
				.hardnessAndResistance(5.0F, 6.0F)
				.harvestTool(ToolType.AXE)
				.sound(SoundType.METAL)
				.notSolid());

		registerDefaultState(getStateDefinition().any()
				.setValue(FACING, Direction.NORTH)
				.setValue(SLAVE, false));
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder){
		builder.add(FACING, SLAVE);
	}
	
	@Override
	protected BlockItem createBlockItem(){
		return new AutoLubricatorBlockItem(this);
	}
	
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context){
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}
	
	@Override
	public boolean hasTileEntity(BlockState state){
		return true;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state){
		AutoLubricatorTileEntity te = new AutoLubricatorTileEntity(pos, state);
		te.isSlave = state.getValue(SLAVE);
		te.facing = state.getValue(FACING);
		return te;
	}
	
	@Override
	public void playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player){
		if(state.getValue(SLAVE)){
			worldIn.destroyBlock(pos.offset(0, -1, 0), !player.isCreative());
		}else{
			worldIn.destroyBlock(pos.offset(0, 1, 0), false);
		}

		super.playerWillDestroy(worldIn, pos, state, player);
	}
	
	@Override
	public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit){
		BlockEntity te = worldIn.getBlockEntity(pos);
		if(te instanceof IPlayerInteraction){
			if(((IPlayerInteraction) te).interact(hit.getDirection(), player, handIn, player.getItemInHand(handIn), (float) hit.getLocation().x, (float) hit.getLocation().y, (float) hit.getLocation().z)){
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.FAIL;
	}
	
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
		if(worldIn.isClientSide){
			worldIn.setBlockAndUpdate(pos.offset(0, 1, 0), state.setValue(SLAVE, true));
			BlockEntity te = worldIn.getBlockEntity(pos);
			if(te instanceof IReadOnPlacement){
				((IReadOnPlacement) te).readOnPlacement(placer, stack);
			}
		}
	}
	
	static final VoxelShape SHAPE_SLAVE = Shapes.create(.1875F, 0, .1875F, .8125f, 1, .8125f);
	static final VoxelShape SHAPE_MASTER = Shapes.create(.0625f, 0, .0625f, .9375f, 1, .9375f);
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context){
		return state.getValue(SLAVE) ? SHAPE_SLAVE : SHAPE_MASTER;
	}
	
	public static class AutoLubricatorBlockItem extends IPBlockItemBase {
		public AutoLubricatorBlockItem(Block blockIn){
			super(blockIn, new Item.Properties().tab(ImmersivePetroleum.creativeTab));
		}
		
		@Override
		protected boolean canPlace(BlockPlaceContext con, BlockState state){
			// No point in checking if the second block above is empty if it can't even place on the first one
			if(super.canPlace(con, state)){
				BlockPos pos=con.getClickedPos().offset(0, 1, 0);
				BlockState otherState = con.getLevel().getBlockState(pos);
				return otherState.getMaterial() == Material.AIR;
			}
			return false;
		}
	}
}
