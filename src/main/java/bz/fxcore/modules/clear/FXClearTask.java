package bz.fxcore.modules.clear;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class FXClearTask {

    private static int counter = -1;

    public static String getFormattedTimeRemaining() {
        if (!FXClearConfig.DATA.enableAutoClear) {
            return "§cDesativado";
        }
        if (counter < 0) {
            int totalSeconds = FXClearConfig.DATA.clearRectIntervalMinutes * 60;
            counter = totalSeconds;
        }
        
        int minutes = counter / 60;
        int seconds = counter % 60;
        return String.format("§e%02d:%02d", minutes, seconds);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!FXClearConfig.DATA.enableAutoClear) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        // Executa a cada 1 segundo (20 ticks)
        if (server.getTickCount() % 20 != 0) return;

        int totalSecondsLimit = FXClearConfig.DATA.clearRectIntervalMinutes * 60;

        if (counter < 0) {
            counter = totalSecondsLimit;
        }

        counter--;

        // Verifica se deve mandar aviso
        if (FXClearConfig.DATA.warningSeconds.contains(counter)) {
            String msg = FXClearConfig.DATA.warningMsg.replace("{time}", String.valueOf(counter));
            server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }

        // Chegou a zero, limpa o chão
        if (counter <= 0) {
            int count = FXClearManager.clearEntities(server);
            String successMsg = FXClearConfig.DATA.clearSuccessMsg.replace("{count}", String.valueOf(count));
            server.getPlayerList().broadcastSystemMessage(Component.literal(successMsg), false);
            counter = totalSecondsLimit; // Reinicia o ciclo
        }
    }
}