/*
 * Created on Feb 7, 2004
 */
package gov.nist.applet.phone.ua.presence;

import javax.sip.Dialog;

/**
 * This class represents a suscriber to our presence
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Subscriber {
	private Dialog dialog;
	private String address;
	private String status;
	/**
	 * Constructs a new instance of subscriber
	 */
	public Subscriber(String address) {
		this.address=address;
		status="offline";
	}

	/**
	 * Retrieve the Dialog from which we get the subscriber
	 * @return the Dialog from which we get the subscriber
	 */
	public Dialog getDialog(){
		return this.dialog;
	}

	/**
	 * Set the Dialog from which we get the subscriber
	 * @param dialog - the serverTransaction from which we get the subscriber
	 */
	public void setDialog(Dialog dialog){
		this.dialog=dialog;
	}
	
	/**
	 * Retrieve the address of the subscriber
	 * @return the address of the subscriber
	 */
	public String getAddress(){
		return this.address;
	}

	/**
	 * Set the address of the subscriber
	 * @param address - the address of the subscriber
	 */
	public void setAddress(String address){
		this.address=address;
	}
	
	/**
	 * Retrieve the status of the subscriber
	 * @return the status of the subscriber
	 */
	public String getStatus(){
		return this.status;
	}

	/**
	 * Set the status of the subscriber
	 * @param status - the address of the subscriber
	 */
	public void setStatus(String status){
		this.status=status;
	}
	
}
