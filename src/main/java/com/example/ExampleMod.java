package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String FIRST_JOIN_TAG = "gliding_first_join";
	private static final int SPAWN_X = 11;
	private static final int SPAWN_Y = 158;
	private static final int SPAWN_Z = -169;
	private static final int MAX_RADIUS_CHUNKS = 500;
	private static final int MAX_RADIUS_BLOCKS = MAX_RADIUS_CHUNKS * 16;
	private static final int TELEPORT_Y = 300;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing ExampleMod for Glider Teleport");

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (!player.getTags().contains(FIRST_JOIN_TAG)) {
				doFirstJoinSequence(player);
				player.addTag(FIRST_JOIN_TAG);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("glidingfirstjoin")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("target", EntityArgument.player())
					.executes(context -> {
						ServerPlayer target = EntityArgument.getPlayer(context, "target");
						doFirstJoinSequence(target);
						return 1;
					})
				)
			);
		});
	}

	private void doFirstJoinSequence(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		Random random = new Random();
		int offsetX = random.nextInt(MAX_RADIUS_BLOCKS * 2 + 1) - MAX_RADIUS_BLOCKS;
		int offsetZ = random.nextInt(MAX_RADIUS_BLOCKS * 2 + 1) - MAX_RADIUS_BLOCKS;

		int targetX = SPAWN_X + offsetX;
		int targetZ = SPAWN_Z + offsetZ;

		player.teleportTo(level, targetX + 0.5, TELEPORT_Y, targetZ + 0.5, java.util.Collections.emptySet(), player.getYRot(), player.getXRot());

		Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("gliding:wooden_glider"));
		if (itemOpt.isPresent()) {
			ItemStack gliderStack = new ItemStack(itemOpt.get());
			if (!player.getInventory().add(gliderStack)) {
				player.drop(gliderStack, false);
			}
		} else {
			LOGGER.warn("Item gliding:wooden_glider not found in registry.");
		}
	}
}
