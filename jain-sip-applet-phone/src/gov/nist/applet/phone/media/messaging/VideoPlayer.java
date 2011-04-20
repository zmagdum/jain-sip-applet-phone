/*
 * VideoPlayer.java
 * 
 * Created on Mar 17, 2004
 *
 */
package gov.nist.applet.phone.media.messaging;

import gov.nist.applet.phone.media.receiver.PlayerWindow;
import gov.nist.media.protocol.live.RawLiveDataSource;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.GainControl;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.PackageManager;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.StartEvent;
import javax.media.StopAtTimeEvent;

import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import com.sun.media.MediaPlayer;

/**
 * Class allowing one to play back some audio contained in a buffer
 * Play only MPEG_AUDIO and GSM audio data
 * With some minor modifications can play RAW data also
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class VideoPlayer implements ControllerListener{	
	Player player=null;	
	PlayerWindow pw = null;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;		
	double duration=5.0;			
	gov.nist.media.protocol.live.DataSource ds = null;	
		
	/**
	 * Initialize the voice player with the audio buffer
	 * @param data - the buffered voice to play
	 */	
	public void initialize(byte[] data){
		checkForPackage();		
		Vector ppl=PackageManager.getProtocolPrefixList();
		for(int i=0; i<ppl.size();i++)
			System.out.println(ppl.get(i));
		String url="live:";
		MediaLocator outML=new MediaLocator(url);		

		// Create a DataSource given the media locator.
		try {
			ds = new gov.nist.media.protocol.live.DataSource(
				data,
				new ContentDescriptor(ContentDescriptor.RAW).getContentType());	
			ds.connect();			
		} catch (Exception e) {
			e.printStackTrace();			
		}	
		System.err.println("create processor for: " + ds.getContentType());
		System.err.println("Classname of DataSource: " + ds.getClass().getName());
		//Set the data of this DataSource
		//((gov.nist.media.protocol.live.DataSource)ds).setBuffer(data);						
								
		try {
			player = Manager.createPlayer(ds);
			//player = new MediaPlayer();
			pw = new PlayerWindow(player);
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
	
	/**
	 * Play the voice
	 */
	public void play(){
		//Set the stop time if there's one set.
	  	//if (duration > 0)
			//player.setStopTime(new Time(duration));
	  	// Start the player.
	  	player.start();			
	}
	
	/**
	 * Block until the processor has transitioned to the given state.
	 * @param state - the state to wait for
	 * @return false if the transition failed.
	 */
	protected boolean waitForState(int state) {
		synchronized (waitSync) {
			try {
			while (player.getState() < state && stateTransitionOK)
				waitSync.wait();
			} catch (Exception e) {}
		}
		return stateTransitionOK;
	}
				
	/**
	 * Controller Listener Method.
	 * Allow one to know what happen on the player and the voice
	 * @param evt - event received 
	 */
	public void controllerUpdate(ControllerEvent evt) {
		Player p = (Player)evt.getSourceController();

		if (p == null)
			return;

		// Get this when the internal players are realized.
		if (evt instanceof RealizeCompleteEvent) {
			if (pw != null) {
				pw.initialize();
				pw.setVisible(true);
			}
		}

		if (evt instanceof ControllerErrorEvent) {
			p.removeControllerListener(this);
			if (pw != null) {
				pw.close();
			}
			System.err.println("Receiver internal error: " + evt);
		}
		if (evt instanceof StartEvent) {
			GainControl gc=p.getGainControl();
			System.out.println("Class for gain contol"+gc);
			if(gc!=null){
				Component c=gc.getControlComponent();
				System.out.println("Class for component"+c);
			}
		}

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
		VideoPlayer videoPlayer=new VideoPlayer();
		byte[] data=readFile("f:"+File.separator+"test.mp3");
		System.out.println(data[0]);
		videoPlayer.initialize(data);
		videoPlayer.play();				
	}

}
