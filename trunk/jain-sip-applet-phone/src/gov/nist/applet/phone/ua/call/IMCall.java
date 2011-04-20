/*
 * IMCall.java
 * 
 * Created on Mar 15, 2004
 *
 */
package gov.nist.applet.phone.ua.call;

import javax.sip.Dialog;

/**
 * This class will keep information about an instant messaging or a voice 
 * messaging call
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class IMCall implements Call{
	private String callee=null;
	private Dialog dialog=null;
	private String callStatus=null;
	/**
	 * Constructs a new IMCall
	 */
	public IMCall(String callee) {
                System.out.println("creating IM Call " + callee);
		setCallee(callee);				
	}

	/**
	 * Retrieve the current status of the call
	 * @return the current status of the call
	 */
	public String getStatus(){
		return this.callStatus;
	}

	/**
	 * Set the current status of the call
	 * @param callStatus - the current status of the call
	 */
	public void setStatus(String callStatus){
		this.callStatus=callStatus;
	}

	/**
	 * Retrieve the dialog of the call
	 * @return the dialog of the call
	 */
	public Dialog getDialog(){
		return this.dialog;
	}

	/**
	 * Set the dialog of the call
	 * @param dialog - the dialog of the call
	 */
	public void setDialog(Dialog dialog){
		this.dialog=dialog;
	}

	/**
	 * Retrieve the callee of this call
	 * @return the callee of this call
	 */
	public String getCallee(){
		return this.callee;
	}	

	/**
	 * Set the callee of this call
	 * @param callee - the callee of this call
	 */
	public void setCallee(String callee){
		this.callee=callee;
		this.callee=this.callee.replace('<',' ');
		this.callee=this.callee.replace('>',' ');
		this.callee=this.callee.trim();
		//this.callee=this.callee.substring("sip:".length(),this.callee.length());
	}
}
