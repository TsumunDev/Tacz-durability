package com.tsumundev.gundurability.command;

import com.tsumundev.gundurability.config.Config;
import com.tsumundev.gundurability.util.GunNBTUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tacz.guns.item.ModernKineticGunItem;

@Mod.EventBusSubscriber
public class TaczDurabilityDebugCommands {

    private static final String PREFIX = "taczd";

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(PREFIX)
            .requires(source -> source.hasPermission(2))

            .then(Commands.literal("jam")
                .executes(context -> {
                    return jamHeldGun(context.getSource());
                })
                .then(Commands.literal("all")
                    .executes(context -> {
                        return jamAllGuns(context.getSource());
                    })
                )
            )

            .then(Commands.literal("reload")
                .executes(context -> {
                    return reloadConfigs(context.getSource());
                })
            );

        event.getDispatcher().register(root);
    }

    private static int jamHeldGun(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Cette commande ne peut être utilisée que par un joueur."));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            source.sendFailure(Component.literal("Vous ne tenez pas d'arme TACZ en main."));
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
            source.sendFailure(Component.literal("Cette commande ne peut être utilisée que par un joueur."));
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

        final int finalCount = jammedCount; source.sendSuccess(() -> Component.literal("§a[Debug] " + finalCount + " arme(s) enrayée(s) dans l'inventaire."), true);

        return jammedCount;
    }

    private static int reloadConfigs(CommandSourceStack source) {
        try {
            Config.refreshCache();

            source.sendSuccess(() -> Component.literal("§a[Debug] Configurations rechargées depuis les fichiers TOML!"), true);
            source.sendSuccess(() -> Component.literal("§eLes fichiers TOML ont été relus avec succès."), false);

            source.sendSuccess(() -> Component.literal("§e- useConfig: " + Config.USE_GUN_TYPE_CONFIG.get()), false);
            source.sendSuccess(() -> Component.literal("§e- jamMultiplier (auto/burst/semi): " +
                Config.JAM_MULTIPLIER_AUTO.get() + "/" +
                Config.JAM_MULTIPLIER_BURST.get() + "/" +
                Config.JAM_MULTIPLIER_SEMI.get()), false);
            source.sendSuccess(() -> Component.literal("§e- showImmersiveMessages: " + Config.SHOW_IMMERSIVE_MESSAGES.get()), false);
            source.sendSuccess(() -> Component.literal("§e- defaultMaxDurability: " + Config.DEFAULT_MAX_DURABILITY.get()), false);

            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[Debug] Erreur lors du rechargement: " + e.getMessage()));
            return 0;
        }
    }
}
