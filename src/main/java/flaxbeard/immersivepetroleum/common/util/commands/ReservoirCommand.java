package flaxbeard.immersivepetroleum.common.util.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.PumpjackHandler.ReservoirType;
import flaxbeard.immersivepetroleum.api.crafting.pumpjack.ReservoirWorldInfo;
import flaxbeard.immersivepetroleum.common.IPSaveData;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ReservoirCommand{
	private ReservoirCommand(){
	}
	
	// new TranslatableComponent("chat.immersivepetroleum.command.reservoir.setCapacity.NFE", numberString)
	
	public static LiteralArgumentBuilder<CommandSourceStack> create(){
		LiteralArgumentBuilder<CommandSourceStack> lab = Commands.literal("reservoir")
				.executes(source -> {
					CommandUtils.sendHelp(source.getSource(), "");
					return Command.SINGLE_SUCCESS;
				})
				.requires(source -> source.hasPermission(4));
		
		lab.then(Commands.literal("get").executes(source -> get(source.getSource().getPlayerOrException())));
		
		lab.then(setReservoir());
		
		lab.then(Commands.literal("setAmount")
				.executes(source -> {
					CommandUtils.sendHelp(source.getSource(), ".setAmount");
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
						.executes(context -> setAmount(context.getSource().getPlayerOrException(), context.getArgument("amount", Integer.class)))));
		
		lab.then(Commands.literal("setCapacity")
				.executes(source -> {
					CommandUtils.sendHelp(source.getSource(), ".setCapacity");
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("capacity", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
						.executes(context -> setCapacity(context.getSource().getPlayerOrException(), context.getArgument("capacity", Integer.class)))));
		
		return lab;
	}
	
	static int get(ServerPlayer playerEntity){
		ReservoirWorldInfo info = getOilWorldInfo(playerEntity);
		
		TranslatableComponent cmp = new TranslatableComponent("chat.immersivepetroleum.command.reservoir.get",
				ChatFormatting.GOLD + (info.type != null ? info.type.name : "null") + ChatFormatting.RESET,
				ChatFormatting.GOLD + (info.overrideType != null ? info.overrideType.name : "null") + ChatFormatting.RESET,
				ChatFormatting.GOLD + (info.current + "/" + info.capacity + " mB") + ChatFormatting.RESET);
		
		String h = cmp.getString();
		
		for(String g:h.split("<br>")){
			playerEntity.sendMessage(new TextComponent(g), Util.NIL_UUID);
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	static LiteralArgumentBuilder<CommandSourceStack> setReservoir(){
		RequiredArgumentBuilder<CommandSourceStack, String> nameArg = Commands.argument("name", StringArgumentType.string());
		nameArg.suggests((context, builder) -> {
			return SharedSuggestionProvider.suggest(PumpjackHandler.reservoirs.values().stream().map(type -> type.name), builder);
		}).executes(command -> {
			ServerPlayer player = command.getSource().getPlayerOrException();
			setReservoir(command, player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
			return Command.SINGLE_SUCCESS;
		}).then(Commands.argument("location", ColumnPosArgument.columnPos()).executes(command -> {
			ColumnPos pos = ColumnPosArgument.getColumnPos(command, "location");
			setReservoir(command, pos.x, pos.z);
			return Command.SINGLE_SUCCESS;
		}));
		
		return Commands.literal("set").executes(source -> {
			CommandUtils.sendHelp(source.getSource(), ".set");
			return Command.SINGLE_SUCCESS;
		}).then(nameArg);
	}
	
	static void setReservoir(CommandContext<CommandSourceStack> context, int xChunk, int zChunk){
		CommandSourceStack sender = context.getSource();
		ReservoirWorldInfo info = PumpjackHandler.getOrCreateOilWorldInfo(sender.getLevel(), xChunk, zChunk);
		
		String name = context.getArgument("name", String.class);
		ReservoirType reservoir = null;
		for(ReservoirType res:PumpjackHandler.reservoirs.values())
			if(res.name.equalsIgnoreCase(name))
				reservoir = res;
			
		if(reservoir == null){
			sender.sendSuccess(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.set.invalidReservoir", name), true);
			return;
		}
		
		info.overrideType = reservoir;
		IPSaveData.markInstanceAsDirty();
		sender.sendSuccess(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.set.sucess", reservoir.name), true);
	}
	
	static int set(ServerPlayer playerEntity, String name){
		ReservoirWorldInfo info = getOilWorldInfo(playerEntity);
		
		ReservoirType reservoir = null;
		for(ReservoirType res:PumpjackHandler.reservoirs.values())
			if(res.name.equalsIgnoreCase(name))
				reservoir = res;
			
		if(reservoir == null){
			playerEntity.sendMessage(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.set.invalidReservoir", name), Util.NIL_UUID);
			return Command.SINGLE_SUCCESS;
		}
		
		info.overrideType = reservoir;
		playerEntity.sendMessage(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.set.sucess", reservoir.name), Util.NIL_UUID);
		IPSaveData.markInstanceAsDirty();
		
		return Command.SINGLE_SUCCESS;
	}
	
	static int setAmount(ServerPlayer playerEntity, int amount){
		ReservoirWorldInfo info = getOilWorldInfo(playerEntity);
		
		amount = Math.min(info.capacity, Math.max(0, amount)); // Clamping action; Prevents amount from going negative or over the capacity.
		
		// TODO Maybe add a message to inform the player that the value has been clamped?
		
		info.current = amount;
		playerEntity.sendMessage(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.setAmount.sucess", Integer.toString(amount)), Util.NIL_UUID);
		IPSaveData.markInstanceAsDirty();
		
		return Command.SINGLE_SUCCESS;
	}
	
	static int setCapacity(ServerPlayer playerEntity, int amount){
		ReservoirWorldInfo info = getOilWorldInfo(playerEntity);
		
		amount = Math.max(0, amount);
		
		info.capacity = amount;
		playerEntity.sendMessage(new TranslatableComponent("chat.immersivepetroleum.command.reservoir.setCapacity.sucess", Integer.toString(amount)), Util.NIL_UUID);
		IPSaveData.markInstanceAsDirty();
		
		return Command.SINGLE_SUCCESS;
	}
	
	static ReservoirWorldInfo getOilWorldInfo(ServerPlayer playerEntity){
		ChunkPos coords = new ChunkPos(playerEntity.blockPosition());
		return PumpjackHandler.getOrCreateOilWorldInfo(playerEntity.getLevel(), coords.x, coords.z);
	}
}
