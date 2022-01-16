package flaxbeard.immersivepetroleum.api.crafting;

import net.minecraft.tags.Tag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple handler for the Flarestack
 * @author TwistedGate
 */
public class FlarestackHandler {
	static final Set<Tag<Fluid>> burnables = new HashSet<>();
	
	/**
	 * Registers a fluidtag as being burnable in the Flarestack
	 * 
	 * @param fluidTag that should be burned
	 */
	public static void register(@Nonnull Tag<Fluid> fluidTag){
		if(fluidTag != null && !burnables.contains(fluidTag)){
			burnables.add(fluidTag);
		}
	}
	
	/**
	 * Tests wether the given fluid is burnable by the flarestack or not
	 * 
	 * @param fluid to test
	 * @return true if the given fluid is infact burnable, false otherwise
	 */
	public static boolean isBurnable(@Nonnull Fluid fluid){
		return fluid != null && burnables.stream().anyMatch(tag -> tag.contains(fluid));
	}
	
	/**
	 * Tests wether the given fluidstack is burnable by the flarestack or not
	 * 
	 * @param fluidstack to test
	 * @return true if the given fluid is infact burnable, false otherwise
	 */
	public static boolean isBurnable(@Nonnull FluidStack fluidstack){
		return !fluidstack.isEmpty() && isBurnable(fluidstack.getFluid());
	}
	
	public static Set<Tag<Fluid>> getSet(){
		return Collections.unmodifiableSet(burnables);
	}
}
