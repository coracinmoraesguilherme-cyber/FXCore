package bz.fxcore.modules.staff;

import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class StaffEvents {

    // Comandos de comunicação alternativos bloqueados pelo Mute
    private static final Set<String> CHAT_COMMANDS = Set.of(
        "g", "l", "tell", "msg", "w", "r", "me", "tc"
    );

    private static final double LOCAL_CHAT_RADIUS = 100.0; // Raio em blocos para o chat local

    // Intercepta comandos de mensagem para validar Freeze e Mute
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            String commandName = event.getParseResults().getReader().getString().toLowerCase();
            
            if (commandName.startsWith("/")) {
                commandName = commandName.substring(1);
            }
            
            String baseCommand = commandName.split(" ")[0];

            // 1. Checagem de Freeze para comandos
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            if (data != null && data.isFrozen) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode usar comandos."));
                return;
            }

            // 2. Checagem de Mute para comandos de chat
            if (CHAT_COMMANDS.contains(baseCommand)) {
                if (FXStaffData.isMuted(player.getUUID())) {
                    event.setCanceled(true);
                    long remaining = FXStaffData.getMuteRemainingTime(player.getUUID());
                    String timeText = TimeUtil.formatTime(remaining);

                    player.sendSystemMessage(Component.literal("§c[FXCore] Você está mutado! Tempo restante: §f" + timeText));
                }
            }
        }
    }


    // Trava a movimentação do jogador congelado
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());

            if (data != null && data.isFrozen) {
                player.setDeltaMovement(new Vec3(0, player.getDeltaMovement().y < 0 ? player.getDeltaMovement().y : 0, 0));
                player.hurtMarked = true;
            }
        }
    }

    // Impede abertura de inventários/mochilas quando congelado
    @SubscribeEvent
    public static void onOpenContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());

            if (data != null && data.isFrozen) {
                player.closeContainer();
                player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode abrir inventários ou mochilas."));
            }
        }
    }

    // Bloqueios de interações físicas
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            if (data != null && data.isFrozen) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode interagir!"));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            if (data != null && data.isFrozen) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode interagir!"));
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            if (data != null && data.isFrozen) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode interagir!"));
            }
        }
    }
}