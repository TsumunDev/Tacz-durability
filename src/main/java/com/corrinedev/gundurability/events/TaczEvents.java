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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber
public class TaczEvents {
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Component MSG_JAMMED = Component.literal("Enrayée !")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.RED);
    private static final Component MSG_GUN_BROKE = Component.literal("Votre arme s'est cassee")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);

    private static final String UNJAMMING_HANDLE = "gundurability$hj";
    private static final String WARNING_PLAYED_HANDLE = "gundurability$wp";

    private static final SoundEvent JAM_SOUND;
    private static final SoundEvent WARNING_SOUND;
    private static final SoundEvent ITEM_BREAK_SOUND;

    private static final float JAM_VOLUME = 4.0f;
    private static final float WARNING_VOLUME = 3.5f;
    private static final float JAM_PITCH = 0.8f;
    private static final float WARNING_PITCH = 0.9f;

    private static final Queue<DelayedSound> DELAYED_SOUNDS = new ArrayDeque<>();

    static {
        JAM_SOUND = GundurabilityModSounds.JAMSFX.get();
        WARNING_SOUND = GundurabilityModSounds.JAM_WARNING.get();
        ITEM_BREAK_SOUND = SoundEvents.ITEM_BREAK;
    }

    @SubscribeEvent
    public static void onShootEvent(GunFireEvent event) {
        if (event.getLogicalSide().isServer()) {
            handleServerSide(event);
        } else {
            handleClientSide(event);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processDelayedSounds();
        }
    }

    private static void processDelayedSounds() {
        while (!DELAYED_SOUNDS.isEmpty()) {
            DelayedSound delayed = DELAYED_SOUNDS.peek();
            if (delayed.tick()) {
                delayed.play();
                DELAYED_SOUNDS.poll();
            } else {
                break;
            }
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

        handleDurabilityLogic(event, shooter, gunStack, tag, currentDurability);
    }

    private static void handleDurabilityLogic(GunFireEvent event, LivingEntity shooter,
            ItemStack gunStack, CompoundTag tag, int currentDurability) {

        if (tag.getBoolean(GunNBTUtil.KEY_JAMMED) || currentDurability <= 0) {
            handleBrokenOrJammedGun(event, shooter, gunStack, tag);
            return;
        }

        String gunId = tag.getString(GunNBTUtil.KEY_GUN_ID);
        FireModeData modeData = extractFireModeData(tag);

        int newDurability = applyDurabilityDamage(currentDurability, gunId, shooter, tag, modeData);

        checkJamWarning(shooter, tag, newDurability, gunId, modeData);
        checkAndApplyJam(shooter, tag, newDurability, gunId, modeData);
    }

    private static FireModeData extractFireModeData(CompoundTag tag) {
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
        return new FireModeData(isAuto, isBurst, fireModeMultiplier);
    }

    private static int applyDurabilityDamage(int currentDurability, String gunId, LivingEntity shooter,
            CompoundTag tag, FireModeData modeData) {

        float gunModifier = 1.0f;
        Config.DurabilityModifier durabilityModifier = Config.getDurabilityModifier(gunId);
        if (durabilityModifier != null) {
            gunModifier = durabilityModifier.jamMultiplier();
        }

        double biomeModifier = getBiomeModifier(shooter);
        double totalModifier = gunModifier * biomeModifier;

        float damage;
        if (modeData.isAuto) {
            damage = GunDurabilityConstants.SINGLE_SHOT_MULTIPLIER;
        } else if (modeData.isBurst) {
            damage = GunDurabilityConstants.BURST_MODE_MULTIPLIER;
        } else {
            damage = GunDurabilityConstants.SINGLE_SHOT_MULTIPLIER;
        }

        int newDurability = currentDurability - (int) Math.round(damage * totalModifier);
        newDurability = Math.max(0, newDurability);

        tag.putInt(GunNBTUtil.KEY_DURABILITY, newDurability);

        return newDurability;
    }

    private static double getBiomeModifier(LivingEntity shooter) {
        try {
            var biomeKeyOpt = shooter.level().getBiome(shooter.blockPosition()).unwrapKey();
            if (biomeKeyOpt.isPresent()) {
                String biomeName = biomeKeyOpt.get().location().toString();
                return Config.getBiomeJamMultiplier(biomeName);
            }
        } catch (Exception ignored) {
        }
        return 1.0;
    }

    private static void checkJamWarning(LivingEntity shooter, CompoundTag tag, int currentDurability,
            String gunId, FireModeData modeData) {

        if (Config.isUnjammable(gunId) || Config.JAMCHANCE.get() == 0) {
            return;
        }

        if (tag.getBoolean(WARNING_PLAYED_HANDLE)) {
            return;
        }

        int maxDurability = tag.getInt("MaxDurability");
        if (maxDurability == 0) {
            maxDurability = Config.getDurability(gunId);
        }

        double durabilityMultiplier = Config.calculateJamChanceMultiplier(currentDurability, maxDurability);
        double biomeMod = getBiomeModifier(shooter);
        double totalMultiplier = durabilityMultiplier * modeData.fireModeMultiplier * biomeMod;

        double jamProbability = (Config.JAMCHANCE.get() * totalMultiplier) / maxDurability;

        if (jamProbability > 0.15) {
            tag.putBoolean(WARNING_PLAYED_HANDLE, true);
            DELAYED_SOUNDS.add(new DelayedSound(shooter, 3, true));
        } else if (jamProbability <= 0.05) {
            tag.putBoolean(WARNING_PLAYED_HANDLE, false);
        }
    }

    private static void checkAndApplyJam(LivingEntity shooter, CompoundTag tag, int currentDurability,
            String gunId, FireModeData modeData) {

        if (Config.isUnjammable(gunId) || Config.JAMCHANCE.get() == 0) {
            return;
        }

        int maxDurability = tag.getInt("MaxDurability");
        if (maxDurability == 0) {
            maxDurability = Config.getDurability(gunId);
            tag.putInt("MaxDurability", maxDurability);
        }

        double durabilityMultiplier = Config.calculateJamChanceMultiplier(currentDurability, maxDurability);
        double biomeMod = getBiomeModifier(shooter);
        double totalMultiplier = durabilityMultiplier * modeData.fireModeMultiplier * biomeMod;

        int jamRange = Math.max(1, (int) Math.round(maxDurability / (Config.JAMCHANCE.get() * totalMultiplier)));

        if (RANDOM.nextInt(jamRange + 1) == 0) {
            tag.putBoolean(GunNBTUtil.KEY_JAMMED, true);
            DELAYED_SOUNDS.add(new DelayedSound(shooter, 2, false));

            if (shooter instanceof Player player && Config.SHOW_IMMERSIVE_MESSAGES.get()) {
                player.displayClientMessage(MSG_JAMMED, true);
            }
        }

        boolean shouldBreak = shooter.level().getGameRules().getBoolean(GundurabilityModGameRules.GUNBREAK)
                || Config.GUNSBREAK.get();

        if (shouldBreak && currentDurability <= 0) {
            shooter.getMainHandItem().setCount(0);
            shooter.playSound(ITEM_BREAK_SOUND);

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
            shooter.playSound(ITEM_BREAK_SOUND);

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

    private static int getJamTimeForGunId(String gunId) {
        Config.DurabilityModifier modifier = Config.getDurabilityModifier(gunId);
        return (modifier != null) ? modifier.jamTime() : GunDurabilityConstants.DEFAULT_UNJAM_TIME_TICKS;
    }

    private static final class FireModeData {
        final boolean isAuto;
        final boolean isBurst;
        final double fireModeMultiplier;

        FireModeData(boolean isAuto, boolean isBurst, double fireModeMultiplier) {
            this.isAuto = isAuto;
            this.isBurst = isBurst;
            this.fireModeMultiplier = fireModeMultiplier;
        }
    }

    private static final class DelayedSound {
        private final LivingEntity shooter;
        private final boolean isWarning;
        private int ticks;

        DelayedSound(LivingEntity shooter, int ticks, boolean isWarning) {
            this.shooter = shooter;
            this.ticks = ticks;
            this.isWarning = isWarning;
        }

        boolean tick() {
            ticks--;
            return ticks <= 0;
        }

        void play() {
            if (!shooter.isAlive()) return;

            if (isWarning) {
                shooter.playSound(WARNING_SOUND, WARNING_VOLUME, WARNING_PITCH);
            } else {
                shooter.playSound(JAM_SOUND, JAM_VOLUME, JAM_PITCH);
            }
        }
    }
}
