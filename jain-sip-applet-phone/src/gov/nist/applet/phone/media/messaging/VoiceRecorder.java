/*
 * VoiceRecorder.java
 * 
 * Created on Mar 16, 2004
 *
 */
package gov.nist.applet.phone.media.messaging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.SizeChangeEvent;
import javax.media.StopAtTimeEvent;
import javax.media.StopByRequestEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

/**
 * Class allowing one to record some audio in a buffer
 * Play only MPEG_AUDIO and GSM audio data
 * With some minor modifications can play RAW data also
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class VoiceRecorder implements ControllerListener, DataSinkListener, Runnable{
	Processor p;
	Object waitSync = new Object();
	Object waitRecord = new Object();
	Object waitBuff = new Object();
	boolean stateTransitionOK = true;
	static boolean monitorOn = false;
	private MediaLocator audioLocator=null;		
	DataSourceHandler handler =null;
	Thread recorderThread=null;
	public static final String STOPPED="Stopped";		
	public static final String INITIALIZED="Initialized";
	public static final String RECORDING="Recording";	
	private String state=STOPPED;
	private static VoiceRecorder instance=null;
	private DataSource ods = null;
	
	private VoiceRecorder(){	
		state=STOPPED;						
	}
	
	public static synchronized VoiceRecorder getInstance(){
		if(instance==null)
			instance=new VoiceRecorder();
		return instance;
	}
	
	/**
	 * get the devices for the audio capture and print their formats
	 */
	public synchronized void initialize() {	
		if(state.equalsIgnoreCase(STOPPED)){			
			CaptureDeviceInfo audioCDI=null;
			Vector captureDevices=null;
			captureDevices= CaptureDeviceManager.getDeviceList(null);
			System.out.println("- number of capture devices: "+captureDevices.size() );
			CaptureDeviceInfo cdi=null;
			for (int i = 0; i < captureDevices.size(); i++) {
				cdi = (CaptureDeviceInfo) captureDevices.elementAt(i);		
				Format[] formatArray=cdi.getFormats();
				for (int j = 0; j < formatArray.length; j++) {
					Format format=formatArray[j];				
				   if (format instanceof AudioFormat) {
						if (audioCDI == null) {
							audioCDI=cdi;
						}
				   }			   
				}
			}
			if(audioCDI!=null)
				audioLocator=audioCDI.getLocator();
				
			DataSource ds = null;
			// Create a DataSource given the media locator.
			try {
				ds = Manager.createDataSource(audioLocator);
			} catch (Exception e) {
				System.err.println("Cannot create DataSource from: " + audioLocator);
				e.printStackTrace();
			}		

			try {
				p = Manager.createProcessor(ds);
			} catch (Exception e) {
				System.err.println("Failed to create a processor from the given DataSource: " + e);
				e.printStackTrace();			
			}

			p.addControllerListener(this);

			// Put the Processor into configured state.
			p.configure();
			if (!waitForState(Processor.Configured)) {
				System.err.println("Failed to configure the processor.");				
			}
			setTrackFormat();
			/*ContentDescriptor[] descriptors = p.getSupportedContentDescriptors();
			for (int n = 0; n < descriptors.length; n++) {
				System.out.println("Desc: " + descriptors[n].toString());
			}*/
			// Get the raw output from the processor.
			//p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
			//p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.MPEG_AUDIO));
			p.realize();
			if (!waitForState(Controller.Realized)) {
				System.err.println("Failed to realize the processor.");				
			}

			// Get the output DataSource from the processor and
			// hook it up to the RawDataSourceHandler.
			ods = p.getDataOutput();			
			handler = new DataSourceHandler();
			try {
				handler.setSource(ods);
			} catch (IncompatibleSourceException e) {
				System.err.println("Cannot handle the output DataSource from the processor: " + ods);
				//return false;
			}	
			System.err.println("Start datasource handler ");
			handler.addDataSinkListener(this);
			
			System.err.println("Prefetch the processor ");
			// Prefetch the processor.
			p.prefetch();
			if (!waitForState(Controller.Prefetched)) {
				System.err.println("Failed to prefetch the processor.");				
			}		
			state=INITIALIZED;				
		}						
	}

	/**
	 * Set the format of the tracks
	 * either to MPEG_AUDIO or GSM
	 */
	protected void setTrackFormat(){
		//Get the tracks from the processor
		TrackControl[] tracks = p.getTrackControls();

		// Do we have atleast one track?
		if (tracks == null || tracks.length < 1)
	 		System.out.println("Couldn't find tracks in processor");
	 		
		// Set the output content descriptor to GSM
		// This will limit the supported formats reported from
		// Track.getSupportedFormats to only valid AVI formats.
		//p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.MPEG_AUDIO));
		p.setContentDescriptor(new FileTypeDescriptor(FileTypeDescriptor.GSM));
		
		Format supported[];
		Format chosen=null;
	 	boolean atLeastOneTrack = false;
		
		// Program the tracks.
		for (int i = 0; i < tracks.length; i++) {
			Format format = tracks[i].getFormat();
			if (tracks[i].isEnabled()) {
				supported = tracks[i].getSupportedFormats();
				/*System.out.println("track : "+ i);
				for(int j=0;j<supported.length;j++)
				System.out.println("Supported format : "+supported[j].getEncoding());*/
				// We've set the output content to the GSM.            
				if (supported.length > 0) {
					for(int j=0;j<supported.length;j++){
						System.out.println("Supported format : "+supported[j].toString().toLowerCase());
				 		if (supported[j] instanceof AudioFormat) {
							chosen = supported[j];  
						}
					}
					if(chosen!=null){
						tracks[i].setFormat(chosen);                
						System.err.println("Track " + i + " is set to transmit as:");
						System.err.println("  " + chosen);
						atLeastOneTrack = true;
					}
				} else
					tracks[i].setEnabled(false);
			} else
				tracks[i].setEnabled(false);
		}
	}

	/**
	 * Given a DataSource, create a processor and hook up the output
	 * DataSource from the processor to a customed DataSink.
	 * @return false if something wrong happened
	 */
	protected boolean record() {
		if(state.equalsIgnoreCase(INITIALIZED)){
			try{
				handler.start();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}						
			// Start the processor.
			p.start();			
			System.err.println("processor started");				
			state=RECORDING;
			synchronized(waitRecord){
				waitRecord.notifyAll();
			}
			
			return true;		
		}
		return true;
	}

	/**
	 * Block until file writing is done. 
	 */
	/*private boolean waitForFileDone(double duration) {		
		synchronized (waitFileSync) {
			try {
				while (!bufferingDone) {
					if(p.getMediaTime().getSeconds() > duration)
						p.close();
					waitFileSync.wait(500);
					System.err.print(".");
				}
			} catch (Exception e) {}
		}
		bufferingDone=false;
		return true;
	}*/		
	
	/**
	 * Block until the processor has transitioned to the given state.
	 * @param state - the state to wait for
	 * @return false if the transition failed.
	 */
	protected boolean waitForState(int state) {
		synchronized (waitSync) {
			try {
			while (p.getState() < state && stateTransitionOK)
				waitSync.wait();
			} catch (Exception e) {}
		}
		return stateTransitionOK;
	}

	protected void waitForRecording() {
		synchronized (waitRecord) {
			try {
				while ( !state.equalsIgnoreCase(RECORDING))
					waitRecord.wait();
			} catch (Exception e) {}
		}		
	}
	
	/**
	 * Close the voice recording
	 */
	public synchronized void close(){
		System.out.println("Closing voice recorder");
		System.out.println(state);
		if(p!=null)		
			p.close();
		if(handler!=null)
			handler.close();				
		instance=null;
		state=STOPPED;
		System.out.println(state);		
	}
	
	/**
	 * Tells if the voice recorder is cloaed
	 * @return true if it is closed
	 */
	public static boolean isClosed(){
		if(instance==null)
			return true;
		return false;
	}

	/**
	 * Stop the voice recording
	 */
	public synchronized boolean stop(){
		System.out.println(state);
		if(state.equalsIgnoreCase(STOPPED)){
			System.out.println("Cannot stop recording, it didn't start");
			return false;
		}				
		p.stop();					
		return true;
	}
	
	/**
	 * Start the voice recording
	 */
	public synchronized boolean start(){		
		System.out.println(state);	
		if(state.equalsIgnoreCase(RECORDING)){
			System.out.println("Already recording...");
			return false;
		}						
		//initialize();
		if(recorderThread==null){
			recorderThread=new Thread(this);
			recorderThread.setName("Voice Recorder Thread");
		}
					
		boolean succeeded=record();
		if(!succeeded){			
			return false;			
		}
		recorderThread.start();		
		return true;		
	}
	
	/**
	 * the process of recording the voice
	 */
	public void run(){	
		synchronized (waitBuff) {
			try {
				while ( state.equalsIgnoreCase(RECORDING))
					waitBuff.wait();
			} catch (Exception e) {}
		}								
		recorderThread=null;									
	}

	/**
	 * Controller Listener Method.
	 * Allow one to know what happen on the recorder and the voice
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
			System.err.println("End of Media");
			System.err.println("closing handler" );
			//try{
				handler.close();
			/*}
			catch(IOException ioe){
				ioe.printStackTrace();	
			}*/			
			state=STOPPED;
			/*synchronized(waitBuff){
				waitBuff.notifyAll();
			}*/						
		} else if (evt instanceof SizeChangeEvent) {
		}
		else if (evt instanceof MediaTimeSetEvent) {
			System.err.println("- mediaTime set: " + 
			((MediaTimeSetEvent)evt).getMediaTime().getSeconds());
		} else if (evt instanceof StopAtTimeEvent) {			
			System.err.println("- stop at time: " +
			((StopAtTimeEvent)evt).getMediaTime().getSeconds());
			System.err.println("stoping handler" );
			try{
				handler.stop();
			}
			catch(IOException ioe){
				ioe.printStackTrace();	
			}
			System.err.println("...done Buffering.");
			state=INITIALIZED;
			synchronized(waitBuff){
				waitBuff.notifyAll();
			}			
		}
		else if (evt instanceof StopByRequestEvent) {
			System.err.println("Stop by request");				
			//Clean up		
		  	System.err.println("stoping handler" );
		  	try{
				handler.stop();
		  	}
		  	catch(IOException ioe){
				ioe.printStackTrace();	
		  	}
		  	System.err.println("...done Buffering.");
			state=INITIALIZED;
			synchronized(waitBuff){
				waitBuff.notifyAll();
			}												
		}
	}

	/**
	 * Get the recorded voice buffer 
	 * @return the voice recorded in an array of bytes
	 */
	public byte[] getRecord(){
		if(handler==null)
			return null;		
		return handler.getRecordBuffer();
	}

	/**
	 * DataSink Listener
	 * @param evt - event received  
	 */
	public void dataSinkUpdate(DataSinkEvent evt) {

		if (evt instanceof EndOfStreamEvent) {				
			//waitFileSync.notifyAll();
			//state=INITIALIZED;
			System.out.println("End of Stream event received");
			synchronized(waitBuff){
				waitBuff.notifyAll();
			}
			System.err.println("closing datasink" );
			evt.getSourceDataSink().close();
			state=STOPPED;
			/*try{
				evt.getSourceDataSink().stop();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}*/
			
			//System.exit(0);
		}
		else if (evt instanceof DataSinkErrorEvent) {
			//synchronized (waitFileSync) {
			System.out.println("Data sink error event received");
			state=STOPPED;
			synchronized(waitBuff){
				waitBuff.notifyAll();
			}	
			System.err.println("closing datasink" );
			evt.getSourceDataSink().close();			
			/*try{
				evt.getSourceDataSink().stop();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}*/
		}
	}

	/**
	 * Utility method to write a recorded voice buffer to a file
	 * @param data -  the recorded voice
	 */
	private static void writeBufferToFile(byte[] data){
		File f=new File("F://test.mov");
		FileOutputStream fos=null;
		try{
			fos=new FileOutputStream(f);
		}
		catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}
		try{
			fos.write(data);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	/**
	 * Main program
	 * @param args - 
	 */
	public static void main(String [] args) {
		VoiceRecorder voiceRecorder = new VoiceRecorder();
		VoicePlayer voicePlayer=new VoicePlayer();	
		//for(int i=0;i<2;i++){
			voiceRecorder.start();		
			try{
				Thread.sleep(5000);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}
			voiceRecorder.stop();				
			voicePlayer.initialize(voiceRecorder.getRecord());
			voicePlayer.play();	
			try{
				Thread.sleep(5000);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}				
		//}
		writeBufferToFile(voiceRecorder.getRecord());	
	}
}
