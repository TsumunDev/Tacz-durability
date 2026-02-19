package com.tsumundev.gundurability.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tsumundev.gundurability.config.Config;
import com.tsumundev.gundurability.util.GunNBTUtil;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TaczDurabilityAdminCommands {

    private static final String PREFIX = "taczd";

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(PREFIX)
            .requires(source -> source.hasPermission(2))

            .then(setDurability())
            .then(getDurability())
            .then(inspect())
            .then(repair())
            .then(jam())
            .then(clearjam())
            .then(reload())
            .then(restore())
            .then(listGuns())

            .then(Commands.literal("debug")
                .then(Commands.literal("jam")
                    .executes(ctx -> jamHeldGun(ctx.getSource()))
                    .then(Commands.literal("all")
                        .executes(ctx -> jamAllGuns(ctx.getSource()))
                    )
                )
            );

        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setDurability() {
        return Commands.literal("setdurability")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 999999))
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        return setDurability(ctx.getSource(), target, amount);
                    })
                )
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getDurability() {
        return Commands.literal("getdurability")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return getDurability(ctx.getSource(), target);
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inspect() {
        return Commands.literal("inspect")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return inspect(ctx.getSource(), target);
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> repair() {
        return Commands.literal("repair")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return repair(ctx.getSource(), target, -1);
                })
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 999999))
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        return repair(ctx.getSource(), target, amount);
                    })
                )
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> jam() {
        return Commands.literal("jam")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return jam(ctx.getSource(), target);
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clearjam() {
        return Commands.literal("clearjam")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return clearjam(ctx.getSource(), target);
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reload() {
        return Commands.literal("reload")
            .executes(ctx -> reloadConfigs(ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> restore() {
        return Commands.literal("restore")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return restore(ctx.getSource(), target);
                })
            );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listGuns() {
        return Commands.literal("list")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return listGuns(ctx.getSource(), target);
                })
            );
    }

    private static int setDurability(CommandSourceStack source, Player target, int amount) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        GunNBTUtil.setDurability(heldItem, amount);

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§aDurabilité de l'arme §e" + gunName + " §adéfinie à §e" + amount + "§a pour §e" + target.getName().getString()), true);
        return 1;
    }

    private static int getDurability(CommandSourceStack source, Player target) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        if (!GunNBTUtil.hasDurability(heldItem)) {
            source.sendFailure(Component.literal("§cL'arme n'a pas de donnée de durabilité."));
            return 0;
        }

        int current = GunNBTUtil.getDurability(heldItem);
        int max = GunNBTUtil.getMaxDurability(heldItem);
        double percent = GunNBTUtil.getDurabilityPercent(heldItem) * 100;

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§6=== Durabilité de " + target.getName().getString() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§eArme: §f" + gunName), false);
        source.sendSuccess(() -> Component.literal("§eDurabilité: §f" + current + " / " + max), false);
        source.sendSuccess(() -> Component.literal("§ePourcentage: §f" + String.format("%.1f%%", percent)), false);
        source.sendSuccess(() -> Component.literal("§eEnrayée: §f" + (GunNBTUtil.isJammed(heldItem) ? "§cOUI" : "§aNON")), false);

        return 1;
    }

    private static int inspect(CommandSourceStack source, Player target) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§6§l=== Inspection TACZ Durability ==="), false);
        source.sendSuccess(() -> Component.literal("§7Joueur: §f" + target.getName().getString()), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Arme: §f" + gunName), false);

        if (GunNBTUtil.hasDurability(heldItem)) {
            int current = GunNBTUtil.getDurability(heldItem);
            int max = GunNBTUtil.getMaxDurability(heldItem);
            double percent = GunNBTUtil.getDurabilityPercent(heldItem) * 100;

            String statusColor;
            String statusText;
            if (percent >= 50) {
                statusColor = "§a";
                statusText = "Bon état";
            } else if (percent >= 25) {
                statusColor = "§e";
                statusText = "Usée";
            } else if (percent >= 10) {
                statusColor = "§6";
                statusText = "Très usée";
            } else {
                statusColor = "§c";
                statusText = "Critique";
            }

            source.sendSuccess(() -> Component.literal("§7Durabilité: " + statusColor + current + " / " + max + " (" + String.format("%.1f%%", percent) + ")"), false);
            source.sendSuccess(() -> Component.literal("§7État: " + statusColor + statusText), false);
        } else {
            source.sendSuccess(() -> Component.literal("§cAucune donnée de durabilité!"), false);
        }

        source.sendSuccess(() -> Component.literal("§7Enrayée: " + (GunNBTUtil.isJammed(heldItem) ? "§cOUI §7⚠" : "§aNON")), false);

        String fireMode = GunNBTUtil.getFireMode(heldItem);
        if (fireMode != null && !fireMode.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Mode de tir: §f" + fireMode), false);
        }

        boolean isBroken = GunNBTUtil.isBroken(heldItem);
        if (isBroken) {
            source.sendSuccess(() -> Component.literal("§c§lL'ARME EST CASÉE!"), false);
        }

        source.sendSuccess(() -> Component.literal("§6§l================================="), false);

        return 1;
    }

    private static int repair(CommandSourceStack source, Player target, int amount) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        if (!GunNBTUtil.hasDurability(heldItem)) {
            String gunId = GunNBTUtil.getGunId(heldItem);
            int maxDurability = (gunId != null && !gunId.isEmpty())
                ? Config.getDurability(gunId)
                : Config.DEFAULT_MAX_DURABILITY.get();
            GunNBTUtil.setDurability(heldItem, maxDurability);
        }

        int before = GunNBTUtil.getDurability(heldItem);
        int max = GunNBTUtil.getMaxDurability(heldItem);

        if (amount < 0) {
            GunNBTUtil.setDurability(heldItem, max);
        } else {
            GunNBTUtil.repair(heldItem, amount);
        }

        GunNBTUtil.unjam(heldItem);

        int after = GunNBTUtil.getDurability(heldItem);
        int repaired = after - before;

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        if (amount < 0) {
            source.sendSuccess(() -> Component.literal("§aArme §e" + gunName + " §aréparée complètement et désenrayée pour §e" + target.getName().getString()), true);
        } else {
            source.sendSuccess(() -> Component.literal("§aArme §e" + gunName + " §aréparée de §e" + repaired + " §aet désenrayée pour §e" + target.getName().getString()), true);
        }

        return 1;
    }

    private static int jam(CommandSourceStack source, Player target) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        if (!GunNBTUtil.hasDurability(heldItem)) {
            String gunId = GunNBTUtil.getGunId(heldItem);
            int maxDurability = (gunId != null && !gunId.isEmpty())
                ? Config.getDurability(gunId)
                : Config.MAXDURABILITY.get();
            GunNBTUtil.setDurability(heldItem, maxDurability);
        }

        GunNBTUtil.setJammed(heldItem, true);

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§aArme §e" + gunName + " §aenrayée pour §e" + target.getName().getString()), true);
        return 1;
    }

    private static int clearjam(CommandSourceStack source, Player target) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        if (!GunNBTUtil.isJammed(heldItem)) {
            source.sendFailure(Component.literal("§cL'arme n'est pas enrayée."));
            return 0;
        }

        GunNBTUtil.unjam(heldItem);

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§aEnrayage retiré sur l'arme §e" + gunName + " §apour §e" + target.getName().getString()), true);
        return 1;
    }

    private static int reloadConfigs(CommandSourceStack source) {
        try {
            Config.refreshCache();

            source.sendSuccess(() -> Component.literal("§aConfigurations rechargées depuis les fichiers TOML!"), true);
            source.sendSuccess(() -> Component.literal("§e- useConfig: " + Config.USE_GUN_TYPE_CONFIG.get()), false);
            source.sendSuccess(() -> Component.literal("§e- jamMultiplier: " + Config.JAM_MULTIPLIER_AUTO.get() + "/" + Config.JAM_MULTIPLIER_BURST.get() + "/" + Config.JAM_MULTIPLIER_SEMI.get()), false);
            source.sendSuccess(() -> Component.literal("§e- showImmersiveMessages: " + Config.SHOW_IMMERSIVE_MESSAGES.get()), false);
            source.sendSuccess(() -> Component.literal("§e- defaultMaxDurability: " + Config.DEFAULT_MAX_DURABILITY.get()), false);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErreur lors du rechargement: " + e.getMessage()));
            return 0;
        }
    }

    private static int jamHeldGun(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§cCette commande ne peut être utilisée que par un joueur."));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§cVous ne tenez pas d'arme TACZ en main."));
            return 0;
        }

        if (!GunNBTUtil.hasDurability(heldItem)) {
            String gunId = GunNBTUtil.getGunId(heldItem);
            int maxDurability = (gunId != null && !gunId.isEmpty())
                ? Config.getDurability(gunId)
                : Config.MAXDURABILITY.get();
            GunNBTUtil.setDurability(heldItem, maxDurability);
        }

        GunNBTUtil.setJammed(heldItem, true);

        String gunId = GunNBTUtil.getGunId(heldItem);
        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

        source.sendSuccess(() -> Component.literal("§a[Debug] L'arme a été enrayée: " + gunName), true);

        return 1;
    }

    private static int jamAllGuns(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§cCette commande ne peut être utilisée que par un joueur."));
            return 0;
        }

        int jammedCount = 0;

        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof ModernKineticGunItem) {
                if (!GunNBTUtil.hasDurability(item)) {
                    String gunId = GunNBTUtil.getGunId(item);
                    int maxDurability = (gunId != null && !gunId.isEmpty())
                        ? Config.getDurability(gunId)
                        : Config.MAXDURABILITY.get();
                    GunNBTUtil.setDurability(item, maxDurability);
                }

                GunNBTUtil.setJammed(item, true);
                jammedCount++;
            }
        }

        ItemStack offhandItem = player.getOffhandItem();
        if (offhandItem.getItem() instanceof ModernKineticGunItem) {
            if (!GunNBTUtil.hasDurability(offhandItem)) {
                String gunId = GunNBTUtil.getGunId(offhandItem);
                int maxDurability = (gunId != null && !gunId.isEmpty())
                    ? Config.getDurability(gunId)
                    : Config.MAXDURABILITY.get();
                GunNBTUtil.setDurability(offhandItem, maxDurability);
            }
            GunNBTUtil.setJammed(offhandItem, true);
            jammedCount++;
        }

        final int finalJammedCount = jammedCount;
        source.sendSuccess(() -> Component.literal("§a[Debug] " + finalJammedCount + " arme(s) enrayée(s) dans l'inventaire."), true);

        return jammedCount;
    }

    private static int restore(CommandSourceStack source, Player target) {
        ItemStack heldItem = target.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " ne tient pas d'arme TACZ en main."));
            return 0;
        }

        String gunId = GunNBTUtil.getGunId(heldItem);
        int maxDurability = (gunId != null && !gunId.isEmpty())
            ? Config.getDurability(gunId)
            : Config.DEFAULT_MAX_DURABILITY.get();

        GunNBTUtil.setDurability(heldItem, maxDurability);
        GunNBTUtil.unjam(heldItem);

        String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";
        source.sendSuccess(() -> Component.literal("§aArme §e" + gunName + " §arestaurée à 100% pour §e" + target.getName().getString()), true);
        return 1;
    }

    private static int listGuns(CommandSourceStack source, Player target) {
        source.sendSuccess(() -> Component.literal("§6=== Armes TACZ de " + target.getName().getString() + " ==="), false);

        int count = 0;
        for (ItemStack item : target.getInventory().items) {
            if (item.getItem() instanceof ModernKineticGunItem) {
                String gunId = GunNBTUtil.getGunId(item);
                String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

                if (GunNBTUtil.hasDurability(item)) {
                    int current = GunNBTUtil.getDurability(item);
                    int max = GunNBTUtil.getMaxDurability(item);
                    double percent = GunNBTUtil.getDurabilityPercent(item) * 100;
                    String jammed = GunNBTUtil.isJammed(item) ? " §c[ENRAYÉE]" : "";
                    source.sendSuccess(() -> Component.literal("§e" + gunName + "§f: " + current + "/" + + max + " (" + String.format("%.0f%%", percent) + ")" + jammed), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§e" + gunName + "§7: (pas de durabilité)"), false);
                }
                count++;
            }
        }

        ItemStack offhandItem = target.getOffhandItem();
        if (offhandItem.getItem() instanceof ModernKineticGunItem) {
            String gunId = GunNBTUtil.getGunId(offhandItem);
            String gunName = (gunId != null && !gunId.isEmpty()) ? gunId : "arme inconnue";

            if (GunNBTUtil.hasDurability(offhandItem)) {
                int current = GunNBTUtil.getDurability(offhandItem);
                int max = GunNBTUtil.getMaxDurability(offhandItem);
                double percent = GunNBTUtil.getDurabilityPercent(offhandItem) * 100;
                String jammed = GunNBTUtil.isJammed(offhandItem) ? " §c[ENRAYÉE]" : "";
                source.sendSuccess(() -> Component.literal("§e" + gunName + " §7(main secondaire)§f: " + current + "/" + max + " (" + String.format("%.0f%%", percent) + ")" + jammed), false);
            } else {
                source.sendSuccess(() -> Component.literal("§e" + gunName + " §7(main secondaire)§7: (pas de durabilité)"), false);
            }
            count++;
        }

        final int finalCount = count;
        if (finalCount == 0) {
            source.sendSuccess(() -> Component.literal("§7Aucune arme TACZ trouvée."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7Total: §e" + finalCount + " §7arme(s)"), false);
        }

        source.sendSuccess(() -> Component.literal("§6================================="), false);
        return 1;
    }
}
