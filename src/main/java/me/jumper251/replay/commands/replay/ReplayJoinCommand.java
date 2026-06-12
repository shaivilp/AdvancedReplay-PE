package me.jumper251.replay.commands.replay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import me.jumper251.replay.commands.AbstractCommand;
import me.jumper251.replay.commands.SubCommand;
import me.jumper251.replay.filesystem.Messages;
import me.jumper251.replay.replaysystem.replaying.ReplayHelper;
import me.jumper251.replay.replaysystem.replaying.Replayer;

public class ReplayJoinCommand extends SubCommand {

	public ReplayJoinCommand(AbstractCommand parent) {
		super(parent, "join", "Joins an active replay as a spectator", "join <Name>", true);
	}

	@Override
	public boolean execute(CommandSender cs, Command cmd, String label, String[] args) {
		if (args.length != 2) return false;

		String name = args[1];
		Player p = (Player) cs;

		if (ReplayHelper.replaySessions.containsKey(p.getName())) {
			p.sendMessage(Messages.PREFIX.getFullMessage() + "§cYou are already in a replay.");
			return true;
		}

		Replayer replayer = ReplayHelper.activeReplayers.get(name);
		if (replayer == null) {
			p.sendMessage(Messages.PREFIX.getFullMessage() + "§cThere is no active replay with that name to join.");
			return true;
		}

		replayer.addViewer(p);
		p.sendMessage(Messages.PREFIX.getFullMessage() + "§7You joined the replay §e" + name + "§7.");
		return true;
	}

	@Override
	public List<String> onTab(CommandSender cs, Command cmd, String label, String[] args) {
		return new ArrayList<>(ReplayHelper.activeReplayers.keySet()).stream()
				.filter(name -> StringUtil.startsWithIgnoreCase(name, args.length > 1 ? args[1] : null))
				.collect(Collectors.toList());
	}
}
