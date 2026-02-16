package com.corrinedev.gundurability;

import com.corrinedev.gundurability.config.Config;
import com.corrinedev.gundurability.config.ConfigClient;
import com.corrinedev.gundurability.events.TaczEvents;
import com.corrinedev.gundurability.init.*;
import com.corrinedev.gundurability.util.Work;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(Gundurability.MODID)
public class Gundurability {

    public static final String MODID = "gundurability";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final List<Consumer<ModConfig>> CONFIG_RELOAD_LISTENERS = new ArrayList<>();

    public Gundurability() {
        createConfigDirectory();

        registerConfigs();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(GundurabilityModItems::register);
        bus.addListener(this::commonSetup);

        GundurabilityModSounds.REGISTRY.register(bus);
        GundurabilityModBlocks.REGISTRY.register(bus);
        GundurabilityModItems.REPAIRTABLEREGISTER.register(bus);
        GundurabilityModTabs.REGISTRY.register(bus);
        GundurabilityModMenus.REGISTRY.register(bus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(TaczEvents.class);

        LOGGER.info("Gun Durability Mod loaded!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Config.refreshCache();
    }

    private void registerConfigs() {
        ModLoadingContext context = ModLoadingContext.get();

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        context.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);

        LOGGER.info("Configurations registered successfully!");
    }

    public static void addConfigReloadListener(Consumer<ModConfig> listener) {
        CONFIG_RELOAD_LISTENERS.add(listener);
    }

    @SubscribeEvent
    public void onConfigReload(final net.minecraftforge.fml.event.config.ModConfigEvent event) {
        ModConfig config = event.getConfig();
        LOGGER.info("Configuration loaded/reloaded: {}", config.getFileName());

        if (config.getModId().equals(MODID)) {
            String fileName = config.getFileName();
            if (fileName.contains("gundurability-common")) {
                Config.refreshCache();
            }

            for (Consumer<ModConfig> listener : CONFIG_RELOAD_LISTENERS) {
                try {
                    listener.accept(config);
                } catch (Exception e) {
                    LOGGER.error("Error in config reload listener", e);
                }
            }
        }
    }

    private void createConfigDirectory() {
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve(MODID);
            java.nio.file.Files.createDirectories(configPath);
            LOGGER.info("Config directory ensured at: {}", configPath.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to create config directory", e);
        }
    }

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }

    private static final Collection<Work> workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(Work work) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(work);
    }

    public static <MSG> void sendToPlayer(MSG msg, ServerPlayer player) {
        PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<Work> actions = new ArrayList<>();
            workQueue.forEach(work -> {
                work.tick -= 1;
                work.tick();
                if (work.tick == 0)
                    actions.add(work);
            });
            actions.forEach(Runnable::run);
            workQueue.removeAll(actions);
        }
    }
}
