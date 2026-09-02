package bz.fxcore.modules.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TeamManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/fxcore/data/teams.json");
    private static final Map<String, TeamData> TEAMS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_TEAM_MAP = new HashMap<>();
    
    // Guardar os convites temporários na memória (UUID do Convidado -> ID do Time)
    private static final Map<UUID, String> PENDING_INVITES = new HashMap<>();

    public static void init() {
        if (!FILE.getParentFile().exists()) FILE.getParentFile().mkdirs();

        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                Type type = new TypeToken<Map<String, TeamData>>(){}.getType();
                Map<String, TeamData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    TEAMS.putAll(loaded);
                    for (TeamData team : TEAMS.values()) {
                        for (UUID member : team.members) {
                            PLAYER_TEAM_MAP.put(member, team.id);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Gerenciamento de Convites
    public static void invitePlayer(UUID player, String teamId) {
        PENDING_INVITES.put(player, teamId);
    }

    public static String getPendingInvite(UUID player) {
        return PENDING_INVITES.get(player);
    }

    public static void removeInvite(UUID player) {
        PENDING_INVITES.remove(player);
    }

    // Consultas e Ações de Time
    public static boolean hasTeam(UUID player) {
        return PLAYER_TEAM_MAP.containsKey(player);
    }

    public static TeamData getPlayerTeam(UUID player) {
        String id = PLAYER_TEAM_MAP.get(player);
        return id != null ? TEAMS.get(id) : null;
    }

    public static TeamData createTeam(String name, String tag, UUID owner, String ownerName) {
        if (hasTeam(owner)) return null;

        String id = "fx_" + ownerName.toLowerCase();
        TeamData team = new TeamData(id, name, tag, owner);
        TEAMS.put(id, team);
        PLAYER_TEAM_MAP.put(owner, id);
        save();
        return team;
    }

    public static void deleteTeam(String teamId) {
        TeamData team = TEAMS.remove(teamId);
        if (team != null) {
            for (UUID member : team.members) {
                PLAYER_TEAM_MAP.remove(member);
            }
            save();
        }
    }

    public static Collection<TeamData> getAllTeams() {
        return TEAMS.values();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(TEAMS, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}