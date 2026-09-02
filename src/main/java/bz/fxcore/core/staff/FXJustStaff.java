package bz.fxcore.core.staff;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class FXJustStaff {

    private static boolean enabled = false;

    @FunctionalInterface
    public interface CommandFeedback {
        void send(String message);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggleJustStaff(ServerPlayer executor, boolean state, CommandFeedback callback) {
        enabled = state;

        if (enabled) {
            kickNonOps(executor);
            callback.send("§a[FXCore] Modo JustStaff (Manutenção) §lATIVADO§a! Apenas OPs podem entrar.");
        } else {
            callback.send("§c[FXCore] Modo JustStaff (Manutenção) §lDESATIVADO§c! Entrada liberada para todos.");
        }
    }

    private static void kickNonOps(ServerPlayer executor) {
        if (executor.getServer() == null) return;

        Component kickMessage = Component.literal("§c[FXCore] O servidor entrou em modo de manutenção (JustStaff).\n§7Tente novamente mais tarde!");

        for (ServerPlayer player : executor.getServer().getPlayerList().getPlayers()) {
            if (!executor.getServer().getPlayerList().isOp(player.getGameProfile())) {
                player.connection.disconnect(kickMessage);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            var server = player.getServer();
            if (server != null && !server.getPlayerList().isOp(player.getGameProfile())) {
                player.connection.disconnect(Component.literal("§c[FXCore] Servidor em manutenção exclusiva para a Staff (JustStaff)."));
            }
        }
    }
}