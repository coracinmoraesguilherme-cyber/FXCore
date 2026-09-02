package bz.fxcore.core.otimi.server;

import bz.fxcore.FXCore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class FXTaskExecutor {

    // ThreadPool dedicada para tarefas assíncronas do servidor
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "FXCore-AsyncThread-" + count.getAndIncrement());
                thread.setDaemon(true); // Permite que o servidor feche sem travar a JVM
                return thread;
            }
        }
    );

    /**
     * Executa uma tarefa pesada de forma assíncrona (fora da Main Thread).
     * Ideal para: leitura/escrita de arquivos de jogadores, banco de dados, webhooks, etc.
     */
    public static void runAsync(Runnable task) {
        ASYNC_EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                FXCore.LOGGER.error("Erro ao executar tarefa assíncrona no FXCore:", e);
            }
        });
    }

    /**
     * Encerra a ThreadPool de forma limpa ao desligar o servidor.
     */
    public static void shutdown() {
        ASYNC_EXECUTOR.shutdown();
    }
}