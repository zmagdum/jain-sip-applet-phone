/*
 * VideoRecorder.java
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
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
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
public class VideoRecorder implements ControllerListener, DataSinkListener, Runnable{
	Processor p;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	static boolean monitorOn = false;
	private MediaLocator videoLocator=null;	
	boolean bufferingDone = false;
	RawDataSourceHandler handler =null;
	Thread recorderThread=null;		
	DataSource ds = null;
	/**
	 * get the devices for the audio capture and print their formats
	 */
	protected void initialize() {		
		CaptureDeviceInfo videoCDI=null;
		Vector captureDevices=null;
		captureDevices= CaptureDeviceManager.getDeviceList(null);
		System.out.println("- number of capture devices: "+captureDevices.size() );
		CaptureDeviceInfo cdi=null;
		for (int i = 0; i < captureDevices.size(); i++) {
			cdi = (CaptureDeviceInfo) captureDevices.elementAt(i);		
			Format[] formatArray=cdi.getFormats();
			for (int j = 0; j < formatArray.length; j++) {
				Format format=formatArray[j];				
			   if (format instanceof VideoFormat) {
					if (videoCDI == null) {
						videoCDI=cdi;
					}
			   }			   
			}
		}
		if(videoCDI!=null)
			videoLocator=videoCDI.getLocator();
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
		p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
		
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
				 		if (supported[j] instanceof VideoFormat) {
							if(supported[j].toString().toLowerCase().indexOf("rgb")!=-1){
								chosen = supported[j];  
								break;
							}
						}
					}
					if(chosen!=null){
						tracks[i].setFormat(chosen);                
						System.err.println("Track " + i + " is set to transmit as:");
						System.err.println("  " + chosen);
						atLeastOneTrack = true;
					}
					else{
						System.err.println("Track " + i + " is set to transmit as nothing");
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
		// Create a DataSource given the media locator.
		try {
			ds = Manager.createDataSource(videoLocator);
		} catch (Exception e) {
			System.err.println("Cannot create DataSource from: " + videoLocator);
			return false;
		}		
		
		try {
			p = Manager.createProcessor(ds);
		} catch (Exception e) {
			System.err.println("Failed to create a processor from the given DataSource: " + e);
			return false;
		}

		p.addControllerListener(this);

		// Put the Processor into configured state.
		p.configure();
		if (!waitForState(Processor.Configured)) {
			System.err.println("Failed to configure the processor.");
			return false;
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
			return false;
		}
		
		// Get the output DataSource from the processor and
		// hook it up to the RawDataSourceHandler.
		DataSource ods = p.getDataOutput();
		handler = new RawDataSourceHandler();

		try {
			handler.setSource(ods);
		} catch (IncompatibleSourceException e) {
			System.err.println("Cannot handle the output DataSource from the processor: " + ods);
			//return false;
		}
		System.err.println("Start datasource handler ");
		handler.addDataSinkListener(this);
		try{
			handler.setSource(ds);
			handler.start();
		}
		catch(IncompatibleSourceException ioe){
			ioe.printStackTrace();
		}
		System.err.println("Prefetch the processor ");
		// Prefetch the processor.
		p.prefetch();
		if (!waitForState(Controller.Prefetched)) {
			System.err.println("Failed to prefetch the processor.");
			return false;
		}		
		// Start the processor.
		p.start();			
		System.err.println("processor started");				

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

	/**
	 * Stop the voice recording
	 */
	public void stop(){
		p.stop();
		bufferingDone=true;		
	}
	
	/**
	 * Start the voice recording
	 */
	public void start(){
		initialize();
		if(recorderThread==null){
			recorderThread=new Thread(this);
			recorderThread.setName("Voice Recorder Thread");
		}
			
		recorderThread.start();			
	}
	
	/**
	 * the process of recording the voice
	 */
	public void run(){
		boolean succeeded=record();
		if(!succeeded)
			return;
		while(!bufferingDone){
			try{
				recorderThread.sleep(1);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}
		}	
		try{
			Thread.sleep(100);
		}
		catch(InterruptedException ie){
			ie.printStackTrace();
		}	
		//Clean up
		System.err.println("closing datasource" );
		try{
			ds.stop();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		ds.disconnect();						
		System.err.println("closing processor" );
		p.close();
		p.removeControllerListener(this);
		recorderThread=null;
		System.err.println("closing handler" );
		handler.close();		
		System.err.println("...done Buffering.");
		bufferingDone=false;
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
			System.err.println("closing datasource" );
			try{
				ds.stop();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			ds.disconnect();						
			System.err.println("closing controller");
			evt.getSourceController().close();
			//Clean up
			System.err.println("closing processor" );
			p.close();
			p.removeControllerListener(this);
			recorderThread=null;
			System.err.println("closing handler" );
			handler.close();		
			System.err.println("...done Buffering.");
			bufferingDone=true;
		} else if (evt instanceof SizeChangeEvent) {
		}
		else if (evt instanceof MediaTimeSetEvent) {
			System.err.println("- mediaTime set: " + 
			((MediaTimeSetEvent)evt).getMediaTime().getSeconds());
		} else if (evt instanceof StopAtTimeEvent) {
			System.err.println("- stop at time: " +
			((StopAtTimeEvent)evt).getMediaTime().getSeconds());
			//Clean up
			System.err.println("closing datasource" );
			try{
				ds.stop();
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			ds.disconnect();						
			System.err.println("closing controller");
			evt.getSourceController().close();
			System.err.println("closing processor" );
			p.close();
			p.removeControllerListener(this);
			recorderThread=null;
			System.err.println("closing handler" );
			handler.close();		
			System.err.println("...done Buffering.");
			bufferingDone=true;
		}
		else if (evt instanceof StopByRequestEvent) {				
			//			Clean up
		  System.err.println("closing datasource" );
		  try{
			  ds.stop();
		  }
		  catch(IOException ioe){
			  ioe.printStackTrace();
		  }
		  ds.disconnect();
			System.err.println("closing controller");
			evt.getSourceController().close();						
		  	System.err.println("closing processor" );
		  	p.close();
		  	p.removeControllerListener(this);
		  	recorderThread=null;
		  	System.err.println("closing handler" );
		  	handler.close();		
		  	System.err.println("...done Buffering.");
		}
	}

	/**
	 * Get the recorded voice buffer 
	 * @return the voice recorded in an array of bytes
	 */
	public byte[] getRecord(){
		return handler.getRecordBuffer();
	}

	/**
	 * DataSink Listener
	 * @param evt - event received  
	 */
	public void dataSinkUpdate(DataSinkEvent evt) {

		if (evt instanceof EndOfStreamEvent) {
			bufferingDone = true;	
			//waitFileSync.notifyAll();
			System.err.println("All done!");
			evt.getSourceDataSink().close();
			//System.exit(0);
		}
		else if (evt instanceof DataSinkErrorEvent) {
			//synchronized (waitFileSync) {
			bufferingDone = true;	
			evt.getSourceDataSink().close();			
				//waitFileSync.notifyAll();
			//}
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
		VideoRecorder videoRecorder = new VideoRecorder();
		VideoPlayer videoPlayer=new VideoPlayer();	
		//for(int i=0;i<2;i++){
			videoRecorder.start();		
			try{
				Thread.sleep(5000);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}
			//videoRecorder.stop();				
			videoPlayer.initialize(videoRecorder.getRecord());
			videoPlayer.play();	
			try{
				Thread.sleep(5000);
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}				
		//}
		writeBufferToFile(videoRecorder.getRecord());
	}
}
