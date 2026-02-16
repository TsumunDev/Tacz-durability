package com.corrinedev.gundurability.mixin;

import com.corrinedev.gundurability.util.GunNBTUtil;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = GunItemDataAccessor.class, remap = false)
public interface DefaultGunInstanceMixin {
    @Overwrite
    default void setGunId(ItemStack gun, ResourceLocation gunId) {
        String gunIdStr = gunId != null ? gunId.toString() : null;
        GunNBTUtil.setGunId(gun, gunIdStr);
    }
}
