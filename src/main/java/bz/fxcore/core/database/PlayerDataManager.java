package bz.fxcore.core.database;

import bz.fxcore.core.otimi.server.FXTaskExecutor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_DIR = new File("config/fxcore/data/players");
    private static final Map<UUID, FXPlayerData> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();

    public static void init() {
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        FXTaskExecutor.runAsync(PlayerDataManager::preloadNameCache);
    }

    private static void preloadNameCache() {
        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.uuid != null && data.lastName != null && !data.lastName.isEmpty()) {
                        NAME_TO_UUID_CACHE.put(data.lastName.toLowerCase(), data.uuid);
                    }
                } catch (Exception ignored) {}
            }
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

    public static void evict(UUID uuid) {
        CACHE.remove(uuid);
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
                    if (data.lastName != null && !data.lastName.isEmpty()) {
                        NAME_TO_UUID_CACHE.put(data.lastName.toLowerCase(), data.uuid);
                    }
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
        if (data.lastName != null && !data.lastName.isEmpty()) {
            NAME_TO_UUID_CACHE.put(data.lastName.toLowerCase(), data.uuid);
        }
        FXTaskExecutor.runAsync(() -> saveSync(data));
    }

    public static void saveSync(FXPlayerData data) {
        if (data == null || data.uuid == null) return;
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        File file = new File(DATA_DIR, data.uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Descobre o UUID de um jogador pelo nome (Online, arquivos locais ou cache do servidor).
     */
    public static UUID getUUIDByName(MinecraftServer server, String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;

        // 1. Verifica se está online no servidor atualmente
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
        if (onlinePlayer != null) {
            UUID uuid = onlinePlayer.getUUID();
            NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), uuid);
            return uuid;
        }

        // 2. Procura no cache reverso em memória
        UUID cachedUuid = NAME_TO_UUID_CACHE.get(playerName.toLowerCase());
        if (cachedUuid != null) {
            return cachedUuid;
        }

        // 3. Procura nos dados em cache
        for (FXPlayerData cached : CACHE.values()) {
            if (cached.lastName != null && cached.lastName.equalsIgnoreCase(playerName)) {
                NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), cached.uuid);
                return cached.uuid;
            }
        }

        // 4. Tenta buscar pelo cache de perfis oficial do servidor (Mojang/Cache)
        Optional<GameProfile> profileOpt = server.getProfileCache().get(playerName);
        if (profileOpt.isPresent()) {
            UUID uuid = profileOpt.get().getId();
            NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), uuid);
            return uuid;
        }

        // 5. Varredura direta no disco (último recurso)
        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.lastName != null && data.lastName.equalsIgnoreCase(playerName)) {
                        NAME_TO_UUID_CACHE.put(playerName.toLowerCase(), data.uuid);
                        return data.uuid;
                    }
                } catch (IOException ignored) {}
            }
        }

        return null;
    }

    // Regras de Punições (Ban / Mute / Unban / BanList)

    public static void banPlayer(UUID targetUuid, String author, String reason, String duration, long expireTimestamp) {
        FXPlayerData data = get(targetUuid);
        data.isBanned = true;
        data.banReason = reason;
        data.banAuthor = author;
        data.banExpireTimestamp = expireTimestamp;
        data.history.add(new FXPlayerData.HistoryEntry("BAN", reason, author, duration));
        save(data);
    }

    public static void mutePlayer(UUID targetUuid, String author, String reason, String duration, long expireTimestamp) {
        FXPlayerData data = get(targetUuid);
        data.isMuted = true;
        data.muteReason = reason;
        data.muteAuthor = author;
        data.muteExpireTimestamp = expireTimestamp;
        data.history.add(new FXPlayerData.HistoryEntry("MUTE", reason, author, duration));
        save(data);
    }

    public static boolean unmutePlayer(UUID targetUuid) {
        FXPlayerData data = get(targetUuid);
        if (data.isMuted) {
            data.isMuted = false;
            data.muteReason = "";
            data.muteAuthor = "";
            data.muteExpireTimestamp = 0L;
            save(data);
            return true;
        }
        return false;
    }

    public static boolean isPlayerMuted(UUID targetUuid) {
        FXPlayerData data = get(targetUuid);
        if (!data.isMuted) return false;
        if (data.muteExpireTimestamp > 0L && System.currentTimeMillis() > data.muteExpireTimestamp) {
            data.isMuted = false;
            data.muteReason = "";
            data.muteAuthor = "";
            data.muteExpireTimestamp = 0L;
            save(data);
            return false;
        }
        return true;
    }

    public static long getMuteRemainingMillis(UUID targetUuid) {
        FXPlayerData data = get(targetUuid);
        if (!isPlayerMuted(targetUuid)) return 0L;
        if (data.muteExpireTimestamp <= 0L || data.muteExpireTimestamp == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, data.muteExpireTimestamp - System.currentTimeMillis());
    }

    public static boolean unbanPlayer(String playerName) {
        for (FXPlayerData cached : CACHE.values()) {
            if (cached.lastName != null && cached.lastName.equalsIgnoreCase(playerName)) {
                if (cached.isBanned) {
                    cached.isBanned = false;
                    cached.banReason = "";
                    cached.banAuthor = "";
                    cached.banExpireTimestamp = 0L;
                    save(cached);
                    return true;
                }
            }
        }

        File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    FXPlayerData data = GSON.fromJson(reader, FXPlayerData.class);
                    if (data != null && data.lastName != null && data.lastName.equalsIgnoreCase(playerName)) {
                        if (data.isBanned) {
                            data.isBanned = false;
                            data.banReason = "";
                            data.banAuthor = "";
                            data.banExpireTimestamp = 0L;
                            save(data);
                            CACHE.put(data.uuid, data);
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