package flaxbeard.immersivepetroleum.common.network;

import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageDebugSync implements INetMessage{
	
	CompoundTag nbt;
	public MessageDebugSync(CompoundTag nbt){
		this.nbt = nbt;
	}

	public MessageDebugSync(FriendlyByteBuf buf){
		this.nbt = buf.readNbt();
	}

	@Override
	public void toBytes(FriendlyByteBuf buf){
		buf.writeNbt(this.nbt);
	}

	@Override
	public void process(Supplier<NetworkEvent.Context> context){
		context.get().enqueueWork(() -> {
			NetworkEvent.Context con = context.get();

			if(con.getDirection().getReceptionSide() == LogicalSide.SERVER && con.getSender() != null){
				Player player = con.getSender();
				ItemStack mainItem = player.getMainHandItem();
				ItemStack secondItem = player.getOffhandItem();
				boolean main = !mainItem.isEmpty() && mainItem.getItem() == IPContent.debugItem;
				boolean off = !secondItem.isEmpty() && secondItem.getItem() == IPContent.debugItem;

				if(main || off){
					ItemStack target = main ? mainItem : secondItem;

					CompoundTag targetNBT = target.getOrCreateTagElement("settings");
					targetNBT.merge(this.nbt);
				}
			}
		});
	}
}
