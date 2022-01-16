package flaxbeard.immersivepetroleum.common.blocks.stone;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockBase;
import flaxbeard.immersivepetroleum.common.blocks.IPBlockItemBase;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

import javax.annotation.Nullable;

public class PetcokeBlock extends IPBlockBase{
	public PetcokeBlock(){
		super("petcoke_block", Block.Properties.of(Material.STONE));
	}
	
	@Override
	protected BlockItem createBlockItem(){
		return new IPBlockItemBase(this, new Item.Properties().tab(ImmersivePetroleum.creativeTab)){
			@Override
			public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType){
				return 32000;
			}
		};
	}
}
