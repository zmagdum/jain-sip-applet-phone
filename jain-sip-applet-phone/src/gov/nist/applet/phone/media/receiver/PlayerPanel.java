package gov.nist.applet.phone.media.receiver;

import java.awt.*;

import javax.media.*;

/**
 * GUI classes for the Player.
 */
public class PlayerPanel extends Panel {

	Component vc, cc;

	public PlayerPanel(Player p) {
		setLayout(new BorderLayout());
		if ((vc = p.getVisualComponent()) != null)
			add("Center", vc);
		if ((cc = p.getControlPanelComponent()) != null)
			add("South", cc);
	}

	public Dimension getPreferredSize() {
		int w = 0, h = 0;
		if (vc != null) {
			Dimension size = vc.getPreferredSize();
			w = size.width;
			h = size.height;
		}
		if (cc != null) {
			Dimension size = cc.getPreferredSize();
			if (w == 0)
				w = size.width;
			h += size.height;
		}
		if (w < 160)
			w = 160;
		return new Dimension(w, h);
	}
}
