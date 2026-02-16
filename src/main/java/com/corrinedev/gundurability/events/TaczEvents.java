package com.corrinedev.gundurability.events;


import com.corrinedev.gundurability.Gundurability;
import com.corrinedev.gundurability.config.Config;
import com.corrinedev.gundurability.init.GundurabilityModGameRules;
import com.corrinedev.gundurability.init.GundurabilityModSounds;
import com.corrinedev.gundurability.util.GunDurabilityConstants;
import com.corrinedev.gundurability.util.GunNBTUtil;
import com.corrinedev.gundurability.util.Work;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TaczEvents {
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Component MSG_JAMMED = Component.literal("Enrayée !")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);
    private static final Component MSG_GUN_BROKE = Component.literal("Votre arme s'est cassée")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);

    private static final String UNJAMMING_HANDLE = "gundurability$handlingJamming";
    private static final String WARNING_PLAYED_HANDLE = "gundurability$warningPlayed";

    @SubscribeEvent
    public static void onShootEvent(GunFireEvent event) {
        if (event.getLogicalSide().isServer()) {
            handleServerSide(event);
        } else {
            handleClientSide(event);
        }
    }

    private static void handleServerSide(GunFireEvent event) {
        LivingEntity shooter = event.getShooter();
        ItemStack gunStack = event.getGunItemStack();

        CompoundTag tag = gunStack.getOrCreateTag();

        if (!(shooter instanceof Player) && tag.getBoolean(GunNBTUtil.KEY_JAMMED)) {
            handleUnjammingForNonPlayers(shooter);
        }

        int currentDurability = tag.getInt(GunNBTUtil.KEY_DURABILITY);

        if (currentDurability == 0) {
            String gunId = tag.getString(GunNBTUtil.KEY_GUN_ID);
            int maxDur = Config.getDurability(gunId);
            tag.putInt(GunNBTUtil.KEY_DURABILITY, maxDur);
            currentDurability = maxDur;
        }

        handleDurabilityLogicOptimized(event, shooter, gunStack, tag, currentDurability);
    }

    private static void handleDurabilityLogicOptimized(GunFireEvent event, LivingEntity shooter,
            ItemStack gunStack, CompoundTag tag, int currentDurability) {

        if (tag.getBoolean(GunNBTUtil.KEY_JAMMED) || currentDurability <= 0) {
            handleBrokenOrJammedGun(event, shooter, gunStack, tag);
            return;
        }

        String gunId = tag.getString(GunNBTUtil.KEY_GUN_ID);

        float totalModifier = calculateTotalModifier(gunId, shooter);

        int newDurability = applyDurabilityDamage(currentDurability, totalModifier, gunStack, tag);

        currentDurability = newDurability;

        checkJamWarning(shooter, gunStack, tag, currentDurability, gunId);
        checkAndApplyJam(shooter, gunStack, tag, currentDurability, gunId);
    }

    private static float calculateTotalModifier(String gunId, LivingEntity shooter) {
        float gunModifier = 1.0f;
        Config.DurabilityModifier durabilityModifier = Config.getDurabilityModifier(gunId);
        if (durabilityModifier != null) {
            gunModifier = durabilityModifier.jamMultiplier();
        }

        float biomeModifier = getBiomeModifierOptimized(shooter);

        return gunModifier * biomeModifier;
    }

    private static float getBiomeModifierOptimized(LivingEntity shooter) {
        try {
            var biomeKeyOpt = shooter.level().getBiome(shooter.blockPosition()).unwrapKey();
            if (biomeKeyOpt.isPresent()) {
                String biomeName = biomeKeyOpt.get().location().toString();
                return (float) Config.getBiomeJamMultiplier(biomeName);
            }
        } catch (Exception e) {
        }
        return 1.0f;
    }

    private static int applyDurabilityDamage(int currentDurability, float totalModifier,
            ItemStack gunStack, CompoundTag tag) {

        boolean isBurst = tag.getBoolean("Burst");
        boolean isAuto = tag.getBoolean("Auto");

        float damage;
        if (isAuto) {
            damage = GunDurabilityConstants.SINGLE_SHOT_MULTIPLIER;
        } else if (isBurst) {
            damage = GunDurabilityConstants.BURST_MODE_MULTIPLIER;
        } else {
            damage = GunDurabilityConstants.SINGLE_SHOT_MULTIPLIER;
        }

        int newDurability = currentDurability - Math.round(damage * totalModifier);
        newDurability = Math.max(0, newDurability);

        tag.putInt(GunNBTUtil.KEY_DURABILITY, newDurability);

        return newDurability;
    }

    private static void checkJamWarning(LivingEntity shooter, ItemStack gunStack,
            CompoundTag tag, int currentDurability, String gunId) {

        if (!Config.isUnjammable(gunId) && Config.JAMCHANCE.get() > 0) {
            int maxDurability = tag.getInt("MaxDurability");
            if (maxDurability == 0) {
                maxDurability = Config.getDurability(gunId);
            }

            double durabilityMultiplier = Config.calculateJamChanceMultiplier(currentDurability, maxDurability);

            boolean isAuto = tag.getBoolean("Auto");
            boolean isBurst = tag.getBoolean("Burst");
            double fireModeMultiplier;
            if (isAuto) {
                fireModeMultiplier = Config.JAM_MULTIPLIER_AUTO.get();
            } else if (isBurst) {
                fireModeMultiplier = Config.JAM_MULTIPLIER_BURST.get();
            } else {
                fireModeMultiplier = Config.JAM_MULTIPLIER_SEMI.get();
            }

            double biomeMultiplier = getBiomeModifierOptimized(shooter);
            double totalMultiplier = durabilityMultiplier * fireModeMultiplier * biomeMultiplier;

            double jamProbability = (Config.JAMCHANCE.get() * totalMultiplier) / maxDurability;

            if (jamProbability > 0.15 && !tag.getBoolean(WARNING_PLAYED_HANDLE)) {
                tag.putBoolean(WARNING_PLAYED_HANDLE, true);
                shooter.playSound(GundurabilityModSounds.JAM_WARNING.get(), 0.6f, 1.0f);
            } else if (jamProbability <= 0.05) {
                tag.putBoolean(WARNING_PLAYED_HANDLE, false);
            }
        }
    }

    private static void checkAndApplyJam(LivingEntity shooter, ItemStack gunStack,
            CompoundTag tag, int currentDurability, String gunId) {

        boolean allowJam = !Config.isUnjammable(gunId);

        if (!allowJam || Config.JAMCHANCE.get() == 0) {
            return;
        }

        int maxDurability = tag.getInt("MaxDurability");
        if (maxDurability == 0) {
            maxDurability = Config.getDurability(gunId);
            tag.putInt("MaxDurability", maxDurability);
        }

        double durabilityMultiplier = Config.calculateJamChanceMultiplier(currentDurability, maxDurability);

        boolean isAuto = tag.getBoolean("Auto");
        boolean isBurst = tag.getBoolean("Burst");
        double fireModeMultiplier;
        if (isAuto) {
            fireModeMultiplier = Config.JAM_MULTIPLIER_AUTO.get();
        } else if (isBurst) {
            fireModeMultiplier = Config.JAM_MULTIPLIER_BURST.get();
        } else {
            fireModeMultiplier = Config.JAM_MULTIPLIER_SEMI.get();
        }

        double biomeMultiplier = getBiomeModifierOptimized(shooter);

        double totalMultiplier = durabilityMultiplier * fireModeMultiplier * biomeMultiplier;
        int jamRange = Math.max(1, (int) Math.round((double) maxDurability / (Config.JAMCHANCE.get() * totalMultiplier)));

        if (RANDOM.nextInt(jamRange + 1) == 0) {
            tag.putBoolean(GunNBTUtil.KEY_JAMMED, true);
            shooter.playSound(GundurabilityModSounds.JAMSFX.get());

            if (shooter instanceof Player player && Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                player.displayClientMessage(MSG_JAMMED, true);
            }
        }

        boolean shouldBreak = shooter.level().getGameRules().getBoolean(GundurabilityModGameRules.GUNBREAK)
                || Config.GUNSBREAK.get();

        if (shouldBreak && currentDurability <= 0) {
            shooter.getMainHandItem().setCount(0);
            shooter.playSound(SoundEvents.ITEM_BREAK);

            if (shooter instanceof Player player && Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                player.displayClientMessage(MSG_GUN_BROKE, true);
            }
        }
    }

    private static void handleBrokenOrJammedGun(GunFireEvent event, LivingEntity shooter,
            ItemStack gunStack, CompoundTag tag) {

        boolean isBroken = tag.getInt(GunNBTUtil.KEY_DURABILITY) <= 0;
        boolean shouldBreak = shooter.level().getGameRules().getBoolean(GundurabilityModGameRules.GUNBREAK)
                || Config.GUNSBREAK.get();

        if (shouldBreak && isBroken) {
            shooter.getMainHandItem().setCount(0);
            shooter.playSound(SoundEvents.ITEM_BREAK);

            if (shooter instanceof Player player && Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                player.displayClientMessage(MSG_GUN_BROKE, true);
            }
        }
        event.setCanceled(true);
    }

    private static void handleClientSide(GunFireEvent event) {
        ItemStack gunStack = event.getGunItemStack();
        CompoundTag tag = gunStack.getOrCreateTag();

        if (tag.getBoolean(GunNBTUtil.KEY_JAMMED) || tag.getInt(GunNBTUtil.KEY_DURABILITY) <= 0) {
            event.setCanceled(true);
        }
    }

    public static void handleUnjammingForNonPlayers(LivingEntity entity) {
        if (entity.getPersistentData().getBoolean(UNJAMMING_HANDLE)) {
            return;
        }

        ItemStack mainHand = entity.getMainHandItem();
        if (!(mainHand.getItem() instanceof ModernKineticGunItem)) {
            return;
        }

        CompoundTag tag = mainHand.getOrCreateTag();
        if (!tag.getBoolean(GunNBTUtil.KEY_JAMMED)) {
            return;
        }

        String gunId = tag.getString(GunNBTUtil.KEY_GUN_ID);
        int jamTime = getJamTimeForGunId(gunId);

        entity.getPersistentData().putBoolean(UNJAMMING_HANDLE, true);

        Gundurability.queueServerWork(new Work<>(entity, jamTime) {
            private boolean cancel = false;

            @Override
            public void tick() {
                ItemStack currentHand = entity.getMainHandItem();
                cancel = !(currentHand.getItem() instanceof ModernKineticGunItem);
            }

            @Override
            public void run() {
                if (!cancel) {
                    ItemStack currentHand = entity.getMainHandItem();
                    if (currentHand.getItem() instanceof ModernKineticGunItem) {
                        CompoundTag currentTag = currentHand.getOrCreateTag();
                        currentTag.putBoolean(GunNBTUtil.KEY_JAMMED, false);
                        entity.getPersistentData().putBoolean(UNJAMMING_HANDLE, false);
                    }
                }
            }
        });
    }

    private static int getJamTimeForGun(ItemStack gunStack) {
        String gunId = gunStack.getOrCreateTag().getString(GunNBTUtil.KEY_GUN_ID);
        return getJamTimeForGunId(gunId);
    }

    private static int getJamTimeForGunId(String gunId) {
        Config.DurabilityModifier modifier = Config.getDurabilityModifier(gunId);
        return (modifier != null) ? modifier.jamTime() : GunDurabilityConstants.DEFAULT_UNJAM_TIME_TICKS;
    }
}
