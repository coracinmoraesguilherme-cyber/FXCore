package bz.fxcore;

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
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Inicializando FXCore V2...");
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