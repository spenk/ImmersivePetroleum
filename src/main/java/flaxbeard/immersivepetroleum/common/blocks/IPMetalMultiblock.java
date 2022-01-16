package flaxbeard.immersivepetroleum.common.blocks;

import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.MultiblockBEType;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.MetalMultiblockBlock;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class IPMetalMultiblock<T extends MultiblockPartBlockEntity<T>> extends MetalMultiblockBlock<T>{
	public IPMetalMultiblock(Properties props, MultiblockBEType<T> te){
		super(te, props);

/*		// Nessesary hacks
		if(!FMLLoader.isProduction()){
			IEContent.registeredIEBlocks.remove(this);
			Iterator<Item> it = IEContent.registeredIEItems.iterator();
			while(it.hasNext()){
				Item item = it.next();
				if(item instanceof BlockItemIE && ((BlockItemIE) item).getBlock() == this){
					it.remove();
					break;
				}
			}
		}*/
		
		IPContent.registeredIPBlocks.add(this);
		
		BlockItem bItem = new BlockItemIE(this, new Item.Properties().tab(ImmersivePetroleum.creativeTab));
		IPContent.registeredIPItems.add(bItem.setRegistryName(getRegistryName()));
	}

	@Override
	public ResourceLocation createRegistryName(){
		return new ResourceLocation(ImmersivePetroleum.MODID, name);
	}
}
