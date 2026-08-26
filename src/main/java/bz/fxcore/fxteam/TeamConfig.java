package bz.fxcore.fxteam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TeamConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File ARQUIVO_CONFIG = new File(CONFIG_DIR, "fxteam.json");

    public static Messages MESSAGES = new Messages();

    public static class Messages {
        // --- Channel Formatting / Tag ---
        public String teamPrefixFormat = "§7[{TAG_COLOR}{TAG}§7] ";
        public String teamChatFormat = "§8[§aTeam Chat§8] {TAG_COLOR}[{TAG}] §f{PLAYER}§7: §f{MESSAGE}";
        public String teamSpyFormat = "§8[§cTeamSpy§8] {TAG_COLOR}[{TAG}] §f{PLAYER}§7: §f{MESSAGE}";

        // --- /fxteam Messages ---
        public String alreadyInTeam = "§cYou are already in a team! Leave your current team before creating a new one.";
        public String alreadyLeader = "§cYou already have a registered team!";
        public String tagLengthExceeded = "§cTeam tag must be at most {MAX_LENGTH} characters long!";
        public String teamCreatedSuccess = "§aTeam §f{NAME} §a[{TAG_COLOR}{TAG}§a] created successfully!";
        public String teamCreateError = "§cError creating team. Name unavailable.";

        // --- Invite Messages ---
        public String inviteOnlyLeader = "§cOnly the team leader can send invites.";
        public String targetAlreadyInTeam = "§cThe player §f{PLAYER} §calready belongs to a team!";
        public String inviteSent = "§aInvite sent to §f{PLAYER}";
        public String inviteReceived = "§aYou have been invited to join team §f{NAME}!\n§e[CLICK HERE TO ACCEPT]";
        public String inviteHoverText = "§7Click to accept the invite";
        public String noPendingInvites = "§cYou have no pending invites.";
        public String teamNoLongerExists = "§cThe inviting team no longer exists.";
        public String joinedTeamSuccess = "§aYou joined team §f{NAME}";

        // --- Kick / Leave Messages ---
        public String removeOnlyLeader = "§cOnly the team leader can remove members.";
        public String cannotRemoveSelf = "§cYou cannot remove yourself! Use /fxteam delete to delete the team.";
        public String playerRemovedLeader = "§aPlayer §f{PLAYER} §a has been removed from the team.";
        public String playerRemovedTarget = "§cYou were removed from team §f{NAME}";
        public String notInTeam = "§cYou are not in any team.";
        public String leaderCannotLeave = "§cLeaders cannot leave the team. Use /fxteam delete to delete the team.";
        public String playerLeft = "§eYou left team §f{NAME}";

        // --- Modification Messages ---
        public String modifyOnlyLeader = "§cOnly the leader can change team settings.";
        public String colorSuccess = "§aTeam color changed to: {COLOR_NAME}";
        public String invalidColor = "§cInvalid color! Accepted colors: red, green, blue, yellow, gold, aqua, purple, white...";
        public String tagUpdateSuccess = "§aTeam tag updated to: {TAG_FORMATTED}";
        public String tagColorSuccess = "§aTag color changed successfully!";

        // --- Delete / Info Messages ---
        public String deleteOnlyLeader = "§cOnly the leader can delete the team.";
        public String teamDeletedSuccess = "§aTeam deleted successfully!";
        public String teamInfoHeader = "§8=== §aTeam Information §8===";
        public String teamInfoName = "§eName: §f{NAME}";
        public String teamInfoTag = "§eTag: {TAG_FORMATTED}";
        public String teamInfoMembers = "§eMembers: §f{MEMBERS_COUNT}";

        // --- Admin Messages ---
        public String admListHeader = "§8=== §c[FXTeam ADM] Team List §8===";
        public String admListItem = "§7- ID: §f{ID} §7| Name: §f{NAME} §7| Members: §a{MEMBERS_COUNT}";
        public String admForceDeleteSuccess = "§aTeam §f{ID} §adeleted successfully!";
        public String admForceDeleteNotFound = "§cTeam not found with the specified ID.";
        public String spyEnabled = "§a[FXTeam Spy] Enabled! You will see chat messages from all teams.";
        public String spyDisabled = "§c[FXTeam Spy] Disabled.";
    }

    public static void carregar() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        if (ARQUIVO_CONFIG.exists()) {
            try (FileReader reader = new FileReader(ARQUIVO_CONFIG)) {
                Messages carregado = GSON.fromJson(reader, Messages.class);
                if (carregado != null) {
                    MESSAGES = carregado;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            salvar();
        }
    }

    public static void salvar() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(ARQUIVO_CONFIG)) {
                GSON.toJson(MESSAGES, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}