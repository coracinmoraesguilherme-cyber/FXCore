package bz.fxcore;

// AntiLag
import bz.fxcore.fxantilag.ChunkLoadGuard;
import bz.fxcore.fxantilag.DimensionUnloader;
import bz.fxcore.fxantilag.FXAntiLagConfig;
import bz.fxcore.fxantilag.FXCoreCommands;
import bz.fxcore.fxantilag.SmartAFKFarm;

// Build
import bz.fxcore.fxbuild.FXBuildCommands;
import bz.fxcore.fxbuild.FXBuildEvents;

// Chat & Teams
import bz.fxcore.fxchat.ChannelManager;
import bz.fxcore.fxchat.ChatCommand;
import bz.fxcore.fxchat.ModConfig;
import bz.fxcore.fxteam.FXTeamCommand;
import bz.fxcore.fxteam.TeamConfig;

// ClearItems & Server & Unify
import bz.fxcore.fxclearitems.ClearItemsCommand;
import bz.fxcore.fxclearitems.ClearItemsConfig;
import bz.fxcore.fxclearitems.ClearItemsManager;
import bz.fxcore.fxserver.FXServerCommand;
import bz.fxcore.fxserver.FXServerConfig;
import bz.fxcore.fxserver.FXServerManager;
import bz.fxcore.fxunify.FXUnifyCommand;

// Staff
import bz.fxcore.fxstaff.FXFreezeEvents;
import bz.fxcore.fxstaff.FXStaffCommands;
import bz.fxcore.fxstaff.FXStaffConfig;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(FXCore.MODID)
public class FXCore {

    public static final String MODID = "fxcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FXCore() {
        // Registra a própria classe de eventos centrais
        NeoForge.EVENT_BUS.register(this);
        
        // AntiLag Módulos
        NeoForge.EVENT_BUS.register(ChunkLoadGuard.class);
        NeoForge.EVENT_BUS.register(DimensionUnloader.class);
        NeoForge.EVENT_BUS.register(SmartAFKFarm.class);
        NeoForge.EVENT_BUS.register(FXCoreCommands.class);

        // Build & Staff Events
        NeoForge.EVENT_BUS.register(FXBuildEvents.class);
        NeoForge.EVENT_BUS.register(FXFreezeEvents.class);

        // Carregamento de Configurações em Memória
        ModConfig.loadConfig();
        ChannelManager.loadChannels();
        TeamConfig.carregar();
        ClearItemsConfig.carregar();
        FXServerConfig.carregar();
        FXAntiLagConfig.load();
        FXStaffConfig.carregar();

        LOGGER.info("==================================");
        LOGGER.info(" FXCore 1.21.1 Carregado com Sucesso!");
        LOGGER.info("==================================");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ChannelManager.loadChannels();
        
        // Registro de Todos os Comandos dos Módulos
        ChatCommand.register(event.getDispatcher());
        FXTeamCommand.register(event.getDispatcher());
        ClearItemsCommand.register(event.getDispatcher());
        FXServerCommand.register(event.getDispatcher());
        FXUnifyCommand.register(event.getDispatcher());
        FXStaffCommands.register(event.getDispatcher());
        FXBuildCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String mensagem = event.getRawText();

        event.setCanceled(true);
        String canalAtual = ChatCommand.CANAL_ATUAL_JOGADOR.getOrDefault(player.getUUID(), "l");
        ChatCommand.enviarMensagemCanal(player, canalAtual, mensagem);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ClearItemsManager.onServerTick(event.getServer());
        FXServerManager.onServerTick(event.getServer());
    }
}