package com.corrinedev.gundurability.client.screens;

import com.corrinedev.gundurability.config.Config;
import com.corrinedev.gundurability.config.ConfigClient;
import com.corrinedev.gundurability.util.GunNBTUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class IconOverlayOverlay {

	private static final int ICON_SIZE = 16;

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		if (!ConfigClient.SHOWGUI.get() || !ConfigClient.SHOW_ICON_OVERLAY.get()) {
			return;
		}

		Player entity = Minecraft.getInstance().player;
		if (entity == null || !(entity.getMainHandItem().getItem() instanceof ModernKineticGunItem)) {
			return;
		}

		int w = event.getWindow().getGuiScaledWidth();
		int h = event.getWindow().getGuiScaledHeight();

		var gunStack = entity.getMainHandItem();
		String gunId = GunNBTUtil.getGunId(gunStack);
		int maxDurability = (gunId != null && !gunId.isEmpty()) ? Config.getDurability(gunId) : 0;
		if (maxDurability <= 0) maxDurability = Config.MAXDURABILITY.get();

		float percent = (float) GunNBTUtil.getDurability(gunStack) / maxDurability * 100f;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		ResourceLocation iconTexture;
		if (GunNBTUtil.isJammed(gunStack)) {
			iconTexture = new ResourceLocation("gundurability:textures/screens/jam.png");
		} else {
			double highThreshold = ConfigClient.THRESHOLD_HIGH.get();
			double mediumThreshold = ConfigClient.THRESHOLD_MEDIUM.get();
			double lowThreshold = ConfigClient.THRESHOLD_LOW.get();

			if (percent > mediumThreshold && percent <= highThreshold) {
				iconTexture = new ResourceLocation("gundurability:textures/screens/yellow.png");
			} else if (percent > lowThreshold && percent <= mediumThreshold) {
				iconTexture = new ResourceLocation("gundurability:textures/screens/orange.png");
			} else if (percent <= lowThreshold) {
				iconTexture = new ResourceLocation("gundurability:textures/screens/reds.png");
			} else {
				iconTexture = new ResourceLocation("gundurability:textures/screens/green.png");
			}
		}

		int offsetX = ConfigClient.OVERLAY_OFFSET_X.get() + 18;
		int offsetY = ConfigClient.OVERLAY_OFFSET_Y.get() + 9;

		event.getGuiGraphics().blit(iconTexture,
				w - offsetX, h - offsetY, 0, 0,
				ICON_SIZE, ICON_SIZE,
				ICON_SIZE, ICON_SIZE);

		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
