package bz.fxcore.modules.rp;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class RPTickHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Executa a cada 20 ticks (1 segundo) para não sobrecarregar o chat/packet
            if (player.getServer().getTickCount() % 20 == 0) {
                if (RPManager.isOff(player.getUUID())) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.literal("§c§lOFF RP")
                    ));
                }
            }
        }
    }
}