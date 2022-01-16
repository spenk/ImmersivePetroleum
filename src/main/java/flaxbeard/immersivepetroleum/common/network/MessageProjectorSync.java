package flaxbeard.immersivepetroleum.common.network;

import flaxbeard.immersivepetroleum.common.IPContent;
import flaxbeard.immersivepetroleum.common.util.projector.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageProjectorSync implements INetMessage{
	
	public static void sendToServer(Settings settings, InteractionHand hand){
		IPPacketHandler.sendToServer(new MessageProjectorSync(settings, hand, true));
	}
	
	public static void sendToClient(Player player, Settings settings, InteractionHand hand){
		IPPacketHandler.sendToPlayer(player, new MessageProjectorSync(settings, hand, false));
	}
	
	boolean forServer;
	CompoundTag nbt;
	InteractionHand hand;
	
	public MessageProjectorSync(Settings settings, InteractionHand hand, boolean toServer){
		this(settings.toNbt(), hand, toServer);
	}
	
	public MessageProjectorSync(CompoundTag nbt, InteractionHand hand, boolean toServer){
		this.nbt = nbt;
		this.forServer = toServer;
		this.hand = hand;
	}
	
	public MessageProjectorSync(FriendlyByteBuf buf){
		this.nbt = buf.readNbt();
		this.forServer = buf.readBoolean();
		this.hand = InteractionHand.values()[buf.readByte()];
	}
	
	@Override
	public void toBytes(FriendlyByteBuf buf){
		buf.writeNbt(this.nbt);
		buf.writeBoolean(this.forServer);
		buf.writeByte(this.hand.ordinal());
	}
	
	@Override
	public void process(Supplier<NetworkEvent.Context> context){
		context.get().enqueueWork(() -> {
			NetworkEvent.Context con = context.get();
			
			if(con.getDirection().getReceptionSide() == getSide() && con.getSender() != null){
				Player player = con.getSender();
				ItemStack held = player.getItemInHand(this.hand);
				
				if(!held.isEmpty() && held.getItem() == IPContent.Items.projector){
					Settings settings = new Settings(this.nbt);
					settings.applyTo(held);
				}
			}
		});
	}
	
	LogicalSide getSide(){
		return this.forServer ? LogicalSide.SERVER : LogicalSide.CLIENT;
	}
}
