package flaxbeard.immersivepetroleum.common.items;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class IPUpgradeItem extends IPItemBase implements IUpgrade{
	private Set<String> set;
	public IPUpgradeItem(String name, String type){
		super("upgrade_" + name, new Item.Properties().stacksTo(1));
		this.set = ImmutableSet.of(type);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@NotNull ItemStack stack, Level worldIn, List<Component> tooltip, @NotNull TooltipFlag flagIn){
		tooltip.add(new TranslatableComponent("desc.immersivepetroleum.flavour." + getRegistryName().getPath()));
	}
	
	@Override
	public Set<String> getUpgradeTypes(ItemStack upgrade){
		return this.set;
	}
	
	@Override
	public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade){
		return true;
	}
	
	@Override
	public void applyUpgrades(ItemStack target, ItemStack upgrade, CompoundTag modifications){
	}
}
