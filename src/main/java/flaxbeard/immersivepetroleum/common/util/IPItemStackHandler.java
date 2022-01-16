package flaxbeard.immersivepetroleum.common.util;

import blusunrize.immersiveengineering.api.utils.CapabilityUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IPItemStackHandler extends ItemStackHandler implements ICapabilityProvider {
	private static final Runnable EMPTY_RUN = () -> {};
	
	@Nonnull
	private Runnable onChange = EMPTY_RUN;
	public IPItemStackHandler(){
		super();
		int idealSize = getSlots();
		NonNullList<ItemStack> list = NonNullList.withSize(idealSize, ItemStack.EMPTY);
		for(int i = 0;i < Math.min(this.stacks.size(), idealSize);++i){
			list.set(i, this.stacks.get(i));
		}
		
		this.stacks = list;
	}
	
	@Override
	public int getSlots(){
		return 4;
	}
	
	public void setTile(BlockEntity tile){
		this.onChange = tile != null ? tile::setChanged : EMPTY_RUN;
	}
	
	public void setInventoryForUpdate(Inventory inv){
		this.onChange = inv != null ? inv::setChanged : EMPTY_RUN;
	}
	
	protected void onContentsChanged(int slot){
		super.onContentsChanged(slot);
		this.onChange.run();
	}
	
	LazyOptional<IItemHandler> handler = CapabilityUtils.constantOptional(this);
	
	@Nullable
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing){
		return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, handler);
	}
	
	public NonNullList<ItemStack> getContainedItems(){
		return this.stacks;
	}
}
