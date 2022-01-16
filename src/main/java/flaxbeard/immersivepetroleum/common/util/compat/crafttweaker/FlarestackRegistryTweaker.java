package flaxbeard.immersivepetroleum.common.util.compat.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.tag.MCTag;
import flaxbeard.immersivepetroleum.api.crafting.FlarestackHandler;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.material.Fluid;
import org.openzen.zencode.java.ZenCodeType.Method;
import org.openzen.zencode.java.ZenCodeType.Name;

@ZenRegister
@Name("mods.immersivepetroleum.Flarestack")
public class FlarestackRegistryTweaker{
	
	/**
	 * Adds a fluid tag to the Flarestacks "burnable fluids" list
	 * 
	 * @param tag The fluidtag to be added
	 * 
	 * @docParam tag <tag:fluids:minecraft:water>
	 */
	@SuppressWarnings("unchecked")
	@Method
	public static void register(MCTag<Fluid> tag){
		if(tag == null){
			CraftTweakerAPI.LOGGER.error("§cFlarestackHandler: Expected fluidtag as input fluid!§r");
			return;
		}
		
		FlarestackHandler.register((Tag<Fluid>) tag.getInternal());
	}
}
