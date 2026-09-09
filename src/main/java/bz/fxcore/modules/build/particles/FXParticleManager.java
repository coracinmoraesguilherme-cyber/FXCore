package bz.fxcore.modules.build.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class FXParticleManager {

    private static final Map<UUID, ParticleTask> ACTIVE_TASKS = new HashMap<>();

    public static void startTask(ServerPlayer player, String presetName, ParticleOptions particle, float[] delta, int durationSeconds, String animType) {
        ParticleShapeJson shape = ParticleShapeJson.get(presetName);
        if (shape == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[FXCore] Preset de forma '" + presetName + "' não encontrado em config/fxcore/particles/shapes/!"));
            return;
        }

        ACTIVE_TASKS.put(player.getUUID(), new ParticleTask(
            player.serverLevel(),
            player,
            shape,
            particle,
            delta,
            durationSeconds * 20, // Converte segundos para ticks
            animType.toLowerCase()
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
        private final ParticleShapeJson shape;
        private final ParticleOptions particle;
        private final float[] delta;
        private int remainingTicks;
        private final String animType;
        private int tickCounter = 0;

        public ParticleTask(ServerLevel level, ServerPlayer player, ParticleShapeJson shape, ParticleOptions particle, float[] delta, int totalTicks, String animType) {
            this.level = level;
            this.player = player;
            this.shape = shape;
            this.particle = particle;
            this.delta = delta;
            this.remainingTicks = totalTicks;
            this.animType = animType;
        }

        public boolean tick() {
            remainingTicks--;
            tickCounter++;

            Vec3 pos = player.position().add(0, 0.1, 0); // Perto dos pés ou tronco

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

                if (animType.equals("rotate") || animType.equals("s") || animType.equals("true")) {
                    rotAngle = tickCounter * 0.1; // Gira continuamente
                } else if (animType.equals("pulse") || animType.equals("pulsar")) {
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
    public static void startTaskAt(ServerPlayer player, String preset, net.minecraft.core.particles.ParticleOptions particleOpt, net.minecraft.world.phys.Vec3 pos, float[] delta, int durationSec, String anim) {
    // Se você já tiver um método principal, chame-o passando a posição 'pos'
    // Caso contrário, implemente a lógica de spawn das partículas utilizando as coordenadas de 'pos'
    }
}