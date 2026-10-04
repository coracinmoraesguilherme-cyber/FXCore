package bz.fxcore.modules.rp;

import bz.fxcore.FXCore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FXCore.MODID)
public class RPTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Executa a cada 20 ticks (1 segundo) para não sobrecarregar o chat/packet
            if (player.tickCount % 20 == 0) {
                if (RPManager.isOff(player.getUUID())) {
                    player.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.literal("§c§lOFF RP")
                    ));
                }
            }
        }
    }
}