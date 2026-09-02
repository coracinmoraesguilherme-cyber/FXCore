package bz.fxcore.modules.staff;

import bz.fxcore.core.database.PlayerDataManager;

import java.util.UUID;

public class FXStaffData {

    public static boolean isMuted(UUID uuid) {
        return PlayerDataManager.isPlayerMuted(uuid);
    }

    public static long getMuteRemainingTime(UUID uuid) {
        return PlayerDataManager.getMuteRemainingMillis(uuid);
    }

    public static void mute(UUID uuid, long durationMillis) {
        long expire = durationMillis <= 0 ? 0L : System.currentTimeMillis() + durationMillis;
        PlayerDataManager.mutePlayer(uuid, "Staff", "Silenciado por comando", durationMillis <= 0 ? "Permanente" : TimeUtil.formatTime(durationMillis), expire);
    }

    public static void unmute(UUID uuid) {
        PlayerDataManager.unmutePlayer(uuid);
    }
}