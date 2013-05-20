package me.desht.checkers.view.controlpanel;

import java.util.List;

import me.desht.dhutils.MessagePager;

import org.bukkit.event.player.PlayerInteractEvent;

public class BoardInfoButton extends AbstractSignButton {
	
	public BoardInfoButton(ControlPanel panel) {
		super(panel, "boardInfoBtn", "list.board", 0, 2);
	}
	
	@Override
	public void execute(PlayerInteractEvent event) {
		MessagePager pager = MessagePager.getPager(event.getPlayer()).clear();
		List<String> l = getView().getBoardDetail();
		pager.add(l);
		pager.showPage();
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
