package flaxbeard.immersivepetroleum.common.util.loot;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

public class IPLootFunctions{
	public static LootPoolEntryType tileDrop;
	
	public static void modConstruction(){
		tileDrop = registerEntry(IPTileDropLootEntry.ID, new IPTileDropLootEntry.Serializer());
	}
	
	private static LootPoolEntryType registerEntry(ResourceLocation id, Serializer<? extends LootPoolEntryContainer> serializer){
		return Registry.register(Registry.LOOT_POOL_ENTRY_TYPE, id, new LootPoolEntryType(serializer));
	}
}
