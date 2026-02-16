package com.corrinedev.gundurability.init;

import com.corrinedev.gundurability.Gundurability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GundurabilityModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Gundurability.MODID);
	public static final RegistryObject<SoundEvent> JAMSFX = REGISTRY.register("jamsfx", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("gundurability", "jamsfx")));
	public static final RegistryObject<SoundEvent> JAM_WARNING = REGISTRY.register("jam_warning", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("gundurability", "jam_warning")));
}
