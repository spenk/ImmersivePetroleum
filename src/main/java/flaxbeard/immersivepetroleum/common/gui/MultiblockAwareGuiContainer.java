package flaxbeard.immersivepetroleum.common.gui;

import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockBlockEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;

/**
 * @author TwistedGate © 2021
 */
public class MultiblockAwareGuiContainer<T extends PoweredMultiblockBlockEntity<T, ?>> extends IEBaseContainer<T>{
	static final Vec3i ONE = new Vec3i(1, 1, 1);
	
	protected BlockPos templateSize;
	public MultiblockAwareGuiContainer(MenuType<?> menu, T tile, int id, IETemplateMultiblock template){
		super(menu, tile, id);
		
		this.templateSize = new BlockPos(template.getSize(this.tile.getLevelNonnull())).subtract(ONE);
	}
	
	/**
	 * Returns the maximum distance in blocks to the multiblock befor the GUI
	 * get's closed automaticly
	 */
	public int getMaxDistance(){
		return 5;
	}
	
	@Override
	public boolean stillValid(Player player){
		if(inv != null){
			BlockPos min = this.tile.getBlockPosForPos(BlockPos.ZERO);
			BlockPos max = this.tile.getBlockPosForPos(this.templateSize);

			AABB box = new AABB(min, max).inflate(getMaxDistance());

			return box.intersects(player.getBoundingBox());
		}

		return false;
	}
}
