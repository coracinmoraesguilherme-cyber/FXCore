package bz.fxcore;

<<<<<<< Updated upstream
// AntiLag
import bz.fxcore.fxantilag.ChunkLoadGuard;
import bz.fxcore.fxantilag.DimensionUnloader;
import bz.fxcore.fxantilag.FXAntiLagConfig;
import bz.fxcore.fxantilag.FXCoreCommands;
import bz.fxcore.fxantilag.SmartAFKFarm;
=======
import bz.fxcore.core.commands.CommandRegistry;
import bz.fxcore.core.config.FXCoreConfig;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.core.otimi.server.FXTaskExecutor;
import bz.fxcore.modules.build.particles.ParticleShapeJson;
import bz.fxcore.modules.chat.ChannelManager;
import bz.fxcore.modules.team.TeamManager;
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
=======
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
>>>>>>> Stashed changes

@Mod(FXCore.MODID)
public class FXCore {

    public static final String MODID = "fxcore";
    public static final Logger LOGGER = LogUtils.getLogger();

<<<<<<< Updated upstream
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
=======
    public FXCore(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        ChannelManager.loadChannels();

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        ParticleShapeJson.loadShapes(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("==============================================================");
        LOGGER.info("");
        LOGGER.info("  ________    __       _______   _______   ______    _______ ");
        LOGGER.info(" |  ____\\ \\  / /      / ____| | |  __  | |  __  \\  |  ____|");
        LOGGER.info(" | |__   \\ \\/ /      | |      | | |  | | | |__) | | |__    ");
        LOGGER.info(" |  __|  /   \\       | |      | | |  | | |  _  /  |  __|   ");
        LOGGER.info(" | |    / /\\  \\      | |____  | | |__| | | | \\ \\  | |____  ");
        LOGGER.info(" |_|   /_/   \\_/      \\_____| |_|______/ |_|  \\_\\ |______| ");
        LOGGER.info("");
        LOGGER.info("                    FXCore Framework v2");
        LOGGER.info("==============================================================");
        LOGGER.info("");
        LOGGER.info("[Modules]");
        LOGGER.info(" + FXChat");
        LOGGER.info(" + FXTeams");
        LOGGER.info(" + FXClearItems");
        LOGGER.info(" + FXStaff");
        LOGGER.info(" + FXGiveBack");
        LOGGER.info(" + FXBuild");
        LOGGER.info(" + FXAFK");
        LOGGER.info("");
        LOGGER.info("[Status] Initializing...");
        LOGGER.info("==============================================================");
        
        FXCoreConfig.load();
        PlayerDataManager.init();
        TeamManager.init();
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
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
=======
    private void onServerStopping(ServerStoppingEvent event) {
        FXTaskExecutor.shutdown();
>>>>>>> Stashed changes
    }
}