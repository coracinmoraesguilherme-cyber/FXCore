package bz.fxcore.modules.clear;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FXClearCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxclear")
            .requires(source -> source.hasPermission(2))

            .then(Commands.literal("status")
                .executes(context -> {
                    int whitelistSize = FXClearConfig.DATA.itemWhitelist.size();
                    int entityListSize = FXClearConfig.DATA.entityTargetList.size();
                    boolean autoClearEnabled = FXClearConfig.DATA.enableAutoClear;
                    
                    // Pega o tempo restante formatado do FXClearTask
                    String timeRemaining = FXClearTask.getFormattedTimeRemaining();
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§8[§aFXClear§8] §7Status do Módulo:\n" +
                        " §e- Auto-Clear: §f" + (autoClearEnabled ? "§aAtivado" : "§cDesativado") + "\n" +
                        " §e- Próxima Limpeza: §f" + timeRemaining + "\n" +
                        " §e- Itens na Whitelist: §f" + whitelistSize + "\n" +
                        " §e- Entidades Alvo: §f" + entityListSize
                    ), false);
                    return 1;
                })
            )

            .then(Commands.literal("now")
                .executes(context -> {
                    int count = FXClearManager.clearEntities(context.getSource().getServer());
                    String msg = FXClearConfig.DATA.clearSuccessMsg.replace("{count}", String.valueOf(count));
                    context.getSource().sendSuccess(() -> Component.literal(msg), true);
                    return 1;
                })
            )

            .then(Commands.literal("whitelist")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Whitelist de Itens: §f" + FXClearConfig.DATA.itemWhitelist), false);
                    return 1;
                })
                .then(Commands.literal("add")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            ItemStack held = player.getMainHandItem();
                            if (held.isEmpty()) {
                                context.getSource().sendFailure(Component.literal("§cVocê precisa estar segurando um item na mão!"));
                                return 0;
                            }
                            String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                            if (!FXClearConfig.DATA.itemWhitelist.contains(itemId)) {
                                FXClearConfig.DATA.itemWhitelist.add(itemId);
                                FXClearConfig.save();
                                context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Item §e" + itemId + " §aadicionado à whitelist!"), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("§cEste item já está na whitelist."));
                            }
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("remove")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            ItemStack held = player.getMainHandItem();
                            if (held.isEmpty()) {
                                context.getSource().sendFailure(Component.literal("§cVocê precisa estar segurando um item na mão!"));
                                return 0;
                            }
                            String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                            if (FXClearConfig.DATA.itemWhitelist.remove(itemId)) {
                                FXClearConfig.save();
                                context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Item §e" + itemId + " §cremovido da whitelist!"), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("§cEste item não está na whitelist."));
                            }
                        }
                        return 1;
                    })
                )
            )

            .then(Commands.literal("entitylist")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Entidades Alvo: §f" + FXClearConfig.DATA.entityTargetList), false);
                    return 1;
                })
                .then(Commands.literal("add")
                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                        .executes(context -> {
                            ResourceLocation rl = ResourceLocationArgument.getId(context, "entity");
                            String entityName = rl.toString();
                            if (!FXClearConfig.DATA.entityTargetList.contains(entityName)) {
                                FXClearConfig.DATA.entityTargetList.add(entityName);
                                FXClearConfig.save();
                                context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Entidade §e" + entityName + " §aadicionada à lista de limpeza!"), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("§cEsta entidade já está na lista."));
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                        .executes(context -> {
                            ResourceLocation rl = ResourceLocationArgument.getId(context, "entity");
                            String entityName = rl.toString();
                            if (FXClearConfig.DATA.entityTargetList.remove(entityName)) {
                                FXClearConfig.save();
                                context.getSource().sendSuccess(() -> Component.literal("§a[FXClear] Entidade §e" + entityName + " §cremovida da lista de limpeza!"), true);
                            } else {
                                context.getSource().sendFailure(Component.literal("§cEsta entidade não foi encontrada na lista."));
                            }
                            return 1;
                        })
                    )
                )
            )
        );
    }
}