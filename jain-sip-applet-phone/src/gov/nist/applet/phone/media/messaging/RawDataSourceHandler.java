/*
 * RawDataSourceHandler.java
 * 
 * Created on Mar 16, 2004
 *
 */
package gov.nist.applet.phone.media.messaging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.DataSink;
import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.SourceStream;

import javax.media.protocol.DataSource;

/**
 * This Data source Handler allow one to write directly from a DataSource 
 * to an array of byte.
 * This Data Source Handler allow one to fill a buffer with MPEG_AUDIO 
 * or GSM audio data.
 * Example : One can record his voice from a microphone to a buffer in either a
 * MP3 or GSM format. To get the buffer with the recorded voice just call the
 * getRecordBuffer method
 *
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class RawDataSourceHandler implements DataSink, BufferTransferHandler{
	public static final int NUMBER_OF_BYTES_FOR_ONE_SECOND=18928;
	DataSource source;
	PullBufferStream pullStrms[] = null;
	PushBufferStream pushStrms[] = null;
	//length of the buffer read
	int bufferLengthRead=0;
	//length of the buffer read
	int bufferLength=0;
	
	// Data sink listeners.
	private Vector listeners = new Vector(1);

	// Stored all the streams that are not yet finished (i.e. EOM
	// has not been received.
	SourceStream unfinishedStrms[] = null;

	// Loop threads to pull data from a PullBufferDataSource.
	// There is one thread per each PullSourceStream.
	Loop loops[] = null;

	Buffer readBuffer;
	ByteArrayOutputStream baout=null;
	
	/**
	 * Default Constructor
	 */
	public RawDataSourceHandler(){
		baout=new ByteArrayOutputStream();
	}
	
	/**
	 * 
	 * @param duration - duration to buffer in sec
	 */
	public RawDataSourceHandler(int duration){
		bufferLength=duration*NUMBER_OF_BYTES_FOR_ONE_SECOND;		
		baout=new ByteArrayOutputStream(bufferLength);
	}


	/**
     * Set the data source of this data source handler
     * @exception IncompatibleSourceException - if the data source cannot be 
     * handled, this exception is thrown
     */
	public void setSource(DataSource source) throws IncompatibleSourceException {
		//System.out.println(source.getClass().getName());
	    // Different types of DataSources need to handled differently.
	    if (source instanceof PushBufferDataSource) {

			pushStrms = ((PushBufferDataSource)source).getStreams();
			unfinishedStrms = new SourceStream[pushStrms.length];
	
			// Set the transfer handler to receive pushed data from
			// the push DataSource.
			for (int i = 0; i < pushStrms.length; i++) {
			    pushStrms[i].setTransferHandler(this);
			    unfinishedStrms[i] = pushStrms[i];
			}


	    } else if (source instanceof PullBufferDataSource) {

			pullStrms = ((PullBufferDataSource)source).getStreams();
			unfinishedStrms = new SourceStream[pullStrms.length];
	
			// For pull data sources, we'll start a thread per
			// stream to pull data from the source.
			loops = new Loop[pullStrms.length];
			for (int i = 0; i < pullStrms.length; i++) {
			    loops[i] = new Loop(this, pullStrms[i]);
			    unfinishedStrms[i] = pullStrms[i];
			}

	    } else {

			// This handler only handles push or pull buffer datasource.
			throw new IncompatibleSourceException();

	    }

	    this.source = source;
	    readBuffer = new Buffer();
	}


	/**
	 * For completeness, DataSink's require this method.
	 * But we don't need it.
	 */
	public void setOutputLocator(MediaLocator ml) {
	}

	/**
	 * For completeness, DataSink's require this method.
	 * But we don't need it.
	 */
	public MediaLocator getOutputLocator() {
	    return null;
	}

	/**
	 * Get the current content type for this data source handler
	 *
	 * @return The current <CODE>ContentDescriptor</CODE> for this data source handler.
	 */
	public String getContentType() {
	    return source.getContentType();
	}


	/**
	 * Our DataSink does not need to be opened.
	 */
	public void open() {
	}

	/**
	 * @see javax.media.DataSink#start()
	 */
	public void start() {
	    try {
			source.start();
	    } catch (IOException e) {
			System.err.println(e);
	    }

	    // Start the processing loop if we are dealing with a
	    // PullBufferDataSource.
	    if (loops != null) {
			for (int i = 0; i < loops.length; i++)
			    loops[i].restart();
	    }
	}

	/**
	 * @see javax.media.DataSink#stop()
	 */
	public void stop() {
	    try {
			source.stop();
	    } catch (IOException e) {
			System.err.println(e);
	    }

	    // Start the processing loop if we are dealing with a
	    // PullBufferDataSource.
	    if (loops != null) {
			for (int i = 0; i < loops.length; i++)
			    loops[i].pause();
	    }
	}

	/**
	 * @see javax.media.DataSink#close()
	 */
	public void close() {
	    stop();
	    if (loops != null) {
			for (int i = 0; i < loops.length; i++)
			    loops[i].kill();
	    }	
		System.out.println(bufferLengthRead+" bytes have been read");
		System.out.println("nb bytes actually in the buffer: "+baout.size());	
		//writeBufferToFile("d://temp//data.txt");  
	}

	/**
	 * @see javax.media.DataSink#addDataSinkListener(javax.media.datasink.DataSinkListener)
	 */
	public void addDataSinkListener(DataSinkListener dsl) {
	    if (dsl != null)
		if (!listeners.contains(dsl))
		    listeners.addElement(dsl);
	}

	/**
	 * @see javax.media.DataSink#removeDataSinkListener(javax.media.datasink.DataSinkListener)
	 */
	public void removeDataSinkListener(DataSinkListener dsl) {
	    if (dsl != null)
		listeners.removeElement(dsl);
	}

	/**
	 * Send event to the listeners
	 * @param event - the vent to send
	 */
	protected void sendEvent(DataSinkEvent event) {
	    if (!listeners.isEmpty()) {
		synchronized (listeners) {
		    Enumeration list = listeners.elements();
		    while (list.hasMoreElements()) {
			DataSinkListener listener = 
				(DataSinkListener)list.nextElement();
			listener.dataSinkUpdate(event);
		    }
		}
	    }
	}

	/**
	 * This will get called when there's data pushed from the
	 * PushBufferDataSource.
	 * @param stream - stream from which the data are transferred
	 */
	public void transferData(PushBufferStream stream) {
		//System.out.println("push");
	    try {
			stream.read(readBuffer);
	    } catch (IOException e) {
			System.err.println(e);
			sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
			return;
	    }

	    bufferData(readBuffer);

	    // Check to see if we are done with all the streams.
	    if (readBuffer.isEOM() && checkDone(stream)) {
			sendEvent(new EndOfStreamEvent(this));
	    }
	}


	/**
	 * This is called from the Loop thread to pull data from
	 * the PullBufferStream.
	 * @param stream - stream from which the data are pulled
	 */
	public boolean readPullData(PullBufferStream stream) {
		//System.out.println("pull");
	    try {
			stream.read(readBuffer);
	    } catch (IOException e) {			
			System.err.println(e);
			return true;
	    }

	    bufferData(readBuffer);

	    if (readBuffer.isEOM()) {
	        // Check to see if we are done with all the streams.
		if (checkDone(stream)) {
		    System.err.println("All done!");
		    close();
		}
		return true;
	    }
	    return false;
	}


	/**
	 * Check to see if all the streams are processed.
	 * @param strm - the stream to check
	 */
	public boolean checkDone(SourceStream strm) {
	    boolean done = true;

	    for (int i = 0; i < unfinishedStrms.length; i++) {
		if (strm == unfinishedStrms[i])
		    unfinishedStrms[i] = null;
		else if (unfinishedStrms[i] != null) {
		    // There's at least one stream that's not done.
		    done = false;
		}
	    }
	    return done;
	}

	/**
	 * Buffer the data given by the buffer in parameter in our data source handler
	 * buffer
	 * @param buffer - the buffer that provide data
	 */
	void bufferData(Buffer buffer) {
	    //System.err.println("Read from stream: " + stream);
	    /*if (buffer.getFormat() instanceof AudioFormat){
			System.err.println("Read audio data: "+buffer.getFormat());
	    }
	    else
			System.err.println("Read video data:");
	    System.err.println("  Time stamp: " + buffer.getTimeStamp());
	    System.err.println("  Sequence #: " + buffer.getSequenceNumber());
	    System.err.println("  Data length: " + buffer.getLength());*/
		Object data=buffer.getData();
		if(data instanceof byte[]){			
			byte[] audioData=(byte[])data;
			bufferLengthRead+=audioData.length;
			try{
				baout.write(audioData);
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
		}		
			
	    if (buffer.isEOM())
			System.err.println("  Got EOM!");
	}
	/**
	 * Obtain the collection of objects that
	 * control the object that implements this interface.
	 * <p>
	 *
	 * No controls are supported.
	 * A zero length array is returned.
	 *
	 * @return A zero length array
	 */
	public Object [] getControls() {
	    return new Object[0];
	}
	/**
	 * Obtain the object that implements the specified
	 * <code>Class</code> or <code>Interface</code>
	 * The full class or interface name must be used.
	 * <p>
	 *
	 * The control is not supported.
	 * <code>null</code> is returned.
	 *
	 * @return <code>null</code>.
	 */
	public Object getControl(String name) {
	    return null;
	}
	/**
	 * Get the recorded buffer of this data source handler
	 * @return byte array containing the data recorded
	 */
	public byte[] getRecordBuffer(){
		return baout.toByteArray();
	}
	/**
	 * Utility method to write the data source handler buffer to a file
	 * @param fileName - the file where to write the buffer
	 */
	public void writeBufferToFile(String fileName){
		File f=new File(fileName);
		FileOutputStream fos=null;
		try{
			fos=new FileOutputStream(fileName);
		}
		catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}
		try{
			fos.write(baout.toByteArray());
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
}
