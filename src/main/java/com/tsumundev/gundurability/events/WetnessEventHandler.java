package com.tsumundev.gundurability.events;

import com.tacz.guns.item.ModernKineticGunItem;
import com.tsumundev.gundurability.util.GunNBTUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gestionnaire de l'humidité des armes.
 * Les armes deviennent humides quand le joueur est dans l'eau ou sous la pluie.
 */
@Mod.EventBusSubscriber
public class WetnessEventHandler {

    private static final long WETNESS_UPDATE_INTERVAL = 20; // Tous les 1 seconde (20 ticks)
    private static final double WETNESS_IN_WATER = 0.15;    // +15% par seconde dans l'eau
    private static final double WETNESS_IN_RAIN = 0.05;     // +5% par seconde sous la pluie
    private static final double WETNESS_DRYING = 0.02;      // -2% par seconde au sec (abri)
    private static final double WETNESS_EVAPORATION = 0.005;// -0.5% par seconde (normal)

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;

        // Update toutes les secondes
        if (player.level().getGameTime() % WETNESS_UPDATE_INTERVAL != 0) {
            return;
        }

        updateGunWetness(player, player.getMainHandItem());
        updateGunWetness(player, player.getOffhandItem());
    }

    private static void updateGunWetness(Player player, ItemStack gunStack) {
        if (!(gunStack.getItem() instanceof ModernKineticGunItem)) {
            return;
        }

        if (!GunNBTUtil.hasDurability(gunStack)) {
            return;
        }

        boolean isInWater = player.isInWater();
        boolean isRaining = player.level().isRaining() && player.level().canSeeSky(player.blockPosition());
        boolean isOutside = player.level().canSeeSky(player.blockPosition());

        double currentWetness = GunNBTUtil.getWetness(gunStack);

        double newWetness = currentWetness;

        if (isInWater) {
            // Dans l'eau: l'arme se mouille rapidement
            newWetness += WETNESS_IN_WATER;
            GunNBTUtil.setLastRainTick(gunStack, player.level().getGameTime());
        } else if (isRaining && isOutside) {
            // Sous la pluie: l'arme se mouille lentement
            newWetness += WETNESS_IN_RAIN;
            GunNBTUtil.setLastRainTick(gunStack, player.level().getGameTime());
        } else {
            // Au sec: l'arme sèche
            long lastRainTick = GunNBTUtil.getLastRainTick(gunStack);
            long ticksSinceRain = player.level().getGameTime() - lastRainTick;

            if (ticksSinceRain > 200) { // Après 10 secondes au sec, séchage rapide
                newWetness -= WETNESS_DRYING;
            } else {
                newWetness -= WETNESS_EVAPORATION;
            }
        }

        GunNBTUtil.setWetness(gunStack, newWetness);
    }
}
