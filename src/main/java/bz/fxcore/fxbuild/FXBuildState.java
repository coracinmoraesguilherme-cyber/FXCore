package bz.fxcore.fxbuild;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FXBuildState {
    private static final Set<UUID> FREE_PLACE_PLAYERS = new HashSet<>();

    public static boolean toggleFreePlace(UUID playerUUID) {
        if (FREE_PLACE_PLAYERS.contains(playerUUID)) {
            FREE_PLAYERS_REMOVE(playerUUID);
            return false;
        } else {
            FREE_PLACE_PLAYERS.add(playerUUID);
            return true;
        }
    }

    private static void FREE_PLAYERS_REMOVE(UUID playerUUID) {
        FREE_PLACE_PLAYERS.remove(playerUUID);
    }

    public static boolean isFreePlaceEnabled(UUID playerUUID) {
        return FREE_PLACE_PLAYERS.contains(playerUUID);
    }
}