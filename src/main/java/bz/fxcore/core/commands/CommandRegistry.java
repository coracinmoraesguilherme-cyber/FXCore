package bz.fxcore.core.commands;

 //core
 import bz.fxcore.core.otimi.FXCoreCommand;
//modulos
//giveback
import bz.fxcore.modules.giveback.GiveBCommand;
//build
import bz.fxcore.modules.build.FXBuildCommand;
//clear
import bz.fxcore.modules.clear.FXClearCommand;

import bz.fxcore.modules.chat.ChatCommand;
import bz.fxcore.modules.staff.StaffCommands;
import bz.fxcore.modules.chat.TeamChatCommand;
import bz.fxcore.modules.team.TeamCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public class CommandRegistry {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // Módulo Staff (/fxs)
        StaffCommands.register(dispatcher);

        // Módulos futuros serão registrados aqui:
        FXCoreCommand.register(dispatcher);    // /fxcore
        ChatCommand.register(dispatcher);    // /fxchat
        TeamCommands.register(dispatcher);    // /fxteams
        TeamChatCommand.register(dispatcher); //fxchat
        FXClearCommand.register(dispatcher);   // /fxclear
        FXBuildCommand.register(dispatcher);   // /fxbuild
        GiveBCommand.register(dispatcher);// /fxgiveback
    }
}