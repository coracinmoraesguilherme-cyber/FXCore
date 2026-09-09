package bz.fxcore;

import bz.fxcore.modules.build.particles.ParticleShapeJson;
import bz.fxcore.core.commands.CommandRegistry;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.core.otimi.server.FXTaskExecutor;
import bz.fxcore.core.otimi.server.FXServerManager;
import bz.fxcore.modules.chat.ChannelManager;
import bz.fxcore.modules.team.TeamManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FXCore.MODID)
public class FXCore {
    public static final String MODID = "fxcore";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public FXCore(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        ChannelManager.loadChannels();

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        // Registra o evento de tick no barramento
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
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
        LOGGER.info(" + FXServer");
        LOGGER.info(" + FXGiveBack");
        LOGGER.info(" + FXBuild");
        LOGGER.info(" + FXServerTools");
        LOGGER.info(" + FXAFK");
        LOGGER.info("");
        LOGGER.info("[Status] Initializing...");
        LOGGER.info("==============================================================");
        
        PlayerDataManager.init();
        TeamManager.init();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandRegistry.register(event.getDispatcher(), event.getBuildContext());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer() != null) {
            FXServerManager.onServerTick(event.getServer());
        }
    }
    private void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
    FXTaskExecutor.shutdown();
    }
}