package com.tsumundev.gundurability.client.screens;

import com.tsumundev.gundurability.config.Config;
import com.tsumundev.gundurability.config.ConfigClient;
import com.tsumundev.gundurability.config.ConfigClient.OverlayPosition;
import com.tsumundev.gundurability.util.GunNBTUtil;
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
public class DurabilityOverlayOverlay {

	private static final int ICON_SIZE = 32;

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		if (!ConfigClient.SHOWGUI.get() || !ConfigClient.SHOW_DURABILITY_TEXT.get()) {
			return;
		}

		int screenHeight = event.getWindow().getGuiScaledHeight();
		int screenWidth = event.getWindow().getGuiScaledWidth();
		Player entity = Minecraft.getInstance().player;

		if (entity == null || !(entity.getMainHandItem().getItem() instanceof ModernKineticGunItem)) {
			return;
		}

		var gunStack = entity.getMainHandItem();
		String gunId = GunNBTUtil.getGunId(gunStack);
		int maxDurability = (gunId != null && !gunId.isEmpty()) ? Config.getDurability(gunId) : 0;
		if (maxDurability <= 0) maxDurability = Config.MAXDURABILITY.get();

		int currentDurability = GunNBTUtil.getDurability(gunStack);
		double percent = (double) currentDurability / maxDurability * 100.0;

		int offsetX = ConfigClient.OVERLAY_OFFSET_X.get();
		int offsetY = ConfigClient.OVERLAY_OFFSET_Y.get();
		OverlayPosition position = ConfigClient.getOverlayPosition();

		int x, y;
		switch (position) {
			case TOP_LEFT:
				x = offsetX;
				y = offsetY;
				break;
			case TOP_RIGHT:
				x = screenWidth - offsetX - ICON_SIZE;
				y = offsetY;
				break;
			case BOTTOM_LEFT:
				x = offsetX;
				y = screenHeight - offsetY - ICON_SIZE;
				break;
			case BOTTOM_RIGHT:
				x = screenWidth - offsetX - ICON_SIZE;
				y = screenHeight - offsetY - ICON_SIZE;
				break;
			default:
				x = offsetX;
				y = screenHeight - offsetY - ICON_SIZE;
		}

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		ResourceLocation iconTexture;
		int textColor;

		if (GunNBTUtil.isJammed(gunStack)) {
			iconTexture = new ResourceLocation("gundurability:textures/screens/jam.png");
			textColor = 0xFF5555;
		} else if (currentDurability <= 0) {
			iconTexture = new ResourceLocation("gundurability:textures/screens/jam.png");
			textColor = 0xFF5555;
		} else {
			iconTexture = new ResourceLocation("gundurability:textures/screens/jam.png");
			textColor = ConfigClient.getColorForDurability(percent);
		}

		event.getGuiGraphics().blit(iconTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		String displayText;
		if (GunNBTUtil.isJammed(gunStack)) {
			displayText = ConfigClient.TEXT_JAMMED.get();
		} else if (currentDurability <= 0) {
			displayText = ConfigClient.TEXT_BROKEN.get();
		} else {
			displayText = ConfigClient.formatDurabilityText(percent);
		}

		int fontWidth = Minecraft.getInstance().font.width(displayText);
		int textX = x + (ICON_SIZE - fontWidth) / 2;
		int textY = y + ICON_SIZE + 2;

		boolean shadow = ConfigClient.OVERLAY_SHADOW.get();
		event.getGuiGraphics().drawString(Minecraft.getInstance().font, displayText, textX, textY, textColor, shadow);
	}
}
