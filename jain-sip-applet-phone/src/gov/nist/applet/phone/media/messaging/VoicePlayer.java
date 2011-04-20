/*
 * VoicePlayer.java
 * 
 * Created on Mar 17, 2004
 *
 */
package gov.nist.applet.phone.media.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.PackageManager;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.StopAtTimeEvent;

import javax.media.protocol.DataSource;

/**
 * Class allowing one to play back some audio contained in a buffer
 * Play only MPEG_AUDIO and GSM audio data
 * With some minor modifications can play RAW data also
 * This class use double buffering to correctly handle a continuous flow of message
 * This is done because to stop and close the handler it takes some time
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class VoicePlayer implements ControllerListener{	
	Player player=null; 
	Player player2=null;	
	Object waitSync = new Object();
	boolean stateTransitionOK = true;		
	double duration=5.0;			
	DataSource ds = null;		
	Integer doubleBuffer=new Integer(0);	
		
	public VoicePlayer(){
		checkForPackage();
	}
		
	/**
	 * Initialize the voice player with the audio buffer
	 * @param data - the buffered voice to play
	 */	
	public void initialize(byte[] data){								
		/*Vector ppl=PackageManager.getProtocolPrefixList();
		for(int i=0; i<ppl.size();i++)
			System.out.println(ppl.get(i));*/		
		String url="live:";
		MediaLocator outML=new MediaLocator(url);		

		// Create a DataSource given the media locator.
		try {
			ds = Manager.createDataSource(outML);					
		} catch (Exception e) {
			e.printStackTrace();			
		}	
		System.err.println("create processor for: " + ds.getContentType());
		System.err.println("Classname of DataSource: " + ds.getClass().getName());
		//Set the data of this DataSource
		((gov.nist.media.protocol.live.DataSource)ds).setBuffer(data);						
		if(doubleBuffer.intValue()==0){				
			try {
				player = Manager.createPlayer(ds);
			} catch (Exception e) {
				System.err.println("Failed to create a player from the given DataSource: " + e);
				e.printStackTrace();			
			}
	
			player.addControllerListener(this);		
	
			// Get the raw output from the processor.
			player.realize();		
			if (!waitForState(Controller.Realized)) {
				System.err.println("Failed to realize the processor.");			
			}
	
			// Get the output DataSource from the processor and
			// hook it up to the RawDataSourceHandler.		
	
			// Prefetch the processor.
			player.prefetch();
			if (!waitForState(Controller.Prefetched)) {
				System.err.println("Failed to prefetch the processor.");			
			}			
		}
		else{
			try {
				player2 = Manager.createPlayer(ds);
			} catch (Exception e) {
				System.err.println("Failed to create a player from the given DataSource: " + e);
				e.printStackTrace();			
			}

			player2.addControllerListener(this);		

			// Get the raw output from the processor.
			player2.realize();		
			if (!waitForState(Controller.Realized)) {
				System.err.println("Failed to realize the processor.");			
			}

			// Get the output DataSource from the processor and
			// hook it up to the RawDataSourceHandler.		

			// Prefetch the processor.
			player2.prefetch();
			if (!waitForState(Controller.Prefetched)) {
				System.err.println("Failed to prefetch the processor.");			
			}				
		}
	}
	/**
	 * 
	 * @param data
	 */
	/*public synchronized void setData(byte[] data){	
		player.stop();
		player.deallocate();		
		//Set the data of this DataSource
		((gov.nist.media.protocol.live.DataSource)ds).setBuffer(data);	
		try {
			player.setSource(ds);		
		}
		catch(Exception ioe){
			ioe.printStackTrace();
		}		 
	}*/
	
	/*public boolean isInitialized(){
		return initialized;
	}*/
	
	/**
	 * Play the voice
	 */
	public void play(){
		//Set the stop time if there's one set.
	  	//if (duration > 0)
			//player.setStopTime(new Time(duration));
	  	// Start the player.
	  	synchronized(doubleBuffer){	  	
			if(doubleBuffer.intValue()==0){
				player.start();
				doubleBuffer=new Integer(1);
			}	  		
		  	else{
				player2.start();
				doubleBuffer=new Integer(0);
		  	}
	  	}
	}
	
	/**
	 * Block until the processor has transitioned to the given state.
	 * @param state - the state to wait for
	 * @return false if the transition failed.
	 */
	protected boolean waitForState(int state) {
		synchronized (waitSync) {
			if(doubleBuffer.intValue()==0){	
				try {
					while (player.getState() < state && stateTransitionOK)
						waitSync.wait();
				} catch (Exception e) {}
			}
			else{
				try {
					while (player2.getState() < state && stateTransitionOK)
						waitSync.wait();
				} catch (Exception e) {}
			}
			
		}
		return stateTransitionOK;
	}
				
	/**
	 * Controller Listener Method.
	 * Allow one to know what happen on the player and the voice
	 * @param evt - event received 
	 */
	public void controllerUpdate(ControllerEvent evt) {
		//System.out.println("new Event received"+evt.getClass().getName());
		if (evt instanceof ConfigureCompleteEvent ||
			evt instanceof RealizeCompleteEvent ||
			evt instanceof PrefetchCompleteEvent) {
			synchronized (waitSync) {
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		} else if (evt instanceof ResourceUnavailableEvent) {
			synchronized (waitSync) {
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} else if (evt instanceof EndOfMediaEvent) {				
			evt.getSourceController().stop();		
			evt.getSourceController().close();			
		} else if (evt instanceof SizeChangeEvent) {
		}
		else if (evt instanceof MediaTimeSetEvent) {
			System.err.println("- mediaTime set: " + 
			((MediaTimeSetEvent)evt).getMediaTime().getSeconds());
		} else if (evt instanceof StopAtTimeEvent) {
			System.err.println("- stop at time: " +
			((StopAtTimeEvent)evt).getMediaTime().getSeconds());
			ds.disconnect();	
			evt.getSourceController().close();				
			player.close();		
			ds=null;					
		}
	}
	
	/**
	 * Utility method allowing one to read a file in order to get the audio data
	 * @param fileName - the file to read
	 * @return the audio data buffered in an array of byte
	 */
	public static byte[] readFile(String fileName){
		byte[] dataTemp= new byte[822000];
		File f=new File(fileName);
		FileInputStream fis=null;
		try{
			fis=new FileInputStream(fileName);
		}
		catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}
		try{
			fis.read(dataTemp);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		return dataTemp;
	}
	
	/**
	 * check if the gov.nist package is registered with jmf if not it is added
	 */
	public static void checkForPackage(){
		boolean packageFound=false;
		Vector protocols=PackageManager.getProtocolPrefixList();
		for(int i=0;i<protocols.size();i++){
			if(protocols.get(i).equals("gov.nist"))
				packageFound=true;
		}
		if(!packageFound){
			protocols.addElement("gov.nist");
			PackageManager.setProtocolPrefixList(protocols);
			PackageManager.commitProtocolPrefixList();
		}
	}
	
	/**
	 * Main method
	 * @param args - 
	 */
	public static void main(String[] args){		
		VoicePlayer voicePlayer=new VoicePlayer();
		byte[] data=readFile("d://temp//test.mp3");
		voicePlayer.initialize(data);
		voicePlayer.play();				
	}

}
