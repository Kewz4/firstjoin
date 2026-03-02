package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final String FIRST_JOIN_TAG = "gliding_first_join";
	private static final int SPAWN_X = 11;
	private static final int SPAWN_Y = 158;
	private static final int SPAWN_Z = -169;
	private static final int MAX_RADIUS_CHUNKS = 500;
	private static final int MAX_RADIUS_BLOCKS = MAX_RADIUS_CHUNKS * 16;
	private static final int TELEPORT_Y = 500;

	// 20 ticks per second
	private static final int HUD_DURATION_TICKS = 25 * 20;
	private static final int FALL_IMMUNITY_DURATION_TICKS = 30 * 20;

	// Maps to store remaining ticks for players
	public static final Map<UUID, Integer> hudTimers = new HashMap<>();
	public static final Map<UUID, Integer> fallImmunityTimers = new HashMap<>();

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

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				UUID uuid = player.getUUID();

				// Handle Fall Immunity
				if (fallImmunityTimers.containsKey(uuid)) {
					int ticksLeft = fallImmunityTimers.get(uuid);
					if (ticksLeft > 0) {
						// Reset fall distance constantly so they don't take damage when landing
						player.resetFallDistance();
						fallImmunityTimers.put(uuid, ticksLeft - 1);
					} else {
						fallImmunityTimers.remove(uuid);
					}
				}

				// Handle HUD message
				if (hudTimers.containsKey(uuid)) {
					int ticksLeft = hudTimers.get(uuid);
					if (ticksLeft > 0) {
						MutableComponent message = Component.literal("Soar").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
							.append(Component.literal(" to your perfect landing spot. ").withStyle(ChatFormatting.WHITE))
							.append(Component.literal("Welcome to Kewz's Cobbleverse!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

						player.displayClientMessage(message, true); // true = action bar
						hudTimers.put(uuid, ticksLeft - 1);
					} else {
						hudTimers.remove(uuid);
					}
				}
			}
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

		Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("hangglider:reinforced_hang_glider"));
		if (itemOpt.isPresent()) {
			ItemStack gliderStack = new ItemStack(itemOpt.get());

			ItemStack oldMainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
			if (!oldMainHand.isEmpty()) {
				if (!player.getInventory().add(oldMainHand)) {
					player.drop(oldMainHand, false);
				}
			}

			// Equip hang glider
			player.setItemInHand(InteractionHand.MAIN_HAND, gliderStack);

			// Simulate "right click" use on the server by going through the gameMode handler
			player.gameMode.useItem(player, level, gliderStack, InteractionHand.MAIN_HAND);

		} else {
			LOGGER.warn("Item hangglider:reinforced_hang_glider not found in registry.");
		}

		// Start timers for this player
		UUID uuid = player.getUUID();
		hudTimers.put(uuid, HUD_DURATION_TICKS);
		fallImmunityTimers.put(uuid, FALL_IMMUNITY_DURATION_TICKS);
	}
}
