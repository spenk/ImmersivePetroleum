package flaxbeard.immersivepetroleum.common.util.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;

public class CommandUtils{
	static void sendHelp(CommandSourceStack commandSource, String subIdent){
		commandSource.sendSuccess(new TranslatableComponent("chat.immersivepetroleum.command.reservoir" + subIdent + ".help"), true);
	}
}
