package com.tsumundev.gundurability.item;

import com.mojang.datafixers.util.Either;
import com.tsumundev.gundurability.util.GunNBTUtil;
import com.tsumundev.gundurability.util.GunHeatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TooltipEvent {
    @SubscribeEvent
    public static void renderTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        // Tooltip pour les items de réparation
        if (stack.getItem() instanceof RepairItem item) {
            if(Minecraft.getInstance().options.advancedItemTooltips && item.getGunIds() != null) {
                event.getTooltipElements().add(Either.left(Component.literal("-Compatible Guns-").withStyle(ChatFormatting.GRAY)));
                for (String gunId : item.getGunIds()) {
                    event.getTooltipElements().add(Either.left(MutableComponent.create(Component.literal(gunId).getContents()).withStyle(ChatFormatting.DARK_GRAY)));
                }
            }
            return;
        }

        // Tooltip pour les armes TACZ
        if (GunNBTUtil.hasDurability(stack)) {
            addGunTooltip(event, stack);
        }
    }

    private static void addGunTooltip(RenderTooltipEvent.GatherComponents event, ItemStack stack) {
        int durability = GunNBTUtil.getDurability(stack);
        int maxDurability = GunNBTUtil.getMaxDurability(stack);
        double durabilityPercent = GunNBTUtil.getDurabilityPercent(stack);
        boolean isJammed = GunNBTUtil.isJammed(stack);
        boolean isBroken = GunNBTUtil.isBroken(stack);
        double heat = GunHeatManager.getTemperature(stack);
        int heatLevel = GunHeatManager.getHeatLevel(stack);
        double wetness = GunNBTUtil.getWetness(stack);

        // Ligne vide de séparation
        event.getTooltipElements().add(Either.left(Component.literal("")));

        // Barre de durabilité (sans crochets)
        String durabilityBar = getDurabilityBar(durabilityPercent);
        ChatFormatting durabilityColor = getDurabilityColor(durabilityPercent);
        event.getTooltipElements().add(Either.left(Component.literal(durabilityBar + " " + durability + "/" + maxDurability)
            .withStyle(durabilityColor)));

        // État de l'arme (en gris &8)
        Component stateComponent;
        if (isBroken) {
            stateComponent = Component.literal("État: CASSÉ").withStyle(ChatFormatting.DARK_RED);
        } else if (isJammed) {
            int progress = GunNBTUtil.getUnjamProgress(stack);
            int required = GunNBTUtil.getRequiredUnjam(stack);
            if (required <= 1) {
                GunNBTUtil.setupUnjamRequirements(stack);
                required = GunNBTUtil.getRequiredUnjam(stack);
            }
            stateComponent = Component.literal("État: ÉNRAYÉE (" + progress + "/" + required + " checkups)").withStyle(ChatFormatting.RED);
        } else if (durabilityPercent > 0.7) {
            stateComponent = Component.literal("État: Bon état").withStyle(ChatFormatting.DARK_GRAY);
        } else if (durabilityPercent > 0.4) {
            stateComponent = Component.literal("État: Usée").withStyle(ChatFormatting.DARK_GRAY);
        } else if (durabilityPercent > 0.2) {
            stateComponent = Component.literal("État: Très usée").withStyle(ChatFormatting.DARK_GRAY);
        } else {
            stateComponent = Component.literal("État: Critique").withStyle(ChatFormatting.DARK_GRAY);
        }
        event.getTooltipElements().add(Either.left(stateComponent));

        // Chaleur (juste en dessous d'état, en gris &8)
        String heatText = getHeatText(heatLevel);
        event.getTooltipElements().add(Either.left(Component.literal("Chaleur: " + heatText).withStyle(ChatFormatting.DARK_GRAY)));

        // Humidité (juste en dessous de chaleur, en gris &8)
        if (wetness > 0.05) {
            String wetnessText = GunNBTUtil.getWetnessState(stack);
            event.getTooltipElements().add(Either.left(Component.literal("Humidité: " + wetnessText).withStyle(ChatFormatting.DARK_GRAY)));
        }

        // Avertissement si surchauffe
        if (GunHeatManager.isOverheated(stack)) {
            event.getTooltipElements().add(Either.left(Component.literal("⚠️ SURCHAUFFE - Usure x3").withStyle(ChatFormatting.DARK_RED)));
        }
    }

    private static String getDurabilityBar(double percent) {
        int bars = 10;
        int filled = (int) Math.round(bars * percent);
        StringBuilder sb = new StringBuilder();

        ChatFormatting color = getDurabilityColor(percent);
        sb.append(color);

        for (int i = 0; i < filled; i++) {
            sb.append("█");
        }

        sb.append(ChatFormatting.DARK_GRAY);
        for (int i = filled; i < bars; i++) {
            sb.append("░");
        }

        return sb.toString();
    }

    private static ChatFormatting getDurabilityColor(double percent) {
        if (percent > 0.6) return ChatFormatting.GREEN;
        if (percent > 0.3) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static String getHeatText(int level) {
        return switch (level) {
            case 0 -> "Froide";
            case 1 -> "Tiède";
            case 2 -> "Chaude";
            case 3 -> "Très chaude";
            case 4 -> "SURCHAUFFE";
            default -> "Normale";
        };
    }
}
