package bz.fxcore.modules.build.particles;

import bz.fxcore.FXCore;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FXCore.MODID)
public class FXParticleManager {

    private static final Map<UUID, ParticleTask> ACTIVE_TASKS = new ConcurrentHashMap<>();

    public static void startTask(ServerPlayer player, String presetName, ParticleOptions particle, float[] delta, int durationSeconds, String animType) {
        startTaskAt(player, presetName, particle, null, delta, durationSeconds, animType);
    }

    public static void startTaskAt(ServerPlayer player, String presetName, ParticleOptions particleOpt, Vec3 pos, float[] delta, int durationSec, String anim) {
        ParticleShapeJson shape = ParticleShapeJson.get(presetName);
        if (shape == null) {
            player.sendSystemMessage(Component.literal("§c[FXCore] Preset de forma '" + presetName + "' não encontrado em config/fxcore/particles/shapes/!"));
            return;
        }

        ACTIVE_TASKS.put(player.getUUID(), new ParticleTask(
            player.serverLevel(),
            player,
            pos,
            shape,
            particleOpt,
            delta,
            durationSec * 20, // Converte segundos para ticks
            anim != null ? anim.toLowerCase() : "default"
        ));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_TASKS.isEmpty()) return;

        ACTIVE_TASKS.entrySet().removeIf(entry -> {
            ParticleTask task = entry.getValue();
            boolean finished = task.tick();
            return finished || !task.player.isAlive() || task.player.hasDisconnected();
        });
    }

    private static class ParticleTask {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Vec3 fixedPos;
        private final ParticleShapeJson shape;
        private final ParticleOptions particle;
        private final float[] delta;
        private int remainingTicks;
        private final String animType;
        private int tickCounter = 0;

        public ParticleTask(ServerLevel level, ServerPlayer player, Vec3 fixedPos, ParticleShapeJson shape, ParticleOptions particle, float[] delta, int totalTicks, String animType) {
            this.level = level;
            this.player = player;
            this.fixedPos = fixedPos;
            this.shape = shape;
            this.particle = particle;
            this.delta = delta;
            this.remainingTicks = totalTicks;
            this.animType = animType;
        }

        public boolean tick() {
            remainingTicks--;
            tickCounter++;

            Vec3 pos = (fixedPos != null) ? fixedPos : player.position().add(0, 0.1, 0);

            // PRIORIDADE 1: Se o JSON tiver "frames", executa a animação quadro a quadro (tipo GIF)
            if (shape.frames != null && !shape.frames.isEmpty()) {
                int frameIndex = (tickCounter / 5) % shape.frames.size(); // Troca de frame a cada 5 ticks
                List<List<Double>> currentFrame = shape.frames.get(frameIndex);
                renderPoints(currentFrame, pos, 0, 0);
            } 
            // PRIORIDADE 2: Se usar "points", aplica os comportamentos automáticos baseados no animType ou padrão
            else if (shape.points != null && !shape.points.isEmpty()) {
                double rotAngle = 0;
                double scaleOffset = 1.0;

                if ("rotate".equals(animType) || "s".equals(animType) || "true".equals(animType)) {
                    rotAngle = tickCounter * 0.1; // Gira continuamente
                } else if ("pulse".equals(animType) || "pulsar".equals(animType)) {
                    scaleOffset = 1.0 + (Math.sin(tickCounter * 0.2) * 0.2); // Efeito de respiração/pulso
                }

                renderPoints(shape.points, pos, rotAngle, scaleOffset);
            }

            return remainingTicks <= 0;
        }

        private void renderPoints(List<List<Double>> points, Vec3 center, double rotAngle, double scale) {
            double cos = Math.cos(rotAngle);
            double sin = Math.sin(rotAngle);

            for (List<Double> pt : points) {
                if (pt.size() < 3) continue;

                double px = pt.get(0) * scale;
                double py = pt.get(1) * scale;
                double pz = pt.get(2) * scale;

                // Aplica rotação no eixo Y se houver
                double finalX = px * cos - pz * sin;
                double finalZ = px * sin + pz * cos;

                double spawnX = center.x + finalX;
                double spawnY = center.y + py;
                double spawnZ = center.z + finalZ;

                level.sendParticles(particle, spawnX, spawnY, spawnZ, 1, delta[0], delta[1], delta[2], 0.0);
            }
        }
    }
}