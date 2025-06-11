package de.hysky.skyblocker.skyblock.variouscooldowns;

import de.hysky.skyblocker.annotations.Init;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RiftUbixCooldown {
	private static int ubixUsedAt = 0;
	private static boolean ubixUsed = false; // Track if the Ubix was used

	private static final Pattern SPLIT_COOLDOWN_PATTERN = Pattern.compile(
            "SPLIT! You need to wait (?:(\\d+)h )?(?:(\\d+)m )?(\\d+)s before you can play again\\."
	);
	private static final Pattern PLAYER_MOTES_PATTERN = Pattern.compile("You earned \\d{1,3}(,\\d{3})* Motes in this match!");
	private static final Pattern OPPONENT_MOTES_PATTERN = Pattern.compile("Your opponent earned \\d{1,3}(,\\d{3})* Motes in this match!");

	@Init
	public static void init() {
		// Register the UseItemCallback to listen for item usage
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClient) return ActionResult.PASS;
			if (player.getStackInHand(hand).getItem() == Items.PLAYER_HEAD) {
				if (player.getStackInHand(hand).getNeuName().equals("UBIKS_CUBE")) ubixUsed = true;
			}
			return ActionResult.PASS;
		});

		// Listen for chat messages
		ClientReceiveMessageEvents.GAME.register((message, bool) -> {
			String msg = message.getString();

			// Match result messages
			if (PLAYER_MOTES_PATTERN.matcher(msg).find() ||
					OPPONENT_MOTES_PATTERN.matcher(msg).find()) {
//					client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Match result found."));
				if (ubixUsed) {
					ubixUsed = false;
//					client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Ubix was used, setting cooldown timer."));
					ubixUsedAt = (int) (getCurrentRealTimeMillis() / 1000);
				}
			}

			// SPLIT! cooldown message
			Matcher matcher = SPLIT_COOLDOWN_PATTERN.matcher(msg);
			if (matcher.find()) {
				if (ubixUsed) {
					ubixUsed = false;
					int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
					int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
					int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
					int totalSeconds = hours * 3600 + minutes * 60 + seconds;
					ubixUsedAt = (int) (getCurrentRealTimeMillis() / 1000) - (2 * 60 * 60 - totalSeconds);
				}
			}
		});

		// Register the HudLayerRegistrationCallback to render the cooldown
//		HudLayerRegistrationCallback.EVENT.register(d -> d.attachLayerAfter(IdentifiedLayer.TITLE_AND_SUBTITLE,RIFT_UBIX_COOLDOWN, RiftUbixCooldown::render));
	}

	public static long getCurrentRealTimeMillis() {
		return System.currentTimeMillis();
	}

	public static String getRiftUbixCooldown() {
		int currentTime = (int) (getCurrentRealTimeMillis() / 1000);
		int cooldownSeconds = 2 * 60 * 60; // 2 hours = 7200 seconds
		int elapsed = currentTime - ubixUsedAt;
		int remainingTime = cooldownSeconds - elapsed;

		String display;
		if (remainingTime > 0) {
			if (remainingTime >= 3600) {
				int hours = remainingTime / 3600;
				int minutes = (remainingTime % 3600) / 60;
				display = String.format("%d h %d min", hours, minutes);
			} else {
				int minutes = remainingTime / 60;
				int seconds = remainingTime % 60;
				display = String.format("%d min %d sec", minutes, seconds);
			}
		} else {
			display = "Ready";
		}
		return display;
	}
}
