package com.tsumundev.gundurability.network;

import com.tsumundev.gundurability.config.Config;
import com.tsumundev.gundurability.config.ConfigClient;
import com.tsumundev.gundurability.util.GunNBTUtil;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class InspectDurabilityMessage {

    public InspectDurabilityMessage() {
    }

    public InspectDurabilityMessage(FriendlyByteBuf buffer) {
    }

    public static void buffer(InspectDurabilityMessage message, FriendlyByteBuf buffer) {
    }

    public static void handler(InspectDurabilityMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            pressAction(context.getSender());
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(ServerPlayer entity) {
        ItemStack heldItem = entity.getMainHandItem();

        if (!(heldItem.getItem() instanceof ModernKineticGunItem)) {
            return;
        }

        if (!GunNBTUtil.hasDurability(heldItem)) {
            return;
        }

        // Si l'arme est enrayée
        if (GunNBTUtil.isJammed(heldItem)) {
            int progress = GunNBTUtil.getUnjamProgress(heldItem);
            int required = GunNBTUtil.getRequiredUnjam(heldItem);

            // Initialiser les requirements si ce n'est pas fait
            if (required <= 1) {
                GunNBTUtil.setupUnjamRequirements(heldItem);
                required = GunNBTUtil.getRequiredUnjam(heldItem);
            }

            progress++;
            GunNBTUtil.setUnjamProgress(heldItem, progress);

            if (progress >= required) {
                // Désenrayage complet
                GunNBTUtil.unjam(heldItem);
                GunNBTUtil.setUnjamProgress(heldItem, 0);
                GunNBTUtil.setRequiredUnjam(heldItem, 1);

                if (Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                    String jamClearedText = ConfigClient.TEXT_JAM_CLEARED.get();
                    entity.displayClientMessage(MutableComponent.create(Component.literal(jamClearedText).getContents()).withStyle(ChatFormatting.YELLOW), true);
                }
            } else {
                // Progression partielle
                if (Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                    entity.displayClientMessage(Component.literal("Inspection en cours... (" + progress + "/" + required + ")")
                        .withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        com.tsumundev.gundurability.Gundurability.addNetworkMessage(InspectDurabilityMessage.class, InspectDurabilityMessage::buffer, InspectDurabilityMessage::new, InspectDurabilityMessage::handler);
    }
}
