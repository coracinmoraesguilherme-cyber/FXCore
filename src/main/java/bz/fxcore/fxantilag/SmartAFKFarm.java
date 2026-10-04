package bz.fxcore.fxantilag;

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
=======
import bz.fxcore.FXCore;
import bz.fxcore.core.config.FXCoreConfig;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
=======
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

=======
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FXCore.MODID)
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
public class SmartAFKFarm {

    private static final Map<UUID, PlayerData> PLAYER_MAP = new ConcurrentHashMap<>();

    private static class PlayerData {
        long lastActivityTime = System.currentTimeMillis();
        int countdownSeconds = 15;
        long lastCountdownTick = 0;
        boolean inCountdown = false;
        Vec3 lastPos = Vec3.ZERO;
        float lastYaw = 0;
        float lastPitch = 0;

        void resetActivity(ServerPlayer player) {
            this.lastActivityTime = System.currentTimeMillis();
            if (this.inCountdown && player != null) {
                player.displayClientMessage(Component.literal(FXCoreConfig.DATA.afk.afkCancelMessage), true);
            }
            this.countdownSeconds = FXCoreConfig.DATA.afk.countdownSeconds;
            this.inCountdown = false;
        }
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java

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
                    if (cps > FXAntiLagConfig.DATA.cpsThreshold) {
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
            return stdDev < 3.0;
        }
=======
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_MAP.put(player.getUUID(), new PlayerData());
        }
    }

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
    private static void handleMacroCheck(ServerPlayer player, boolean isCanceled, Runnable cancelAction) {
        PlayerData data = PLAYER_MAP.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        
        if (data.inCountdown) {
            player.displayClientMessage(Component.literal(FXAntiLagConfig.DATA.afkCancelMessage), true);
        }
        data.resetActivity();

        if (data.recordClickAndCheckMacro(System.currentTimeMillis())) {
            cancelAction.run();
            player.displayClientMessage(
                Component.literal(FXAntiLagConfig.DATA.autoClickerWarning),
                true
            );
=======
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PLAYER_MAP.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            PlayerData data = PLAYER_MAP.get(player.getUUID());
            if (data != null) {
                data.resetActivity(player);
            }
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
            handleMacroCheck(player, event.isCanceled(), () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, event.isCanceled(), () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, event.isCanceled(), () -> event.setCanceled(true));
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleMacroCheck(player, event.isCanceled(), () -> event.setCanceled(true));
=======
            PlayerData data = PLAYER_MAP.get(player.getUUID());
            if (data != null) {
                data.resetActivity(player);
            }
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
        if (!FXAntiLagConfig.DATA.enableAfkFarmGuard || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (FXAntiLagConfig.DATA.ignoreStaffAfk && player.hasPermissions(2)) {
=======
        if (!FXCoreConfig.DATA.afk.enabled || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (FXCoreConfig.DATA.afk.ignoreStaffAfk && player.hasPermissions(2)) {
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        PlayerData data = PLAYER_MAP.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        long now = System.currentTimeMillis();

        Vec3 currentPos = player.position();
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        boolean isPassenger = player.isPassenger();
        boolean isInWater = player.isInWater() || player.isInFluidType();
        boolean cameraMoved = currentYaw != data.lastYaw || currentPitch != data.lastPitch;

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
        boolean wasManualMove = !currentPos.equals(data.lastPos) && !isPassenger && !isInWater && cameraMoved;
=======
        boolean wasManualMove = (currentPos.distanceToSqr(data.lastPos) > 0.002 && !isPassenger && !isInWater) || cameraMoved;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java

        data.lastPos = currentPos;
        data.lastYaw = currentYaw;
        data.lastPitch = currentPitch;

        if (wasManualMove) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
            if (data.inCountdown) {
                player.displayClientMessage(Component.literal(FXAntiLagConfig.DATA.afkCancelMessage), true);
            }
            data.resetActivity();
=======
            data.resetActivity(player);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
            return;
        }

        long idleTime = now - data.lastActivityTime;
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
        // Atualizado para usar os segundos diretamente (multiplicando apenas por 1000L):
        long maxIdleMs = FXAntiLagConfig.DATA.maxAfkClickSeconds * 1000L;
=======
        long maxIdleMs = FXCoreConfig.DATA.afk.maxAfkSeconds * 1000L;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java

        if (idleTime >= maxIdleMs) {
            data.inCountdown = true;

            if (now - data.lastCountdownTick >= 1000L) {
                data.lastCountdownTick = now;

                if (data.countdownSeconds > 0) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
                    String formattedMsg = String.format(FXAntiLagConfig.DATA.afkHudMessage, data.countdownSeconds);
=======
                    String formattedMsg = String.format(FXCoreConfig.DATA.afk.afkHudMessage, data.countdownSeconds);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
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
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxantilag/SmartAFKFarm.java
        player.connection.disconnect(Component.literal(FXAntiLagConfig.DATA.afkKickMessage));

        String formattedStaffAlert = String.format(FXAntiLagConfig.DATA.staffAfkAlert, player.getScoreboardName());
=======
        player.connection.disconnect(Component.literal(FXCoreConfig.DATA.afk.afkKickMessage));

        String formattedStaffAlert = String.format(FXCoreConfig.DATA.afk.staffAfkAlert, player.getScoreboardName());
>>>>>>> Stashed changes:src/main/java/bz/fxcore/core/otimi/SmartAFKFarm.java
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