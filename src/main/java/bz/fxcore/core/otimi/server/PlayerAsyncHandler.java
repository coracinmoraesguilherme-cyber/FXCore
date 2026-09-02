package bz.fxcore.core.otimi.server;

import bz.fxcore.core.database.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerAsyncHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXTaskExecutor.runAsync(() -> {
                PlayerDataManager.loadPlayerData(player.getUUID());
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXTaskExecutor.runAsync(() -> {
                PlayerDataManager.savePlayerData(player.getUUID());
            });
        }
    }
}