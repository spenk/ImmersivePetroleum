package flaxbeard.immersivepetroleum.common.util.loot;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.function.Consumer;

public class IPTileDropLootEntry extends LootPoolSingletonContainer {
	public static final ResourceLocation ID = new ResourceLocation(ImmersivePetroleum.MODID, "tile_drop");
	
	protected IPTileDropLootEntry(int weightIn, int qualityIn, LootItemCondition[] conditionsIn, LootItemFunction[] functionsIn){
		super(weightIn, qualityIn, conditionsIn, functionsIn);
	}
	
	@Override
	protected void createItemStack(Consumer<ItemStack> stackConsumer, LootContext context){
		if(context.hasParam(LootContextParams.BLOCK_ENTITY)){
			BlockEntity te = context.getParam(LootContextParams.BLOCK_ENTITY);
			if(te instanceof IEBlockInterfaces.IBlockEntityDrop){
				((IEBlockInterfaces.IBlockEntityDrop) te).getBlockEntityDrop(context).forEach(stackConsumer);
			}
		}
	}
	
	@Override
	public LootPoolEntryType getType(){
		return IPLootFunctions.tileDrop;
	}
	
	public static LootPoolSingletonContainer.Builder<?> builder(){
		return simpleBuilder(IPTileDropLootEntry::new);
	}
	
	public static class Serializer extends LootPoolSingletonContainer.Serializer<IPTileDropLootEntry>{

		@Override
		protected IPTileDropLootEntry deserialize(JsonObject json, JsonDeserializationContext context, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
			return new IPTileDropLootEntry(weight, quality, conditions, functions);
		}
	}
}
