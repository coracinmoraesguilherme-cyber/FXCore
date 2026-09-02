package bz.fxcore.modules.staff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FXStaffData {
    // Armazena o UUID do jogador e o tempo (timestamp em ms) até quando ele fica mutado
    private static final Map<UUID, Long> mutedPlayers = new HashMap<>();

    public static boolean isMuted(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return false;
        long expireTime = mutedPlayers.get(uuid);
        
        // Se for permanente (Long.MAX_VALUE) ou o tempo atual for menor que a expiração
        if (expireTime == Long.MAX_VALUE || System.currentTimeMillis() < expireTime) {
            return true;
        }
        
        // Remove automaticamente se o tempo já expirou
        mutedPlayers.remove(uuid);
        return false;
    }

    public static long getMuteRemainingTime(UUID uuid) {
        if (!isMuted(uuid)) return 0;
        long expireTime = mutedPlayers.get(uuid);
        if (expireTime == Long.MAX_VALUE) return Long.MAX_VALUE;
        return expireTime - System.currentTimeMillis();
    }

    public static void mute(UUID uuid, long durationMillis) {
        if (durationMillis <= 0) {
            mutedPlayers.put(uuid, Long.MAX_VALUE); // Permanente
        } else {
            mutedPlayers.put(uuid, System.currentTimeMillis() + durationMillis);
        }
    }

    public static void unmute(UUID uuid) {
        mutedPlayers.remove(uuid);
    }
}