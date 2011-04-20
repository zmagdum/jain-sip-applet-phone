/*
 * Created on Mar 24, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package gov.nist.applet.phone.ua.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * @author DERUELLE Jean
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface NISTMessengerGUI {

	/**
	 * Get the contact list from this frame
	 * @return the contact list from this frame
	 */
	public JList getContactList();

	/**
	 * Get the contact list from this frame
	 * @return the contact list from this frame
	 */
	public JButton getRemoveContactButton();
	
	public void repaint();
	
	/**
	 * Get the view component representing the logged status label
	 * @return the logged status label
	 */
	public JLabel getLoggedStatusLabel() ;


	/** Display a fatal error and then die.
         */
	public void fatalError(String string);
	
}
