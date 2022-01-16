package flaxbeard.immersivepetroleum.common.crafting;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.CokerUnitRecipe;
import flaxbeard.immersivepetroleum.api.crafting.DistillationRecipe;
import flaxbeard.immersivepetroleum.api.crafting.SulfurRecoveryRecipe;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler.ReservoirType;
import flaxbeard.immersivepetroleum.common.crafting.serializers.CokerUnitRecipeSerializer;
import flaxbeard.immersivepetroleum.common.crafting.serializers.DistillationRecipeSerializer;
import flaxbeard.immersivepetroleum.common.crafting.serializers.ReservoirTypeSerializer;
import flaxbeard.immersivepetroleum.common.crafting.serializers.SulfurRecoveryRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Serializers{
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ImmersivePetroleum.MODID);
	
	public static final RegistryObject<IERecipeSerializer<DistillationRecipe>> DISTILLATION_SERIALIZER = RECIPE_SERIALIZERS.register(
			"distillation", DistillationRecipeSerializer::new
	);
	
	public static final RegistryObject<IERecipeSerializer<CokerUnitRecipe>> COKER_SERIALIZER = RECIPE_SERIALIZERS.register(
			"coker", CokerUnitRecipeSerializer::new
	);
	
	public static final RegistryObject<IERecipeSerializer<SulfurRecoveryRecipe>> HYDROTREATER_SERIALIZER = RECIPE_SERIALIZERS.register(
			"hydrotreater", SulfurRecoveryRecipeSerializer::new
	);
	
	public static final RegistryObject<IERecipeSerializer<ReservoirType>> RESERVOIR_SERIALIZER = RECIPE_SERIALIZERS.register(
			"reservoirs", ReservoirTypeSerializer::new
	);
}
