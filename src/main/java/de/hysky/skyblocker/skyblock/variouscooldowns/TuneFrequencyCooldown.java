package de.hysky.skyblocker.skyblock.variouscooldowns;

import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.utils.Utils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.client.network.PlayerListEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TuneFrequencyCooldown {

	private static final Pattern COOLDOWN_PATTERN = Pattern.compile("Cooldown: (?:(\\d+)m\\s*)?(?:(\\d+)s)?");
	private static final Pattern MOONGLADE_PATTERN = Pattern.compile("Moonglade Beacon: \\d+ Stack");
	private static final Pattern FREQ_ADJUSTED_PATTERN = Pattern.compile("You adjusted the frequency of the Beacon!");
	private static final Pattern FREQ_ALREADY_ADJUSTED_PATTERN = Pattern.compile("You are currently on cooldown for this!");
	private static long cooldownEndEpoch = 0; // Epoch time in milliseconds when the cooldown ends

	@Init
	public static void init() {
		ClientReceiveMessageEvents.GAME.register((message, bool) -> {
			String msg = message.getString();

			// Match result messages
			if (FREQ_ALREADY_ADJUSTED_PATTERN.matcher(msg).find()) {
				try {
					updateFromTablist();
				} catch (Exception e) {
					MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Error updating tablist: " + e.getMessage()));
				}
				return;
			}

			if (FREQ_ADJUSTED_PATTERN.matcher(msg).find()) {
				ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
					private int ticks = 0;
					private boolean ran = false;
					@Override
					public void onEndTick(MinecraftClient client) {
						if (ran) return;
						ticks++;
						if (ticks >= 80) {
							ran = true;
							try {
								updateFromTablist();
							} catch (Exception e) {
								MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Error updating tablist: " + e.getMessage()));
							}
						}
					}
				});
			}
		});
	}

	public static void updateFromTablist() {
		if (!Utils.isOnSkyblock() || !Utils.isOnHypixel()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getNetworkHandler() == null) return;
		boolean moondladefound = false;
		boolean cooldownfound = false;

		int cooldownSeconds = -1;
		for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
			Text displayName = entry.getDisplayName();
			if (displayName != null) {
				String displayNameStr = displayName.getString();
				if (MOONGLADE_PATTERN.matcher(displayNameStr).find()) {
//					client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Moonglade Beacon found in tablist."));
					moondladefound = true;
				}
				Matcher matcher = COOLDOWN_PATTERN.matcher(displayNameStr);
				if (matcher.find()) {
					int minutes = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
					int seconds = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
					int parsedCooldown = minutes * 60 + seconds;
//					client.inGameHud.getChatHud().addMessage(
//							Text.literal("[DEBUG] Cooldown found in tablist: " + parsedCooldown + " seconds left (" + minutes + "m " + seconds + "s).")
//					);
					cooldownSeconds = parsedCooldown;
					cooldownfound = true;
				}
			}
		}
//		client.inGameHud.getChatHud().addMessage(Text.literal("[DEBUG] Moondlade found: " + moondladefound + ", Cooldown found: " + cooldownfound + ", Cooldown seconds: " + cooldownSeconds));
		if (moondladefound && cooldownfound && cooldownSeconds > 0) {
			cooldownEndEpoch = getCurrentRealTimeMillis() + (cooldownSeconds * 1_000L);
		}
	}

	public static long getCurrentRealTimeMillis() {
		return System.currentTimeMillis();
	}

	public static String getTuneFrequencyCooldown() {
		if (!SkyblockerConfigManager.get().general.variouscooldowns.tunefrequencycooldown) return null;
		if (!Utils.isOnSkyblock() || !Utils.isOnHypixel()) return null;

		long remaining = (cooldownEndEpoch - System.currentTimeMillis()) / 1000;
		if (remaining > 0) {
			long minutes = remaining / 60;
			long seconds = remaining % 60;
			if (minutes > 0) {
				return minutes + "m " + seconds + "s";
			} else {
				return seconds + "s";
			}
		}
		return "Ready";
	}
}
