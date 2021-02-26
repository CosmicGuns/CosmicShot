package net.velinquish.cosmicshot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.potion.PotionEffectType;

import lombok.Getter;

public class Gun {

	@Getter
	private String name;
	@Getter
	private List<String> bannedRegions;
	@Getter
	private List<PotionEffectType> bannedEffects;
	/**
	 * Whether the list of banned regions is a whitelist instead of a blacklist -
	 * in other words, whether those are the only regions that the gun is allowed to be used in
	 */
	@Getter
	private boolean regionWhitelist;

	public Gun(String name, List<String> bannedRegions, List<String> bannedEffects, boolean regionWhitelist) {
		this.name = name;
		if (bannedRegions == null)
			this.bannedRegions = new ArrayList<>();
		else
			this.bannedRegions = bannedRegions;
		List<PotionEffectType> effects = new ArrayList<>();
		for (String effect : bannedEffects)
			effects.add(PotionEffectType.getByName(effect.toUpperCase()));
		this.bannedEffects = effects;
		this.regionWhitelist = regionWhitelist;
	}
}
