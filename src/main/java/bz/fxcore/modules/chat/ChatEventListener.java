package bz.fxcore.modules.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class ChatEventListener {

    public static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        event.setCanceled(true);

        ServerPlayer player = event.getPlayer();
        String mensagem = event.getRawText();

        String canalAtual = ChatCommand.CANAL_ATUAL_JOGADOR.getOrDefault(player.getUUID(), "l");
        ChatChannel canal = ChannelManager.CHANNELS.get(canalAtual.toLowerCase());

        if (canal == null) {
            canal = ChannelManager.CHANNELS.get("l");
        }

        if (canal != null && canal.getSlow() > 0 && !player.hasPermissions(2)) {
            long agora = System.currentTimeMillis();
            long ultimoEnvio = LAST_MESSAGE_TIME.getOrDefault(player.getUUID(), 0L);
            long tempoEspera = (long) (canal.getSlow() * 1000);

            if (agora - ultimoEnvio < tempoEspera) {
                double restante = ((tempoEspera - (agora - ultimoEnvio)) / 1000.0);
                player.sendSystemMessage(Component.literal(
                    String.format("§c[Chat] Aguarde %.1fs para enviar outra mensagem neste canal!", restante)
                ));
                return;
            }
            LAST_MESSAGE_TIME.put(player.getUUID(), agora);
        }

        ChatCommand.enviarMensagemCanal(player, canalAtual, mensagem);
    }
}