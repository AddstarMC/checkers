package me.desht.checkers.view.controlpanel;

import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.dhutils.PermissionUtils;

abstract class AbstractSignButton extends AbstractSignLabel {

	private final String permissionNode;

	AbstractSignButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y) {
		super(panel, labelKey, x, y);

		this.permissionNode = permissionNode;
	}

	/**
	 * Called when the sign is clicked by the player.  Any CheckersException thrown by this method (and the abstract execute()
	 * method that it calls) will ultimately be caught and reported to the player by the PlayerInteractEvent event handler.
	 *
	 * @param event	The player interaction event as caught by the plugin's event handler
	 */
	public void onClicked(PlayerInteractEvent event) {
		if (isEnabled() && (isReactive() || isReactive(event))) {
			if (permissionNode != null) {
				PermissionUtils.requirePerms(event.getPlayer(), "checkers.commands." + permissionNode);
			}
			execute(event);
		}
	}

	@Override
	boolean isReactive() {
		return true;
	}

	boolean isReactive(PlayerInteractEvent event) {
		return false;
	}

	protected abstract void execute(PlayerInteractEvent event);

}
