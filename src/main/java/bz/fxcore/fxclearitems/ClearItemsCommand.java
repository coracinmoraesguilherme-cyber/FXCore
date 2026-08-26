package bz.fxcore.fxclearitems;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearItemsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxclear")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(ClearItemsConfig.DATA.messages.manualClearStarted), true);
                    int removidos = ClearItemsManager.executarLimpeza(context.getSource().getServer());
                    ClearItemsManager.resetTimer();
                    return removidos;
                })
                .then(Commands.literal("status")
                        .executes(context -> {
                            var msg = ClearItemsConfig.DATA.messages;
                            int restantes = ClearItemsManager.getSegundosRestantes();
                            
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusHeader), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusNextClear.replace("{seconds}", String.valueOf(restantes))), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusInterval.replace("{seconds}", String.valueOf(ClearItemsConfig.DATA.intervalSeconds))), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusWarnings.replace("{warnings}", ClearItemsConfig.DATA.warningTimes.toString())), false);
                            return 1;
                        })
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            ClearItemsConfig.carregar();
                            ClearItemsManager.resetTimer();
                            context.getSource().sendSuccess(() -> Component.literal(ClearItemsConfig.DATA.messages.reloadSuccess), true);
                            return 1;
                        })
                )
                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ItemStack item = player.getMainHandItem();

                                    if (item.isEmpty()) {
                                        player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.noItemInHand));
                                        return 0;
                                    }

                                    String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                                    if (ClearItemsConfig.DATA.whitelist.contains(itemId)) {
                                        player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.itemAlreadyInWhitelist.replace("{item}", itemId)));
                                        return 0;
                                    }

                                    ClearItemsConfig.DATA.whitelist.add(itemId);
                                    ClearItemsConfig.salvar();
                                    player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.itemAddedToWhitelist.replace("{item}", itemId)));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("remove")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ItemStack item = player.getMainHandItem();

                                    if (item.isEmpty()) {
                                        player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.noItemInHand));
                                        return 0;
                                    }

                                    String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                                    if (!ClearItemsConfig.DATA.whitelist.contains(itemId)) {
                                        player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.itemNotInWhitelist.replace("{item}", itemId)));
                                        return 0;
                                    }

                                    ClearItemsConfig.DATA.whitelist.remove(itemId);
                                    ClearItemsConfig.salvar();
                                    player.sendSystemMessage(Component.literal(ClearItemsConfig.DATA.messages.itemRemovedFromWhitelist.replace("{item}", itemId)));
                                    return 1;
                                })
                        )
                )
        );
    }
}