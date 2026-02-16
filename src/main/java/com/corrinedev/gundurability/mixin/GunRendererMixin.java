package com.corrinedev.gundurability.mixin;

import com.corrinedev.gundurability.client.RedTintManager;
import com.corrinedev.gundurability.util.GunNBTUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockGunModel.class, remap = false)
public class GunRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockAnimatedModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V", shift = At.Shift.BEFORE))
    public void renderByItemFirst(PoseStack matrixStack, ItemStack gunItem, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, CallbackInfo ci) {
        if (RedTintManager.isShowRedTint()) {
            RenderSystem.enableBlend();
            int durability = GunNBTUtil.getDurability(gunItem);
            int maxDurability = GunNBTUtil.getMaxDurability(gunItem);
            float healthFactor = Mth.clamp((float) durability / maxDurability + 0.5f, 0f, 1f);
            RenderSystem.setShaderColor(1f, healthFactor, healthFactor, 1f);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockAnimatedModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V", shift = At.Shift.AFTER))
    public void renderByItemLast(PoseStack matrixStack, ItemStack gunItem, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, CallbackInfo ci) {
        if (RedTintManager.isShowRedTint()) {
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
