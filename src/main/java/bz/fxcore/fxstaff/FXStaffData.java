package bz.fxcore.fxstaff;

import java.util.*;

public class FXStaffData {
    private static final Set<UUID> FROZEN_PLAYERS = new HashSet<>();
    private static final Map<UUID, List<String>> PLAYER_NOTES = new HashMap<>();

    public static boolean toggleFreeze(UUID uuid) {
        if (FROZEN_PLAYERS.contains(uuid)) {
            FROZEN_PLAYERS.remove(uuid);
            return false;
        } else {
            FROZEN_PLAYERS.add(uuid);
            return true;
        }
    }

    public static boolean isFrozen(UUID uuid) {
        return FROZEN_PLAYERS.contains(uuid);
    }

    public static void addNote(UUID uuid, String note) {
        addNoteSilent(uuid, note);
        FXStaffConfig.salvarAsync();
    }

    public static void addNoteSilent(UUID uuid, String note) {
        PLAYER_NOTES.computeIfAbsent(uuid, k -> new ArrayList<>()).add(note);
    }

    public static boolean removeNote(UUID uuid, int index) {
        List<String> notes = PLAYER_NOTES.get(uuid);
        if (notes != null && index >= 0 && index < notes.size()) {
            notes.remove(index);
            FXStaffConfig.salvarAsync();
            return true;
        }
        return false;
    }

    public static List<String> getNotes(UUID uuid) {
        return PLAYER_NOTES.getOrDefault(uuid, Collections.emptyList());
    }

    public static Map<UUID, List<String>> getAllNotes() {
        return PLAYER_NOTES;
    }

    public static void clearNotes() {
        PLAYER_NOTES.clear();
    }
}