package bz.fxcore.fxchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths; // Caso use Forge antigo: net.minecraftforge.fml.loading.FMLPaths

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChannelManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Pega o diretório do jogo de forma dinâmica
    private static final File CHANNELS_DIR = FMLPaths.GAMEDIR.get()
            .resolve("config")
            .resolve("fxchat")
            .resolve("channels")
            .toFile();

    public static final Map<String, ChatChannel> CHANNELS = new HashMap<>();

    public static void loadChannels() {
        CHANNELS.clear();

        if (!CHANNELS_DIR.exists()) {
            CHANNELS_DIR.mkdirs();
            criarCanaisPadrao(CHANNELS_DIR);
        }

        File[] files = CHANNELS_DIR.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            criarCanaisPadrao(CHANNELS_DIR);
            files = CHANNELS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        }

        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    ChatChannel canal = GSON.fromJson(reader, ChatChannel.class);
                    if (canal != null && canal.getCommand() != null) {
                        CHANNELS.put(canal.getCommand().toLowerCase(), canal);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void criarCanaisPadrao(File dir) {
        salvarCanal(new ChatChannel(
                "Local",
                "l",
                "§e[L] {prefix}§f{player}§8: §f{msg}",
                100.0,
                0
        ));

        salvarCanal(new ChatChannel(
                "Global",
                "g",
                "§7[G] {prefix}§f{player}§8: §f{msg}",
                -1.0,
                0
        ));

        salvarCanal(new ChatChannel(
                "Time",
                "tc",
                "{prefix}§f{player}§8: §f{msg}",
                -1.0,
                0
        ));

        salvarCanal(new ChatChannel(
                "Staff",
                "s",
                "§c[Staff] §f{player}§8: §c{msg}",
                -1.0,
                2
        ));
    }

    public static void salvarCanal(ChatChannel canal) {
        if (!CHANNELS_DIR.exists()) {
            CHANNELS_DIR.mkdirs();
        }

        File arquivo = new File(CHANNELS_DIR, canal.getCommand().toLowerCase() + ".json");
        try (FileWriter writer = new FileWriter(arquivo)) {
            GSON.toJson(canal, writer);
            CHANNELS.put(canal.getCommand().toLowerCase(), canal);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean deletarCanal(String comando) {
        String cmd = comando.toLowerCase();
        File arquivo = new File(CHANNELS_DIR, cmd + ".json");

        if (arquivo.exists() && arquivo.delete()) {
            CHANNELS.remove(cmd);
            return true;
        }
        return false;
    }
}