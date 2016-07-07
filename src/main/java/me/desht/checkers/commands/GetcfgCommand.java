package me.desht.checkers.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.checkers.CheckersPlugin;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class GetcfgCommand extends AbstractCheckersCommand {

	public GetcfgCommand() {
		super("checkers getcfg", 0, 1);
		setPermissionNode("checkers.commands.getcfg");
		setUsage("/<command> getcfg  [<config-key>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		List<String> lines = getPluginConfiguration(args.length >= 1 ? args[0] : null);
		if (lines.size() > 1) {
			MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
			for (String line : lines) {
				pager.add(line);
			}
			pager.showPage();
		} else if (lines.size() == 1) {
			MiscUtil.statusMessage(sender, lines.get(0));
		}
		return true;
	}
	public List<String> getPluginConfiguration() {
		return getPluginConfiguration(null);
	}

	private List<String> getPluginConfiguration(String section) {
		ArrayList<String> res = new ArrayList<>();
		Configuration config = CheckersPlugin.getInstance().getConfig();
		ConfigurationSection cs = config.getRoot();

		Set<String> items;
		if (section == null) {
			items = config.getDefaults().getKeys(true);
		} else {
			if (config.getDefaults().isConfigurationSection(section)) {
				cs = config.getConfigurationSection(section);
				items = config.getDefaults().getConfigurationSection(section).getKeys(true);
			} else {
				items = new HashSet<>();
				if (config.getDefaults().contains(section))
					items.add(section);
			}
		}

		for (String k : items) {
			if (cs.isConfigurationSection(k))
				continue;
			res.add("&f" + k + "&- = '&e" + cs.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}

	@NotNull
	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getConfigCompletions(sender, CheckersPlugin.getInstance().getConfig(), args[0]);
		default:
			return noCompletions(sender);
		}
	}
}
