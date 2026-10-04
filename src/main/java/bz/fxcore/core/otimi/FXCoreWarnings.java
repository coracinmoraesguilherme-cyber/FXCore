package bz.fxcore.core.otimi;

import bz.fxcore.core.config.FXCoreConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FXCoreWarnings {

    private static final List<Long> warningHistory = new CopyOnWriteArrayList<>();
    private static final long ONE_HOUR_MS = 3600 * 1000L;

    public static void warnStaff(MinecraftServer server, String message) {
        if (server == null || server.getPlayerList() == null) return;
        
        if (!FXCoreConfig.DATA.warningsEnabled) return;

        // Registra o aviso com o horário atual
        warningHistory.add(System.currentTimeMillis());

        Component component = Component.literal("§c[FXCore Warning] §f" + message);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(component);
            }
        }
    }

    /**
     * Retorna a quantidade de avisos disparados na última 1 hora.
     */
    public static int getRecentWarningsCount() {
        long now = System.currentTimeMillis();
        // Limpa o histórico removendo registros mais antigos que 1 hora
        warningHistory.removeIf(timestamp -> (now - timestamp) > ONE_HOUR_MS);
        return warningHistory.size();
    }
}