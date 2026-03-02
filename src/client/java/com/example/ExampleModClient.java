package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.DeltaTracker;

import java.io.File;
import java.io.IOException;

public class ExampleModClient implements ClientModInitializer {

	private static final String CONFIG_FILE_NAME = "first_join_soar_done.txt";
	private boolean hasRunSequence = false;
	private boolean sequenceActive = false;
	private long sequenceStartTime = 0;
	private static final long SEQUENCE_DURATION_MS = 15000;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);
		if (configFile.exists()) {
			hasRunSequence = true;
		}

		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof LocalPlayer && entity == Minecraft.getInstance().player) {
				if (!hasRunSequence && !sequenceActive) {
					sequenceActive = true;
					sequenceStartTime = System.currentTimeMillis();
					hasRunSequence = true;
					try {
						configFile.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (sequenceActive) {
				long currentTime = System.currentTimeMillis();
				if (currentTime - sequenceStartTime < SEQUENCE_DURATION_MS) {
					// Hold space
					client.options.keyJump.setDown(true);
				} else {
					sequenceActive = false;
					// Release space
					client.options.keyJump.setDown(false);
				}
			}
		});

		HudRenderCallback.EVENT.register((GuiGraphics context, DeltaTracker tickCounter) -> {
			if (sequenceActive) {
				Minecraft client = Minecraft.getInstance();
				if (client.player != null && client.font != null) {
					String message = "SOAR AROUND HOLD SPACE TO GLIDE AND DROP IN YOUR PERFECT SPOT";
					int width = client.getWindow().getGuiScaledWidth();
					int height = client.getWindow().getGuiScaledHeight();
					int textWidth = client.font.width(message);

					// Draw centered at top of screen (or maybe slightly below top)
					context.drawString(client.font, message, (width - textWidth) / 2, height / 4, 0xFFD700, true); // Gold color
				}
			}
		});
	}
}
