package net.velinquish.cosmicshot.commands;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import net.velinquish.cosmicshot.CosmicShot;
import net.velinquish.utils.Common;

public class ReloadCommand extends Command {

	private CosmicShot plugin;

	public ReloadCommand(CosmicShot plugin) {
		super(plugin.getConfig().getString("main-command"));
		setAliases(plugin.getConfig().getStringList("plugin-aliases"));
		setDescription("Main command for CosmicShot");
		this.plugin = plugin;
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		if (args.length < 1 || !"reload".equalsIgnoreCase(args[0]))
			plugin.getLangManager().getNode("command-reload-usage").execute(sender);
		else {
			if (!sender.hasPermission(plugin.getPermission().replaceAll("%action%", "reload"))) {
				plugin.getLangManager().getNode("no-permission").execute(sender);
				return false;
			}

			try {
				plugin.loadFiles();
			} catch (IOException | InvalidConfigurationException e) {
				Common.tell(sender, "&cAn error occurred while reloading the configuration files. The errors were sent to console!");
				e.printStackTrace();
				return false;
			}
			plugin.getLangManager().getNode("plugin-reloaded").execute(sender);
		}
		return false;
	}
}
