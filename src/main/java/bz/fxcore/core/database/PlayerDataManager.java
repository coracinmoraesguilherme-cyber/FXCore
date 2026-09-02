package bz.fxcore.core.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_DIR = new File("config/fxcore/data/players");
    private static final Map<UUID, FXPlayerData> CACHE = new HashMap<>();

    public static void init() {
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
    }

    public static void loadPlayerData(UUID uuid) {
        get(uuid);
    }

    public static void savePlayerData(UUID uuid) {
        FXPlayerData data = CACHE.get(uuid);
        if (data != null) {
            save(data);
            CACHE.remove(uuid);
        }
    }

    public static FXPlayerData get(UUID uuid) {
        if (CACHE.containsKey(uuid)) {
            return CACHE.get(uuid);
        }

        File file = new File(DATA_DIR, uuid.toString() + ".json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                if (data != null) {
                    CACHE.put(uuid, data);
                    return data;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FXPlayerData newData = new FXPlayerData();
        newData.uuid = uuid;
        CACHE.put(uuid, newData);
        return newData;
    }

    public static void save(FXPlayerData data) {
        if (data == null || data.uuid == null) return;
        
        File file = new File(DATA_DIR, data.uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Regras de Punições (Ban / Mute / Unban / BanList)

    public static void banPlayer(UUID targetUuid, String author, String reason, String duration) {
        FXPlayerData data = get(targetUuid);
        data.isBanned = true;
        data.banReason = reason;
        data.banAuthor = author;
        data.history.add(new FXPlayerData.HistoryEntry("BAN", reason, author, duration));
        save(data);
    }

    public static void mutePlayer(UUID targetUuid, String author, String reason, String duration) {
        FXPlayerData data = get(targetUuid);
        data.history.add(new FXPlayerData.HistoryEntry("MUTE", reason, author, duration));
        save(data);
    }

    public static boolean unbanPlayer(String playerName) {
        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.lastName != null && data.lastName.equalsIgnoreCase(playerName)) {
                        if (data.isBanned) {
                            data.isBanned = false;
                            save(data);
                            return true;
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        return false;
    }

    public static List<String> getBannedList() {
        List<String> banned = new ArrayList<>();
        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.isBanned) {
                        String name = data.lastName != null ? data.lastName : data.uuid.toString();
                        banned.add(name + " §7(Motivo: " + data.banReason + " | Por: " + data.banAuthor + ")");
                    }
                } catch (IOException ignored) {}
            }
        }
        return banned;
    }

    // Gestão de Anotações

    public static void addNote(UUID targetUuid, String author, String text) {
        FXPlayerData data = get(targetUuid);
        int nextId = data.notes.stream().mapToInt(n -> n.id).max().orElse(0) + 1;
        data.notes.add(new FXPlayerData.NoteEntry(nextId, author, text, System.currentTimeMillis()));
        save(data);
    }

    public static boolean removeNote(UUID targetUuid, int noteId) {
        FXPlayerData data = get(targetUuid);
        boolean removed = data.notes.removeIf(n -> n.id == noteId);
        if (removed) {
            save(data);
        }
        return removed;
    }

    // Busca de Contas Secundárias (Alts)

    public static List<String> findAlts(String ip) {
        List<String> alts = new ArrayList<>();
        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.ipHistory != null && data.ipHistory.contains(ip)) {
                        if (data.lastName != null) {
                            alts.add(data.lastName);
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        return alts;
    }
}