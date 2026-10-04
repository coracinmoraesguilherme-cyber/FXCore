package bz.fxcore.modules.rp;

import bz.fxcore.FXCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FXCore.MODID)
public class RPManager {
    private static final Set<UUID> RP_OFF_PLAYERS = ConcurrentHashMap.newKeySet();

    public static boolean isOff(UUID uuid) {
        return RP_OFF_PLAYERS.contains(uuid);
    }

    public static void toggle(UUID uuid) {
        if (RP_OFF_PLAYERS.contains(uuid)) {
            RP_OFF_PLAYERS.remove(uuid);
        } else {
            RP_OFF_PLAYERS.add(uuid);
        }
    }

    public static void setStatus(UUID uuid, boolean off) {
        if (off) {
            RP_OFF_PLAYERS.add(uuid);
        } else {
            RP_OFF_PLAYERS.remove(uuid);
        }
    }

    public static Set<UUID> getOffPlayers() {
        return RP_OFF_PLAYERS;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RP_OFF_PLAYERS.remove(event.getEntity().getUUID());
    }
}