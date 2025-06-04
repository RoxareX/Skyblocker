package de.hysky.skyblocker.skyblock.rift;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.ClientPlayerEntity;
import de.hysky.skyblocker.utils.Utils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RiftUbixCooldown {
	private static int ubixUsedAt = 0;
	private static final Identifier RIFT_UBIX_COOLDOWN = Identifier.of(SkyblockerMod.NAMESPACE, "rift_ubix_cooldown");
	private static String message = "";
	private static boolean playedReadySound = false;

	private static boolean ubixUsed = false; // Track if the Ubix was used

	private static final Pattern SPLIT_COOLDOWN_PATTERN = Pattern.compile(
			"SPLIT! You need to wait (\\d+)h (\\d+)m (\\d+)s before you can play again\\."
	);
	private static final Pattern PLAYER_MOTES_PATTERN = Pattern.compile("You earned \\d{1,3}(,\\d{3})* Motes in this match!");
	private static final Pattern OPPONENT_MOTES_PATTERN = Pattern.compile("Your opponent earned \\d{1,3}(,\\d{3})* Motes in this match!");

	@Init
	public static void init() {
		MinecraftClient client = MinecraftClient.getInstance();

		// Register the UseItemCallback to listen for item usage
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClient) return ActionResult.PASS;
			if (player.getStackInHand(hand).getItem() == Items.PLAYER_HEAD) {
				if (player.getStackInHand(hand).getNeuName().equals("UBIKS_CUBE")) ubixUsed = true;
			}
			return ActionResult.PASS;
		});

		// Listen for chat messages
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String msg = message.getString();

			// Match result messages
			if (PLAYER_MOTES_PATTERN.matcher(msg).find() ||
					OPPONENT_MOTES_PATTERN.matcher(msg).find()) {
				if (ubixUsed) {
					ubixUsed = false;
					client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Ubix was used, setting cooldown timer."));
					ubixUsedAt = (int) (getCurrentRealTimeMillis() / 1000);
				}
			}

			// SPLIT! cooldown message
			Matcher matcher = SPLIT_COOLDOWN_PATTERN.matcher(msg);
			if (matcher.find()) {
				if (ubixUsed) {
					ubixUsed = false;

					int hours = Integer.parseInt(matcher.group(1));
					int minutes = Integer.parseInt(matcher.group(2));
					int seconds = Integer.parseInt(matcher.group(3));
					int totalSeconds = hours * 3600 + minutes * 60 + seconds;
					ubixUsedAt = (int) (getCurrentRealTimeMillis() / 1000) - (2 * 60 * 60 - totalSeconds); // 2h = 7200s
					client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] SPLIT! cooldown detected, set remaining: " + totalSeconds + "s"));
				}
			}
		});

		// Register the HudLayerRegistrationCallback to render the cooldown
		HudLayerRegistrationCallback.EVENT.register(d -> d.attachLayerAfter(IdentifiedLayer.TITLE_AND_SUBTITLE,RIFT_UBIX_COOLDOWN, RiftUbixCooldown::render));
	}

	public static void render(DrawContext context, RenderTickCounter tickDelta) {
		if (!SkyblockerConfigManager.get().general.itemcooldowns.riftUbixCooldown) return;

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) return;
		if (!(Utils.isOnSkyblock() && Utils.isOnHypixel())) return;

		int currentTime = (int) (getCurrentRealTimeMillis() / 1000);
		int cooldownSeconds = 2 * 60 * 60; // 2 hours = 7200 seconds
		int elapsed = currentTime - ubixUsedAt;
		int remainingTime = cooldownSeconds - elapsed;

		if (remainingTime > 0) {
			playedReadySound = false; // Reset when cooldown is active

			if (remainingTime >= 3600) {
				double hours = remainingTime / 3600.0;
				message = String.format("Rift Ubix: %.1fh", hours);
			} else {
				int minutes = remainingTime / 60;
				int seconds = remainingTime % 60;
				message = String.format("Rift Ubix: %dmin %d sec", minutes, seconds);
			}
		} else {
			message = "Rift Ubix: Ready";
			if (!playedReadySound && client.player != null) {
				// Play the sound twice so it more distinguishable
				client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 80f, 0.1f);
				client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 80f, 0.1f);
				playedReadySound = true;
			}
		}

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		int scale = 1;
		int margin = (int) (screenWidth * 0.1); // Optional margin from the right edge
		int x = (int) ((screenWidth - margin) / (float) scale);
		int y = (int) ((screenHeight / 4.0F) / scale);

		// Scale the text by 1x
		context.getMatrices().push();
		context.getMatrices().scale(1.0F, 1.0F, 1.0F);
		context.drawCenteredTextWithShadow(client.textRenderer, message, (int) (x / 1.0F), (int) (y / 1.0F), 0xFFF000);
		context.getMatrices().pop();
	}

	public static long getCurrentRealTimeMillis() {
		return System.currentTimeMillis();
	}

}
