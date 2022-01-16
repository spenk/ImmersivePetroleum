package flaxbeard.immersivepetroleum.common.util.compat.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.tag.MCTag;
import com.blamejared.crafttweaker.api.util.Many;
import flaxbeard.immersivepetroleum.api.crafting.LubricantHandler;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.material.Fluid;
import org.openzen.zencode.java.ZenCodeType.Method;
import org.openzen.zencode.java.ZenCodeType.Name;

@ZenRegister
@Name("mods.immersivepetroleum.Lubricant")
public class LubricantRegistryTweaker {
	
	@SuppressWarnings("unchecked")
	@Method
	public static void register(Many<MCTag<Fluid>> tag){
		if(tag == null){
			CraftTweakerAPI.LOGGER.error("§cLubricantRegistry: Expected fluidtag as input fluid!§r");
			return;
		}
		
		if(tag.getAmount() < 1){
			CraftTweakerAPI.LOGGER.error("§cLubricantRegistry: Amount must atleast be 1mB!§r");
			return;
		}
		
		LubricantHandler.register((Tag<Fluid>) tag.getData().getInternal(), tag.getAmount());
	}
}
