package bz.fxcore.core.database;

import bz.fxcore.FXCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = FXCore.MODID)
public class PlayerConnectionHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());

            // 1. Verificação de Banimento
            if (data.isBanned) {
                if (data.banExpireTimestamp > 0L && System.currentTimeMillis() > data.banExpireTimestamp) {
                    // Ban temporário expirou
                    data.isBanned = false;
                    data.banReason = "";
                    data.banAuthor = "";
                    data.banExpireTimestamp = 0L;
                } else {
                    String reason = data.banReason != null && !data.banReason.isEmpty() ? data.banReason : "Não especificado";
                    String author = data.banAuthor != null && !data.banAuthor.isEmpty() ? data.banAuthor : "Administração";
                    player.connection.disconnect(net.minecraft.network.chat.Component.literal(
                        "§c[FXCore] Você está banido deste servidor!\n§7Motivo: §f" + reason + "\n§7Autor: §f" + author
                    ));
                    return;
                }
            }

            // 2. Atualização de dados de acesso
            data.lastConnectionTimestamp = System.currentTimeMillis();
            String currentIp = player.getIpAddress();
            String currentName = player.getName().getString();

            data.lastName = currentName;
            if (!data.ipHistory.contains(currentIp)) {
                data.ipHistory.add(currentIp);
            }

            PlayerDataManager.save(data);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataManager.savePlayerData(player.getUUID());
        }
    }
}