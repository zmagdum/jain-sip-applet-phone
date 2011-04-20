/*
 * RawLiveDataSource.java
 *
 * Created on March 20, 2003, 8:56 AM
 */
package gov.nist.media.protocol.live;

import javax.media.Time;
import javax.media.protocol.*;
import java.io.IOException;

/**
 * A default data-source created directly from an array of byte
 * This Data source allow one to play or play back a buffer filled by RAW 
 * audio data
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class RawLiveDataSource extends PushBufferDataSource {

    protected Object [] controls = new Object[0]; 
    protected boolean started = false;    
    protected boolean connected = false;
    protected Time duration = DURATION_UNKNOWN;
    protected RawLiveStream [] streams = null;
    protected RawLiveStream stream = null;
    
    /**
     * Constructs a new instance of RawLiveDataSource
     *
     */
    public RawLiveDataSource() {
    }
    
	/**
	 * Get the current content type for this stream.
	 *
	 * @return The current <CODE>ContentDescriptor</CODE> for this stream.
	 */
    public String getContentType() {
		if (!connected){
	    	System.err.println("Error: DataSource not connected");
	        return null;
	    }
	    return ContentDescriptor.RAW;		
    }

	/**
	 * The <CODE>connect</CODE> method initiates communication with the source.
	 *
	 * @exception IOException Thrown if there are IO problems
	 * when <CODE>connect</CODE> is called.
	 */
    public void connect() throws IOException {
		 if (connected)
	            return;
		 connected = true;
    }
    
	/**
	 * The <CODE>disconnect</CODE> method frees resources used to maintain a
	 * connection to the source.
	 * If no resources are in use, <CODE>disconnect</CODE> is ignored.
	 */
    public void disconnect() {
		try {
            if (started)
                stop();
        } catch (IOException e) {}
		connected = false;
    }

	/**
	 * Initiate data-transfer. The <CODE>start</CODE> method must be
	 * called before data is available.
	 *(You must call <CODE>connect</CODE> before calling <CODE>start</CODE>.)
	 *
	 * @exception IOException Thrown if there are IO problems with the source
	 * when <CODE>start</CODE> is called.
	 */
    public void start() throws IOException {
	// we need to throw error if connect() has not been called
        if (!connected)
            throw new java.lang.Error("DataSource must be connected before it can be started");
        if (started)
            return;
		started = true;
		stream.start(true);
    }

	/**
	 * Stop the data-transfer.
	 * If the source has not been connected and started,
	 * <CODE>stop</CODE> does nothing.
	 */
    public void stop() throws IOException {
		if ((!connected) || (!started))
		    return;
		started = false;
		stream.start(false);
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
		return controls;
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
    public Object getControl(String controlType) {
       try {
          Class  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }

	/**
	 * Set the buffer of this <code>DataSource</code>
	 * @param buffer
	 */
	public void setBuffer(byte[] buffer){
		if (streams == null) {
			streams = new RawLiveStream[1];
			stream = streams[0] = new RawLiveStream();
		}
		stream.data=buffer;
	}
	/**
	 * @see javax.media.Duration#getDuration()
	 */
    public Time getDuration() {
		return duration;
    }
	/**
	 * Get the collection of streams that this source
	 * manages. The collection of streams is entirely
	 * content dependent. The  MIME type of this
	 * <CODE>DataSource</CODE> provides the only indication of
	 * what streams can be available on this connection.
	 *
	 * @return The collection of streams for this source.
	 */
    public PushBufferStream [] getStreams() {
		if (streams == null) {
		    streams = new RawLiveStream[1];
		    stream = streams[0] = new RawLiveStream();
		}
		return streams;
    }
    
}