package de.hysky.skyblocker.skyblock.variouscooldowns;

import de.hysky.skyblocker.annotations.RegisterWidget;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.skyblock.tabhud.config.WidgetsConfigurationScreen;
import de.hysky.skyblocker.skyblock.tabhud.util.Ico;
import de.hysky.skyblocker.skyblock.tabhud.widget.ComponentBasedWidget;
import de.hysky.skyblocker.utils.Location;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;

@RegisterWidget
public class VariousCooldownsHudWidget extends ComponentBasedWidget {
	private static VariousCooldownsHudWidget instance;

	public static VariousCooldownsHudWidget getInstance() {
		return instance;
	}

	public VariousCooldownsHudWidget() {
		super(Text.literal("Various Cooldowns").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), Formatting.DARK_PURPLE.getColorValue(), "hud_variouscooldowns");
		instance = this;
	}

	@Override
	public Set<Location> availableLocations() {
		return Set.of(Location.values());
	}

	@Override
	public void setEnabledIn(Location location, boolean enabled) {
		if (enabled) {
			SkyblockerConfigManager.get().general.variouscooldownsHudEnabledLocations.add(location);
		} else {
			SkyblockerConfigManager.get().general.variouscooldownsHudEnabledLocations.remove(location);
		}
	}

	@Override
	public boolean isEnabledIn(Location location) {
		return SkyblockerConfigManager.get().general.variouscooldownsHudEnabledLocations.contains(location);
	}

	@Override
	public boolean shouldRender(Location location) {
		boolean hasContent = SkyblockerConfigManager.get().general.variouscooldowns.riftUbixCooldown
				|| SkyblockerConfigManager.get().general.variouscooldowns.tunefrequencycooldown;
		return SkyblockerConfigManager.get().general.variouscooldownsHudEnabledLocations.contains(location)
				&& SkyblockerConfigManager.get().general.variouscooldowns.variousCooldownsHUD
				&& hasContent
				&& super.shouldRender(location);
	}

	@Override
	public void updateContent() {
		if (MinecraftClient.getInstance().currentScreen instanceof WidgetsConfigurationScreen) {
				addSimpleIcoText(Ico.CLOCK, "Ubik's Cube: ", Formatting.YELLOW, "1 h 30 min");
				addSimpleIcoText(Ico.BEACON, "Tune Frequency: ", Formatting.YELLOW, "10 min");
			return;
		}

		if (SkyblockerConfigManager.get().general.variouscooldowns.riftUbixCooldown) {
			addSimpleIcoText(Ico.CLOCK, "Ubik's Cube: ", Formatting.YELLOW, RiftUbixCooldown.getRiftUbixCooldown());
		}
		if (SkyblockerConfigManager.get().general.variouscooldowns.tunefrequencycooldown) {
			addSimpleIcoText(Ico.BEACON, "Tune Frequency: ", Formatting.YELLOW, TuneFrequencyCooldown.getTuneFrequencyCooldown());
		}

	}

	@Override
	public Text getDisplayName() {
		return Text.literal("Various Cooldowns Hud");
	}

}
