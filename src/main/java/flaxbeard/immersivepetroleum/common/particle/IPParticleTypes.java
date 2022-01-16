package flaxbeard.immersivepetroleum.common.particle;

import blusunrize.immersiveengineering.common.register.IEParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.RegistryObject;

public class IPParticleTypes{
	public static final RegistryObject<SimpleParticleType> FLARE_FIRE = IEParticles.REGISTER.register(
			"flare_fire", () -> new SimpleParticleType(false)
	);
}
