package bz.fxcore.core.otimi;

import bz.fxcore.core.otimi.server.FXServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class SmartAFKFarm {

    private static final Map<UUID, PlayerData> PLAYER_MAP = new ConcurrentHashMap<>();

    private static class PlayerData {
        long lastActivityTime = System.currentTimeMillis();
        int countdownSeconds = 10;
        long lastCountdownTick = 0;
        boolean inCountdown = false;
        Vec3 lastPos = Vec3.ZERO;
        float lastYaw = 0;
        float lastPitch = 0;

        final Queue<Long> clickTimestamps = new LinkedList<>();

        void resetActivity() {
            this.lastActivityTime = System.currentTimeMillis();
            this.countdownSeconds = 10;
            this.inCountdown = false;
        }

        boolean recordClickAndCheckMacro(long now) {
            clickTimestamps.add(now);
            if (clickTimestamps.size() > 20) {
                clickTimestamps.poll();
            }

            if (clickTimestamps.size() >= 10) {
                long oldest = clickTimestamps.peek();
                double durationSeconds = (now - oldest) / 1000.0;
                if (durationSeconds > 0) {
                    double cps = clickTimestamps.size() / durationSeconds;
                    if (cps > FXServerConfig.DATA.cpsThreshold) {
                        return true;
                    }
                }

                if (clickTimestamps.size() == 20 && isMacroPattern()) {
                    return true;
                }
            }
            return false;
        }

        private boolean isMacroPattern() {
            Long[] times = clickTimestamps.toArray(new Long[0]);
            long[] intervals = new long[times.length - 1];
            long sum = 0;

            for (int i = 0; i < intervals.length; i++) {
                intervals[i] = times[i + 1] - times[i];
                sum += intervals[i];
            }

            double mean = (double) sum / intervals.length;
            double variance = 0;

            for (long interval : intervals) {
                variance += Math.pow(interval - mean, 2);
            }

            double stdDev = Math.sqrt(variance / intervals.length);
            return stdDev < 3.0; // Desvio padrão extremamente baixo indica intervalo robótico/macro
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_MAP.put(player.getUUID(), new PlayerData());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_MAP.remove(player.getUUID());
        }
    }

    private static void handleMacroCheck(ServerPlayer player, Runnable cancelAction) {
        PlayerData data = PLAYER_MAP.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        
        if (data.inCountdown) {
            player.displayClientMessage(Component.literal(FXServerConfig.DATA.messages.afkCancelMessage), true);
        }
        data.resetActivity();

        if (data.recordClickAndCheckMacro(System.currentTimeMillis())) {
            cancelAction.run();
            player.displayClientMessage(
                Component.literal(FXServerConfig.DATA.messages.autoClickerWarning),
                true
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!FXServerConfig.DATA.enableAfkFarmGuard || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (FXServerConfig.DATA.ignoreStaffAfk && player.hasPermissions(2)) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) return;

        PlayerData data = PLAYER_MAP.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        long now = System.currentTimeMillis();

        Vec3 currentPos = player.position();
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        boolean isPassenger = player.isPassenger();
        boolean isInWater = player.isInWater() || player.isInFluidType();
        boolean cameraMoved = Math.abs(currentYaw - data.lastYaw) > 0.5f || Math.abs(currentPitch - data.lastPitch) > 0.5f;

        boolean wasManualMove = currentPos.distanceToSqr(data.lastPos) > 0.002 && !isPassenger && !isInWater && cameraMoved;

        data.lastPos = currentPos;
        data.lastYaw = currentYaw;
        data.lastPitch = currentPitch;

        if (wasManualMove) {
            if (data.inCountdown) {
                player.displayClientMessage(Component.literal(FXServerConfig.DATA.messages.afkCancelMessage), true);
            }
            data.resetActivity();
            return;
        }

        long idleTime = now - data.lastActivityTime;
        long maxIdleMs = FXServerConfig.DATA.maxAfkClickSeconds * 1000L;

        if (idleTime >= maxIdleMs) {
            data.inCountdown = true;

            if (now - data.lastCountdownTick >= 1000L) {
                data.lastCountdownTick = now;

                if (data.countdownSeconds > 0) {
                    String formattedMsg = String.format(FXServerConfig.DATA.messages.afkHudMessage, data.countdownSeconds);
                    player.displayClientMessage(Component.literal(formattedMsg), true);

                    player.playNotifySound(
                        SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS,
                        1.0f,
                        1.5f
                    );

                    data.countdownSeconds--;
                } else {
                    kickAndNotifyStaff(player);
                }
            }
        }
    }

    private static void kickAndNotifyStaff(ServerPlayer player) {
        player.connection.disconnect(Component.literal(FXServerConfig.DATA.messages.afkKickMessage));

        String formattedStaffAlert = String.format(FXServerConfig.DATA.messages.staffAfkAlert, player.getScoreboardName());
        Component staffAlert = Component.literal(formattedStaffAlert);
        
        if (player.getServer() != null) {
            player.getServer().getPlayerList().getPlayers().forEach(p -> {
                if (p.hasPermissions(2)) {
                    p.sendSystemMessage(staffAlert);
                }
            });
        }
    }
}