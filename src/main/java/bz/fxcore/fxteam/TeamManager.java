package bz.fxcore.fxteam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PASTA_FXCHAT = new File("config/fxchat");
    private static final File ARQUIVO_TIMES = new File(PASTA_FXCHAT, "teams_data.json");

    private static Map<UUID, String> TIME_LIDERES = new HashMap<>();
    public static final Map<UUID, String> CONVITES_PENDENTES = new HashMap<>();
    public static final Set<UUID> TEAM_SPY_ATIVOS = new HashSet<>();

    public static void carregarDados() {
        TeamConfig.carregar(); // Carrega as mensagens customizáveis

        if (!PASTA_FXCHAT.exists()) {
            PASTA_FXCHAT.mkdirs();
        }

        if (ARQUIVO_TIMES.exists()) {
            try (FileReader reader = new FileReader(ARQUIVO_TIMES)) {
                Type type = new TypeToken<Map<UUID, String>>() {}.getType();
                Map<UUID, String> map = GSON.fromJson(reader, type);
                if (map != null) {
                    TIME_LIDERES = map;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void salvarDados() {
        try {
            if (!PASTA_FXCHAT.exists()) {
                PASTA_FXCHAT.mkdirs();
            }
            try (FileWriter writer = new FileWriter(ARQUIVO_TIMES)) {
                GSON.toJson(TIME_LIDERES, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean ehLiderDeQualquerTime(UUID uuid) {
        carregarDados();
        return TIME_LIDERES.containsKey(uuid);
    }

    public static String getTeamNameDoLider(UUID uuid) {
        carregarDados();
        return TIME_LIDERES.get(uuid);
    }

    public static void registrarLider(UUID liderUuid, String teamName) {
        carregarDados();
        TIME_LIDERES.put(liderUuid, teamName);
        salvarDados();
    }

    public static void removerLider(UUID liderUuid) {
        carregarDados();
        TIME_LIDERES.remove(liderUuid);
        salvarDados();
    }

    public static boolean estaEmAlgumTime(ServerPlayer player) {
        return player.getTeam() != null;
    }

    public static boolean criarTime(ServerPlayer criador, String nomeFormatado, String tag) {
        Scoreboard scoreboard = criador.getServer().getScoreboard();
        String idTime = "fx_" + criador.getScoreboardName().toLowerCase();

        if (scoreboard.getPlayerTeam(idTime) != null) {
            return false;
        }

        PlayerTeam team = scoreboard.addPlayerTeam(idTime);
        team.setDisplayName(Component.literal(nomeFormatado));

        atualizarTagTime(team, tag, "§f");
        team.setColor(ChatFormatting.WHITE);

        scoreboard.addPlayerToTeam(criador.getScoreboardName(), team);
        registrarLider(criador.getUUID(), idTime);
        return true;
    }

    public static void atualizarTagTime(PlayerTeam team, String tagTexto, String codigoCorTag) {
        String formato = TeamConfig.MESSAGES.teamPrefixFormat
                .replace("{TAG}", tagTexto)
                .replace("{TAG_COLOR}", codigoCorTag);
        team.setPlayerPrefix(Component.literal(formato));
    }

    public static boolean deletarTime(MinecraftServer server, String idTime) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(idTime);
        if (team != null) {
            scoreboard.removePlayerTeam(team);
            TIME_LIDERES.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(idTime));
            salvarDados();
            return true;
        }
        return false;
    }
}