package com.tsumundev.gundurability.init;

import com.tsumundev.gundurability.Gundurability;
import com.tsumundev.gundurability.block.RepairTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GundurabilityModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, Gundurability.MODID);
	public static final RegistryObject<Block> REPAIR_TABLE = REGISTRY.register("repair_table", RepairTableBlock::new);
}
