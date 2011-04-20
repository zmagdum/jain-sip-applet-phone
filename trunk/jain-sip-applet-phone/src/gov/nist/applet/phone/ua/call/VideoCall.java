/*
 * CallStatus.java
 *
 * Created on November 25, 2003, 4:03 PM
 */

package gov.nist.applet.phone.ua.call;

import javax.sip.Dialog;
import javax.sip.ServerTransaction;
import javax.sip.address.URI;
import javax.sip.message.Request;

import gov.nist.applet.phone.media.*;
import gov.nist.applet.phone.ua.MessageListener;
/**
 * This class will keep information about an audio call
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class VideoCall implements Call{    
    private String callStatus=null;
    private String callee=null;
    private Dialog dialog=null;
    private URI url=null;
	private Request request=null;
	private ServerTransaction serverTransaction=null;
    private MediaManager mediaManager=null;    
    private boolean videoMessaging=false;
        
    /** Creates a new instance of an audio Call */
    public VideoCall(MessageListener messageListener) {
        callStatus=NOT_IN_A_CALL;
        mediaManager=new MediaManager(messageListener);       
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
	 * Retrieve the MediaManager for this call
	 * @return the media manager of the call
	 */
	public MediaManager getMediaManager(){
		return mediaManager;
	}
    
    /**
     * Set the MediaManager for this call
     * @param mediaManager - the media manager of the call
     */
    public void setMediaManager(MediaManager mediaManager){
        this.mediaManager=mediaManager;
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
    /**
     * enable or not the voice messaging for this call
     * @param voiceMessaging - flag to enable the voice messaging for this call
     */
    public void setVideoMesaging(boolean videoMessaging){
    	this.videoMessaging=videoMessaging;
    }
    /**
     * Return true if the voice messaging is enabled for this call
     * @return true if the voice messaging is enabled for this call
     */
    public boolean getVideoMesaging(){
    	return videoMessaging;
    }
    
	/**
	 * Retrieve the url set by a busy to this call
	 * @return the url set by a busy to this call
	 */
	public URI getURL(){
		return this.url;
	}

	/**
	 * Set the url set by a busy to this call
	 * @param url - the url set by a busy to this call
	 */
	public void setURL(URI url){
		this.url=url;
	}
}
