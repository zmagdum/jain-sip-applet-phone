/*
 * Call.java
 * 
 * Created on Mar 16, 2004
 *
 */
package gov.nist.applet.phone.ua.call;

import javax.sip.Dialog;

/**
 * This interface represents a call that can be received by the application
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public interface Call {
	public static final String NOT_IN_A_CALL="Not in a call";
	public static final String TRYING="Trying...";
	public static final String RINGING="Ringing...";
	public static final String IN_A_CALL="In a call";
	public static final String INCOMING_CALL="Incoming call";
	public static final String BUSY="Busy";
	public static final String CANCEL="Cancel";
	public static final String TEMPORARY_UNAVAILABLE="Temporary unavailable";
	/**
	 * Retrieve the callee of this call
	 * @return the callee of this call
	 */
	public String getCallee();
	/**
	 * Set the callee of this call
	 * @param callee - the callee of this call
	 */
	public void setCallee(String callee);
	/**
	 * Retrieve the dialog of the call
	 * @return the dialog of the call
	 */
	public Dialog getDialog();
	/**
	 * Set the dialog of the call
	 * @param dialog - the dialog of the call
	 */
	public void setDialog(Dialog dialog);
	
	/**
	 * Retrieve the current status of the call
	 * @return the current status of the call
	 */
	public String getStatus();

	/**
	 * Set the current status of the call
	 * @param callStatus - the current status of the call
	 */
	public void setStatus(String callStatus);
		
}
