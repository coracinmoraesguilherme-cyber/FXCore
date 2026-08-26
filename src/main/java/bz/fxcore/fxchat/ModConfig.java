package bz.fxcore.fxchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxchat.json");

    public static Messages MESSAGES = new Messages();

    public static class Messages {
        public String canalEntrou = "§aVocê entrou no canal §e[{canal}]§a!";
        public String ninguemOuviu = "§c[!] Ninguém ouviu sua mensagem...";
        public String semQuemResponder = "§cVocê não tem ninguém para responder!";
        public String destinatarioOffline = "§cO jogador anterior não está mais online.";
        public String formatoTellRemetente = "§7[§fVocê §8-> §f{destinatario}§7] §f{msg}";
        public String formatoTellDestinatario = "§7[§f{remetente} §8-> §fVocê§7] §f{msg}";
        public String formatoSpy = "§8[SPY] §7{remetente} §8-> §7{destinatario}: §f{msg}";
        public String spyAtivado = "§a[FXChat] Modo SocialSpy ativado!";
        public String spyDesativado = "§c[FXChat] Modo SocialSpy desativado.";
        public String reloadSucesso = "§a[FXChat] Configurações e mensagens recarregadas com sucesso!";
        public String canalCriado = "§a[FXChat] Canal '{nome}' (/{comando}) criado com sucesso!";
        public String canalRemovido = "§a[FXChat] Canal do comando /{comando} removido!";
        public String canalNaoEncontrado = "§c[FXChat] Canal não encontrado.";
    }

    public static void loadConfig() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                MESSAGES = GSON.fromJson(reader, Messages.class);
                if (MESSAGES == null) {
                    MESSAGES = new Messages();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(MESSAGES, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}