package flaxbeard.immersivepetroleum.common.util.compat.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import flaxbeard.immersivepetroleum.api.energy.FuelHandler;
import net.minecraftforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeType.Method;
import org.openzen.zencode.java.ZenCodeType.Name;

@ZenRegister
@Name("mods.immersivepetroleum.FuelRegistry")
public class FuelTweaker{
	
	@Method
	public static void registerGeneratorFuel(IFluidStack fluid, int fluxPerTick){
		if(fluid == null){
			CraftTweakerAPI.LOGGER.error("§cGeneratorFuel fluid can not be null!§r");
			return;
		}
		
		if(fluxPerTick < 1){
			CraftTweakerAPI.LOGGER.error("§cGeneratorFuel fluxPerTick has to be at least 1!§r");
			return;
		}
		
		FluidStack fstack = fluid.getInternal();
		FuelHandler.registerPortableGeneratorFuel(fstack.getFluid(), fluxPerTick, fstack.getAmount());
	}
	
	@Method
	public static void registerMotorboatFuel(IFluidStack fluid){
		if(fluid == null){
			CraftTweakerAPI.LOGGER.error("§cMotorboatFuel fluid can not be null!§r");
			return;
		}
		
		FluidStack fstack = fluid.getInternal();
		FuelHandler.registerMotorboatFuel(fstack.getFluid(), fstack.getAmount());
	}
}
