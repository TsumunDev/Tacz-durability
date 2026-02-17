package com.tsumundev.gundurability.init;

import com.tsumundev.gundurability.Gundurability;
import com.tsumundev.gundurability.world.inventory.RepairGUIMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GundurabilityModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Gundurability.MODID);
	public static final RegistryObject<MenuType<RepairGUIMenu>> REPAIR_GUI = REGISTRY.register("repair_gui", () -> IForgeMenuType.create(RepairGUIMenu::new));
}
