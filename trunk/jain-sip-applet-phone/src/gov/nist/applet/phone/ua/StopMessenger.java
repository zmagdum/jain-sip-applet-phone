/*
 * StopMessenger.java
 * 
 * Created on Mar 26, 2004
 *
 */
package gov.nist.applet.phone.ua;

import java.util.Iterator;

import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipProvider;
import javax.sip.SipStack;

/**
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class StopMessenger implements Runnable {
	private Thread stopThread=null;
	private MessageListener messageListener=null;
	/**
	 * 
	 */
	public StopMessenger(MessageListener messageListener) {
		this.messageListener=messageListener;
		if(stopThread==null){
			stopThread=new Thread(this);
			stopThread.setName("Stop Messenger Thread");
		}
			
		stopThread.start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		SipStack sipStack=messageListener.sipStack;		
		if (sipStack==null) return;                        					
		Iterator listeningPoints=sipStack.getListeningPoints();
		if (listeningPoints!=null) {
			while( listeningPoints.hasNext()) {
				ListeningPoint lp=(ListeningPoint)listeningPoints.next();							
				try{
					sipStack.deleteListeningPoint(lp);
					lp=null;
					System.out.println("One listening point removed!");	
				}
				catch(ObjectInUseException oiue){
					oiue.printStackTrace();					
				}				
				listeningPoints=sipStack.getListeningPoints();				
			}
		}
		else {
			System.out.println("WARNING, STOP, The NIST Messenger" +
				" has no listening points to remove!");
		} 
		try{
			Thread.currentThread().sleep(1000);
		}
		catch(InterruptedException ie){
			ie.printStackTrace();
		}
		Iterator sipProviders=sipStack.getSipProviders();
		if (sipProviders!=null) {
			while( sipProviders.hasNext()) {
				SipProvider sp=(SipProvider)sipProviders.next();                    
				sp.removeSipListener(messageListener);
				try{
					sipStack.deleteSipProvider(sp);
					sp=null;
					System.out.println("One sip Provider removed!");
				}
				catch(ObjectInUseException oiue){
					System.out.println("Waiting for the sip providers to " +
						"release their references");
					try{
						stopThread.sleep(2000);
					}
					catch(InterruptedException ie){
						ie.printStackTrace();
					}
				}
				sipProviders=sipStack.getSipProviders();				
			}
		}
		else {
			System.out.println("WARNING, STOP, NIST messenger" +
				" has no sip Provider to remove!");
		}
		sipStack=null;
		System.gc();      							
	}

}
