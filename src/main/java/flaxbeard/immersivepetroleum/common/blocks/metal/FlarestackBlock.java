package flaxbeard.immersivepetroleum.common.blocks.metal;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.util.ChatUtils;
import blusunrize.immersiveengineering.common.util.Utils;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPTileTypes;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockBase;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockItemBase;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.FlarestackTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

public class FlarestackBlock extends IPBlockBase{
	private static final Material material = new Material(MaterialColor.METAL, false, false, true, true, false, false, PushReaction.BLOCK);

	public static final BooleanProperty SLAVE = BooleanProperty.create("slave");
	
	public FlarestackBlock(){
		super("flarestack", Block.Properties.create(material)
				.hardnessAndResistance(3.0F, 15.0F)
				.harvestTool(ToolType.PICKAXE)
				.sound(SoundType.METAL)
				.notSolid());
		
		registerDefaultState(getStateDefinition().any()
				.with(SLAVE, false));
	}
	
	@Override
	protected BlockItem createBlockItem(){
		return new FlarestackBlockItem(this);
	}
	
	@Override
	protected void fillStateContainer(StateDefinition.Builder<Block, BlockState> builder){
		builder.add(SLAVE);
	}
	
	@Override
	public int getOpacity(BlockState state, BlockGetter worldIn, BlockPos pos){
		return 0;
	}
	
	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos){
		return true;
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public float getAmbientOcclusionLightValue(BlockState state, BlockGetter worldIn, BlockPos pos){
		return 1.0F;
	}
	
	@Override
	public InteractionResult onBlockActivated(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit){
		if(Utils.isScrewdriver(player.getItemInHand(handIn))){
			if(state.getValue(SLAVE)){
				pos = pos.offset(Direction.DOWN);
			}
			
			if(!worldIn.isRemote){
				BlockEntity te = worldIn.getBlockEntity(pos);
				if(te != null && te instanceof FlarestackTileEntity){
					FlarestackTileEntity flare = ((FlarestackTileEntity) te);
					flare.invertRedstone();
					
					ChatUtils.sendServerNoSpamMessages(player, new TranslatableComponent(Lib.CHAT_INFO + "rsControl." + (flare.isRedstoneInverted() ? "invertedOn" : "invertedOff")));
				}
			}
			
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	
	@Override
	public void onBlockHarvested(Level worldIn, BlockPos pos, BlockState state, Player player){
		if(state.getValue(SLAVE)){
			worldIn.destroyBlock(pos.add(0, -1, 0), !player.isCreative());
		}else{
			worldIn.destroyBlock(pos.add(0, 1, 0), false);
		}
		super.onBlockHarvested(worldIn, pos, state, player);
	}
	
	@Override
	public void onBlockPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack){
		if(!worldIn.isRemote){
			worldIn.setBlockState(pos.offset(Direction.UP), state.with(SLAVE, true));
		}
	}
	
	@Override
	public void onEntityCollision(BlockState state, Level worldIn, BlockPos pos, Entity entityIn){
		if(state.getValue(SLAVE) && !entityIn.isImmuneToFire()){
			entityIn.attackEntityFrom(DamageSource.HOT_FLOOR, 1.0F);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder){
		if(state.getValue(SLAVE)){
			// TODO Don't know how else i would do this yet
			return Collections.emptyList();
		}
		
		return super.getDrops(state, builder);
	}
	
	static VoxelShape SHAPE_SLAVE;
	static VoxelShape SHAPE_MASTER;
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context){
		if(state.getValue(SLAVE)){
			if(SHAPE_SLAVE == null){
				VoxelShape s0 = Shapes.create(0.125, 0.0, 0.125, 0.875, 0.75, 0.875);
				VoxelShape s1 = Shapes.create(0.0625, 0.0, 0.0625, 0.9375, 0.375, 0.9375);
				SHAPE_SLAVE = Shapes.combineAndSimplify(s0, s1, IBooleanFunction.OR);
			}
			
			return SHAPE_SLAVE;
		}else{
			if(SHAPE_MASTER == null){
				VoxelShape s0 = Shapes.create(0.125, 0.0, 0.125, 0.875, 0.75, 0.875);
				VoxelShape s1 = Shapes.create(0.0625, 0.5, 0.0625, 0.9375, 1.0, 0.9375);
				SHAPE_MASTER = Shapes.combineAndSimplify(s0, s1, IBooleanFunction.OR);
			}
			
			return SHAPE_MASTER;
		}
	}
	
	@Override
	public boolean hasTileEntity(BlockState state){
		return !state.getValue(SLAVE);
	}
	
	@Override
	public BlockEntity createTileEntity(BlockState state, BlockGetter world){
		return IPTileTypes.FLARE.get().create();
	}
	
	public static class FlarestackBlockItem extends IPBlockItemBase{
		public FlarestackBlockItem(Block blockIn){
			super(blockIn, new Item.Properties().tab(ImmersivePetroleum.creativeTab));
		}
		
		@Override
		protected boolean canPlace(BlockPlaceContext con, BlockState state){
			if(super.canPlace(con, state)){
				BlockPos otherPos = con.getPos().offset(Direction.UP);
				BlockState otherState = con.getWorld().getBlockState(otherPos);
				
				return otherState.getBlock().isAir(otherState, con.getLevel(), otherPos);
			}
			return false;
		}
	}
}
