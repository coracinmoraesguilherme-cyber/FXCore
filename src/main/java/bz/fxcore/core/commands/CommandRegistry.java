package bz.fxcore.core.commands;

//core
import bz.fxcore.core.otimi.FXCoreCommand;
//simple command
import bz.fxcore.core.util.SimpleCommand;
//modulos
//giveback
import bz.fxcore.modules.giveback.GiveBCommand;
//build
import bz.fxcore.modules.build.FXBuildCommand;
//clear
import bz.fxcore.modules.clear.FXClearCommand;
//rp
import bz.fxcore.modules.rp.RPCommand;
import bz.fxcore.modules.rp.RPAdminCommand;

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

        // Módulos do Core e Sistemas
        FXCoreCommand.register(dispatcher);    // /fxcore
        ChatCommand.register(dispatcher);    // /fxchat
        TeamCommands.register(dispatcher);    // /fxteams
        TeamChatCommand.register(dispatcher); // /fxchat (team)
        FXClearCommand.register(dispatcher);   // /fxclear
        FXBuildCommand.register(dispatcher, context);   // /fxbuild
        GiveBCommand.register(dispatcher);// /fxgiveback
        SimpleCommand.register(dispatcher); // simple commands
        
        // Módulo de Roleplay (RP)
        RPCommand.register(dispatcher);
        RPAdminCommand.register(dispatcher);
    }
}