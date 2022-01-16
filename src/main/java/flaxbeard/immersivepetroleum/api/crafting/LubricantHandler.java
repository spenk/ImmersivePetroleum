package flaxbeard.immersivepetroleum.api.crafting;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LubricantHandler{
	static final Set<Pair<Tag<Fluid>, Integer>> lubricants = new HashSet<>();
	
	/**
	 * Registers a lubricant to be used in the Lubricant Can and Automatic
	 * Lubricator
	 *
	 * @param lube The fluid to be used as lubricant
	 * @param amount mB of lubricant to spend every 4 ticks
	 * @deprecated THIS DOES NOTHING! in favour of fluid tags, use {@link #register(net.minecraft.tags.Tag, int)} instead.
	 */
	public static void registerLubricant(Fluid lube, int amount){
		ImmersivePetroleum.log.warn("LubricantHandler skipped adding \""+lube.getRegistryName()+"\". Please use the FluidTag registration!");
	}
	
	/**
	 * Registers a lubricant to be used in the Lubricant Can and Automatic
	 * Lubricator
	 *
	 * @param fluid The fluid to be used as lubricant
	 * @param amount mB of lubricant to spend every 4 ticks
	 */
	public static void register(@Nonnull Tag<Fluid> fluid, int amount){
		if(fluid != null && !lubricants.stream().anyMatch(pair -> pair.getLeft() == fluid)){
			lubricants.add(Pair.of(fluid, amount));
		}
	}
	
	/**
	 * Gets amount of this Fluid that is used every four ticks for the Automatic
	 * Lubricator. 0 if not valid lube. 100 * this result is used for the
	 * Lubricant Can
	 *
	 * @param toCheck Fluid to check
	 * @return mB of this Fluid used to lubricate
	 */
	public static int getLubeAmount(@Nonnull Fluid toCheck){
		if(toCheck != null){
			for(Map.Entry<Tag<Fluid>, Integer> entry:lubricants){
				if(entry.getKey().contains(toCheck)){
					return entry.getValue();
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Whether or not the given Fluid is a valid lubricant
	 *
	 * @param toCheck Fluid to check
	 * @return Whether or not the Fluid is a lubricant
	 */
	public static boolean isValidLube(@Nonnull Fluid toCheck){
		return toCheck != null && lubricants.stream().anyMatch(pair -> pair.getKey().contains(toCheck));
	}
}
