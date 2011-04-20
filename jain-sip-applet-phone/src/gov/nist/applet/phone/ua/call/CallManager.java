/*
 * CallManager.java
 * 
 * Created on Mar 15, 2004
 *
 */
package gov.nist.applet.phone.ua.call;

import java.util.Iterator;
import java.util.Vector;

/**
 * This class manage all the calls of the application
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class CallManager {
	private Vector imCalls=null;
	private Vector audioCalls=null;
	
	/**
	 * Creates a new instance of the call manager  
	 */
	public CallManager() {		
		imCalls=new Vector();
		audioCalls=new Vector();
	}

	/**
	 * Find the Audio call for a specified callee
	 * @param callee - the callee
	 * @return the Audio call currently bound to the callee 
	 */
	public AudioCall findAudioCall(String callee){
		//System.out.println("Test against "+ callee);
		Iterator it=audioCalls.iterator();
		while(it.hasNext()){
			AudioCall call=(AudioCall)it.next();
			//System.out.println("callee "+ call.getCallee());
			if(call.getCallee().equalsIgnoreCase(callee))
				return call;	
		}		
		return null;
	}
	
	/**
	 * Find the Instant Messaging call for a specified callee
	 * @param callee - the callee
	 * @return the IM call currently bound to the callee 
	 */
	public IMCall findIMCall(String callee){
		//System.out.println("Test against "+ callee);
		Iterator it=imCalls.iterator();
		while(it.hasNext()){
			IMCall call=(IMCall)it.next();
			//System.out.println("callee "+ call.getCallee());
			if(call.getCallee().equalsIgnoreCase(callee))
				return call;	
		}			
		return null;
	}
	/**
	 * Find an existing call with the matching dialogId in parameter 
	 * @param dialogId - the dialog Id of the searched call 
	 * @return call matching the dialogId in parameter
	 */
	public Call findCall(String dialogId){
		Iterator it=imCalls.iterator();
		while(it.hasNext()){
			Call call=(Call)it.next();
			//System.out.println("callee "+ call.getCallee());			
			if(call.getDialog().getDialogId().equalsIgnoreCase(dialogId))
				return call;	
		}		
		it=audioCalls.iterator();
		while(it.hasNext()){
			Call call=(Call)it.next();
			//System.out.println("callee "+ call.getCallee());
			if(call.getDialog().getDialogId().equalsIgnoreCase(dialogId))
				return call;	
		}		
		return null;
	}
	
	/**
	 * Add a new Media call to the call manager
	 * @param audioCall - the Media call to add
	 */
	public void addAudioCall(AudioCall audioCall){		
		audioCalls.add(audioCall);					
	}
	
	/**
	 * Add a new Instant Messaging call to the call manager
	 * @param imCall - the Instant Messaging call to add
	 */
	public void addIMCall(IMCall imCall){
		imCalls.add(imCall);					
	}
	
	/**
	 * Remove an Audio call from the call manager
	 * @param audioCall - the audio call to remove
	 */
	public void removeAudioCall(AudioCall audioCall){
		audioCalls.remove(audioCall);		
	}
	
	/**
	 * Remove an Instant Messaging call from the call manager
	 * @param imCall - the Instant Messaging call to remove
	 */
	public void removeIMCall(IMCall imCall){
                if (imCall.getDialog() != null) {
                        imCall.getDialog().delete();
                }
		imCalls.remove(imCall);		
	}

	/** Remove an im call given its id.
	*@param imcallId -- id for removing the IM call.
	*/
	public void  removeIMCall(String callId) {
		Iterator it = audioCalls.iterator();
		while (it.hasNext() ) {
			Call c = (Call) it.next();
			IMCall call;
			if (c  instanceof IMCall) {
				call = (IMCall) c;
			} else {
				 continue;
			}
			if (call.getCallee().equals(callId) ) it.remove();
                        if (call.getDialog() != null) {
                                call.getDialog().delete();
                        }
		}
	}
	
	/**
	 * Tells if a media session is already active
	 * @return true if there is already an audio call active
	 */
	public boolean isAlreadyInAudioCall(){
		Iterator it=audioCalls.iterator();
		while(it.hasNext()){
			Call call=(Call)it.next();
			if(call.getStatus().equalsIgnoreCase(Call.IN_A_CALL))
				return true;	
		}		
		return false;	
	}
}
