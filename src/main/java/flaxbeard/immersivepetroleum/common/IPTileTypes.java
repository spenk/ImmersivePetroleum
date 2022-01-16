package flaxbeard.immersivepetroleum.common;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.MultiblockBEType;
import com.google.common.collect.ImmutableSet;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.blocks.tileentities.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static blusunrize.immersiveengineering.common.register.IEBlockEntities.makeType;

public class IPTileTypes {
	public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, ImmersivePetroleum.MODID);

	public static final MultiblockBEType<HydrotreaterTileEntity> TREATER = makeMultiblock(
			"hydrotreater", HydrotreaterTileEntity::new, IPContent.Multiblock.hydrotreater
	);

	public static final MultiblockBEType<PumpjackTileEntity> PUMP = makeMultiblock(
			"pumpjack", PumpjackTileEntity::new, IPContent.Multiblock.pumpjack
	);

	public static final MultiblockBEType<DistillationTowerTileEntity> TOWER = makeMultiblock(
			"distillationtower", DistillationTowerTileEntity::new, IPContent.Multiblock.distillationtower
	);

	public static final MultiblockBEType<CokerUnitTileEntity> COKER = makeMultiblock(
			"cokerunit", CokerUnitTileEntity::new, IPContent.Multiblock.cokerunit
	);


	public static final RegistryObject<BlockEntityType<GasGeneratorTileEntity>> GENERATOR = REGISTER.register(
			"gasgenerator", makeType(GasGeneratorTileEntity::new, IPContent.Blocks.gas_generator)
	);

	public static final RegistryObject<BlockEntityType<AutoLubricatorTileEntity>> AUTOLUBE = REGISTER.register(
			"auto_lubricator", makeType(AutoLubricatorTileEntity::new, IPContent.Blocks.auto_lubricator)
	);

	public static final RegistryObject<BlockEntityType<FlarestackTileEntity>> FLARE = REGISTER.register(
			"flarestack", makeType(FlarestackTileEntity::new, IPContent.Blocks.flarestack)
	);

	public static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeTypeMultipleBlocks(
			BlockEntityType.BlockEntitySupplier<T> create, Collection<? extends Supplier<? extends Block>> valid
	)
	{
		return () -> new BlockEntityType<>(
				create, ImmutableSet.copyOf(valid.stream().map(Supplier::get).collect(Collectors.toList())), null
		);
	}

	private static <T extends BlockEntity & IEBlockInterfaces.IGeneralMultiblock>
	MultiblockBEType<T> makeMultiblock(String name, MultiblockBEType.BEWithTypeConstructor<T> make, Supplier<? extends Block> block)
	{
		return new MultiblockBEType<>(
				name, REGISTER, make, block, state -> state.hasProperty(IEProperties.MULTIBLOCKSLAVE)&&!state.getValue(IEProperties.MULTIBLOCKSLAVE)
		);
	}
}
