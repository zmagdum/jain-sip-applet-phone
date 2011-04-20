package gov.nist.applet.phone.media.receiver;

import java.awt.*;

import javax.media.*;
import javax.media.rtp.*;

/**
 * GUI classes for the Player.
 */
public class PlayerWindow extends Frame {

	Player player;
	ReceiveStream stream;

	public PlayerWindow(Player p, ReceiveStream strm) {
		player = p;
		stream = strm;
	}

	public PlayerWindow(Player p) {
		player = p;
	}
		
	public void initialize() {
		add(new PlayerPanel(player));
	}

	public void close() {
		player.close();
		setVisible(false);
		dispose();
	}

	public void addNotify() {
		super.addNotify();
		pack();
	}
}