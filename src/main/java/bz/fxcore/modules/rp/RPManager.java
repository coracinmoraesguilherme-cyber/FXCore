package bz.fxcore.modules.rp;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RPManager {
    private static final Set<UUID> RP_OFF_PLAYERS = new HashSet<>();

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
}