package flaxbeard.immersivepetroleum.common.items;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.entity.MotorboatEntity;
import flaxbeard.immersivepetroleum.common.util.IPItemStackHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class MotorboatItem extends IPItemBase implements IUpgradeableTool {
	public static final String UPGRADE_TYPE = "MOTORBOAT";
	
	public MotorboatItem(String name){
		super(name, new Item.Properties().stacksTo(1).tab(ImmersivePetroleum.creativeTab));
	}
	
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt){
		return new IPItemStackHandler();
	}
	
	@Override
	public CompoundTag getUpgrades(ItemStack stack){
		return new CompoundTag();
	}
	
	@Override
	public void clearUpgrades(ItemStack stack){
	}
	
	@Override
	public boolean canTakeFromWorkbench(ItemStack stack){
		return true;
	}
	
	@Override
	public boolean canModify(ItemStack stack){
		return true;
	}

	@Override
	public void recalculateUpgrades(ItemStack stack, Level w, Player player){
		if(!w.isClientSide){
			return;
		}
		
		clearUpgrades(stack);
		
		LazyOptional<IItemHandler> lazy = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		lazy.ifPresent(handler -> {
			CompoundTag nbt = new CompoundTag();
			
			for(int i = 0;i < handler.getSlots();i++){
				ItemStack u = handler.getStackInSlot(i);
				if(!u.isEmpty() && u.getItem() instanceof IUpgrade){
					IUpgrade upg = (IUpgrade) u.getItem();
					if(upg.getUpgradeTypes(u).contains(UPGRADE_TYPE) && upg.canApplyUpgrades(stack, u)){
						upg.applyUpgrades(stack, u, nbt);
					}
				}
			}
			
			finishUpgradeRecalculation(stack);
		});
	}
	
	@Override
	public void removeFromWorkbench(Player player, ItemStack stack){
	}
	
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, @NotNull InteractionHand handIn){
		ItemStack itemstack = playerIn.getItemInHand(handIn);
		float f1 = playerIn.xRotO + (playerIn.getXRot() - playerIn.xRotO);
		float f2 = playerIn.yRotO + (playerIn.getYRot() - playerIn.yRotO);
		double d0 = playerIn.xo + (playerIn.getX() - playerIn.xo);
		double d1 = playerIn.yo + (playerIn.getY() - playerIn.yo) + (double) playerIn.getEyeHeight();
		double d2 = playerIn.zo + (playerIn.getZ() - playerIn.zo);
		Vec3 vec3d = new Vec3(d0, d1, d2);
		float f3 = Mth.cos(-f2 * 0.017453292F - (float) Math.PI);
		float f4 = Mth.sin(-f2 * 0.017453292F - (float) Math.PI);
		float f5 = -Mth.cos(-f1 * 0.017453292F);
		float f6 = Mth.sin(-f1 * 0.017453292F);
		float f7 = f4 * f5;
		float f8 = f3 * f5;
		
		Vec3 vec3d1 = vec3d.add((double) f7 * 5.0D, (double) f6 * 5.0D, (double) f8 * 5.0D);
		HitResult raytraceresult = worldIn.clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, playerIn));

		if(raytraceresult != null){
			Vec3 vec3d2 = playerIn.getLookAngle();
			boolean flag = false;
			AABB bb = playerIn.getBoundingBox();
			if(bb == null)
				bb = playerIn.getBoundingBox();

			if(bb != null){
				List<Entity> list = worldIn.getEntities(playerIn, bb.expandTowards(vec3d2.x * 5.0D, vec3d2.y * 5.0D, vec3d2.z * 5.0D).inflate(1.0D));
				for(int i = 0;i < list.size();++i){
					Entity entity = (Entity) list.get(i);

					if(entity.canBeCollidedWith()){
						AABB AABB = entity.getBoundingBox();
						if(AABB != null && AABB.inflate((double) entity.getPickRadius()).contains(vec3d)){
							flag = true;
						}
					}
				}
			}

			if(flag){
				return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, itemstack);
			}else if(raytraceresult.getType() != BlockHitResult.Type.BLOCK){
				return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, itemstack);
			}else{
				Vec3 hit = raytraceresult.getLocation();
				Block block = worldIn.getBlockState(new BlockPos(hit.add(0, .5, 0))).getBlock();
				boolean flag1 = block == Blocks.WATER;
				MotorboatEntity entityboat = new MotorboatEntity(worldIn, hit.x, flag1 ? hit.y - 0.12D : hit.y, hit.z);
				{
					entityboat.setYRot(playerIn.getYRot());
					entityboat.setUpgrades(getContainedItems(itemstack));
					entityboat.readTank(itemstack.getTag());
				}

				if(worldIn.getBlockCollisions(entityboat, entityboat.getBoundingBox().inflate(-0.1D)).iterator().hasNext()){
					return new InteractionResultHolder<ItemStack>(InteractionResult.FAIL, itemstack);
				}else{
					if(worldIn.isClientSide){
						worldIn.addFreshEntity(entityboat);
					}

					if(!playerIn.isCreative()){
						itemstack.shrink(1);
					}

					// playerIn.addStat(net.minecraft.stats.Stats.CUSTOM.get(getRegistryName()));
					return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, itemstack);
				}
			}
		}

		return new InteractionResultHolder<ItemStack>(InteractionResult.PASS, itemstack);
	}
	
	protected NonNullList<ItemStack> getContainedItems(ItemStack stack){
		IItemHandler handler = (IItemHandler) stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(null);
		
		if(handler == null){
			ImmersivePetroleum.log.info("No valid inventory handler found for " + stack);
			return NonNullList.create();
		}
		
		if(handler instanceof IPItemStackHandler){
			return ((IPItemStackHandler) handler).getContainedItems();
		}
		
		ImmersivePetroleum.log.warn("Inefficiently getting contained items. Why does " + stack + " have a non-IE IItemHandler?");
		NonNullList<ItemStack> inv = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
		for(int i = 0;i < handler.getSlots();++i){
			inv.set(i, handler.getStackInSlot(i));
		}
		
		return inv;
	}
	
	@Override
	public void finishUpgradeRecalculation(ItemStack stack){
	}

	@Override
	public Slot[] getWorkbenchSlots(AbstractContainerMenu container, ItemStack stack, Level world, Supplier<Player> getPlayer, IItemHandler inv){
		if(inv != null){
			return new Slot[]{
					new IESlot.Upgrades(container, inv, 0, 78, 35 - 5, UPGRADE_TYPE, stack, true, world, getPlayer),
					new IESlot.Upgrades(container, inv, 1, 98, 35 + 5, UPGRADE_TYPE, stack, true, world, getPlayer),
					new IESlot.Upgrades(container, inv, 2, 118, 35 - 5, UPGRADE_TYPE, stack, true, world, getPlayer)
			};
		}else{
			return new Slot[0];
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(@NotNull ItemStack stack, Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn){
		if(ItemNBTHelper.hasKey(stack, "tank")){
			FluidStack fs = FluidStack.loadFluidStackFromNBT(ItemNBTHelper.getTagCompound(stack, "tank"));
			if(fs != null){
				tooltip.add(((TextComponent) fs.getDisplayName()).append(": " + fs.getAmount() + "mB").withStyle(ChatFormatting.GRAY));
			}
		}
		
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
	}
}
