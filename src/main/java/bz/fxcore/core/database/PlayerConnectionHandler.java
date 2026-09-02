package bz.fxcore.core.database;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerConnectionHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            
            String currentIp = player.getIpAddress();
            String currentName = player.getName().getString();

            data.lastName = currentName;
            if (!data.ipHistory.contains(currentIp)) {
                data.ipHistory.add(currentIp);
            }

            PlayerDataManager.save(data);
        }
    }
}