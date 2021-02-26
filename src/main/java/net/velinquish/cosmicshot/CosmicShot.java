package net.velinquish.cosmicshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponPrepareShootEvent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;

import lombok.Getter;
import net.velinquish.cosmicshot.commands.ReloadCommand;
import net.velinquish.utils.Common;
import net.velinquish.utils.VelinquishPlugin;
import net.velinquish.utils.lang.LangManager;

public class CosmicShot extends JavaPlugin implements Listener, VelinquishPlugin {

	@Getter
	private static CosmicShot instance;
	@Getter
	private LangManager langManager;

	@Getter
	private String prefix;
	@Getter
	private String permission;

	private static boolean debug;

	@Getter
	private YamlConfiguration config;
	private File configFile;

	@Getter
	private YamlConfiguration lang;
	private File langFile;

	private RegionManager regionManager;

	private List<Gun> guns;
	private Gun defaults;

	@Override
	public void onEnable() {
		instance = this;
		Common.setInstance(this);

		langManager = new LangManager();
		regionManager = WorldGuardPlugin.inst().getRegionManager(Bukkit.getWorld("world"));

		try {
			loadFiles();
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(this, this);
		Common.registerCommand(new ReloadCommand(this));
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	public void loadFiles() throws IOException, InvalidConfigurationException {
		configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}
		config = new YamlConfiguration();
		config.load(configFile);

		prefix = getConfig().getString("plugin-prefix");
		debug = getConfig().getBoolean("debug");
		permission = getConfig().getString("permission");
		loadGuns();

		langFile = new File(getDataFolder(), "lang.yml");
		if (!langFile.exists()) {
			langFile.getParentFile().mkdirs();
			saveResource("lang.yml", false);
		}
		lang = new YamlConfiguration();
		lang.load(langFile);

		langManager.clear();
		langManager.setPrefix(prefix);
		langManager.loadLang(lang);
	}

	private void loadGuns() {
		guns = new ArrayList<>();
		for (String gun : config.getConfigurationSection("Guns").getKeys(false))
			guns.add(new Gun(gun, defaultStringList("Guns." + gun, ".Banned_Regions"), defaultStringList("Guns." + gun, ".Banned_Effects"),
					config.getBoolean("Guns." + gun + ".Region_Whitelist")));

		//Loads defaults
		defaults = new Gun(null, config.getStringList("Default.Banned_Regions"), config.getStringList("Default.Banned_Effects"), false);
	}

	private List<String> defaultStringList(String prefix, String attribute) {
		List<String> list = config.getStringList(prefix + attribute);
		if (list == null)
			return getConfig().getStringList("Default" + attribute);
		return list;
	}

	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
	public void onPreShoot(final WeaponPrepareShootEvent e) {
		Player player = e.getPlayer();
		Gun gun = defaults;
		for (Gun each : guns)
			if (e.getWeaponTitle().indexOf(each.getName()) != -1) {
				gun = each;
				break;
			}

		List<String> bannedRegions = gun.getBannedRegions();
		String bannedRegion = isWithinAny(player.getLocation(), bannedRegions);
		boolean isWithinBannedRegions = bannedRegion != null;

		ItemStack inHand = player.getInventory().getItemInMainHand();
		String name = null;
		if (inHand.hasItemMeta())
			name = removeAmmo(inHand.getItemMeta().getDisplayName());
		else
			name = e.getWeaponTitle();

		if (gun.isRegionWhitelist()) {
			if (!isWithinBannedRegions) {
				e.setCancelled(true);

				langManager.getNode("whitelisted-region").replace(Common.map("%region%", bannedRegions.get(0), "%weapon%", name)).execute(player);
			}
		} else if (isWithinBannedRegions) {
			e.setCancelled(true);

			langManager.getNode("banned-region").replace(Common.map("%region%", bannedRegion, "%weapon%", name)).execute(player);
		}

		for (PotionEffectType effect : gun.getBannedEffects())
			if (player.getPotionEffect(effect) != null) {
				e.setCancelled(true);
				langManager.getNode("banned-effect").replace(Common.map("%effect%", effect.getName().toLowerCase(), "%weapon%",
						(player.getInventory().getItemInMainHand().hasItemMeta() ?
								player.getInventory().getItemInMainHand().getItemMeta().getDisplayName() : e.getWeaponTitle()))).execute(player);
				return;
			}
	}

	@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
	public void onCrackShotDamage(final WeaponDamageEntityEvent e) {
		Gun gun = defaults;
		for (Gun each : guns)
			if (e.getWeaponTitle().indexOf(each.getName()) != -1) {
				gun = each;
				break;
			}

		boolean isWithinBannedRegions = isWithinAny(e.getVictim().getLocation(), gun.getBannedRegions()) != null;
		if (gun.isRegionWhitelist() ? !isWithinBannedRegions : isWithinBannedRegions)
			e.setCancelled(true);
	}

	/**
	 * Removes the ammo part of the weapon display name
	 * @param name Weapon display name
	 * @return The weapon display name with the ammo removed
	 */
	private String removeAmmo(String name) {
		int index = name.indexOf("«");

		if (index == -1)
			return name;

		return name.substring(0, index - 1);
	}

	/**
	 * @param loc Location
	 * @param regions The regions to check if the location is in
	 * @return The name of the banned region that the player is in or null if the player is not in a banned region
	 */
	private String isWithinAny(Location loc, List<String> regions) {
		// Names of the regions that the player is in
		List<String> names = regionManager.getApplicableRegions(loc).getRegions().stream().map(rg -> rg.getId()).collect(Collectors.toList());
		names.retainAll(regions);
		if (names.size() > 0)
			return names.get(0); // Returns the name of the banned region the player is in
		return null;
	}

	public static void debug(String message) {
		if (debug == true)
			Common.log(message);
	}
}
