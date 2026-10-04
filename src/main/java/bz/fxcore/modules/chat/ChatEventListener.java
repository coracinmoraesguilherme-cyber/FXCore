package bz.fxcore.modules.chat;

import bz.fxcore.FXCore;
import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.modules.staff.FXStaffData;
import bz.fxcore.modules.staff.TimeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FXCore.MODID)
public class ChatEventListener {

    public static final Map<UUID, Long> LAST_MESSAGE_TIME = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        event.setCanceled(true);

        ServerPlayer player = event.getPlayer();
        UUID uuid = player.getUUID();

        // 1. Checagem de Mute
        if (FXStaffData.isMuted(uuid)) {
            long remaining = FXStaffData.getMuteRemainingTime(uuid);
            String timeText = (remaining <= 0 || remaining == Long.MAX_VALUE) ? "Permanente" : TimeUtil.formatTime(remaining);
            player.sendSystemMessage(Component.literal("§c[FXCore] Você está mutado! Tempo restante: §f" + timeText));
            return;
        }

        // 2. Checagem de Freeze
        FXPlayerData data = PlayerDataManager.get(uuid);
        if (data != null && data.isFrozen) {
            player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode enviar mensagens."));
            return;
        }

        String mensagem = event.getRawText();

        String canalAtual = ChatCommand.CANAL_ATUAL_JOGADOR.getOrDefault(uuid, "l");
        ChatChannel canal = ChannelManager.CHANNELS.get(canalAtual.toLowerCase());

        if (canal == null) {
            canal = ChannelManager.CHANNELS.get("l");
        }

        // 3. Checagem de Slowmode
        if (canal != null && canal.getSlow() > 0 && !player.hasPermissions(2)) {
            long agora = System.currentTimeMillis();
            long ultimoEnvio = LAST_MESSAGE_TIME.getOrDefault(uuid, 0L);
            long tempoEspera = (long) (canal.getSlow() * 1000);

            if (agora - ultimoEnvio < tempoEspera) {
                double restante = ((tempoEspera - (agora - ultimoEnvio)) / 1000.0);
                player.sendSystemMessage(Component.literal(
                    String.format("§c[Chat] Aguarde %.1fs para enviar outra mensagem neste canal!", restante)
                ));
                return;
            }
            LAST_MESSAGE_TIME.put(uuid, agora);
        }

        ChatCommand.enviarMensagemCanal(player, canalAtual, mensagem);
    }

    // Limpeza de sessão no logout para prevenir vazamento de memória
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            LAST_MESSAGE_TIME.remove(uuid);
            ChatCommand.CANAL_ATUAL_JOGADOR.remove(uuid);
            ChatCommand.ULTIMA_CONVERSA.remove(uuid);
        }
    }
}