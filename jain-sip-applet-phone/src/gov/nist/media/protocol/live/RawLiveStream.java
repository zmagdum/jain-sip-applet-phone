/*
 * RawLiveStream.java
 *
 * Created on March 20, 2003, 10:56 AM
 */
package gov.nist.media.protocol.live;

import java.awt.Dimension;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import java.io.*;
/**
 * A stream who read directly from an array of byte
 * This Stream allow one to play or play back a buffer filled by RAW
 * audio data
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class RawLiveStream implements PushBufferStream, Runnable {

    protected ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);
    protected int maxDataLength; 
    protected byte [] data;
    protected Dimension size;
    protected RGBFormat rgbFormat;
    protected AudioFormat audioFormat;
    protected boolean started;
    protected Thread thread;
    protected float frameRate = 20f;
    protected BufferTransferHandler transferHandler;
    protected Control [] controls = new Control[0];

    //protected boolean videoData = false;
    
    /**
     * Create a new RawLiveStream
     */
    public RawLiveStream() {

		/*if (videoData) {
		    int x, y, pos, revpos;
		    
		    size = new Dimension(320, 240);
		    maxDataLength = size.width * size.height * 3;
		    rgbFormat = new RGBFormat(size, maxDataLength,
					      Format.byteArray,
					      frameRate,
					      24,
					      3, 2, 1,
					      3, size.width * 3,
					      VideoFormat.FALSE,
					      Format.NOT_SPECIFIED);
		    
		    // generate the data
		    data = new byte[maxDataLength];
		    pos = 0;
		    revpos = (size.height - 1) * size.width * 3;
		    for (y = 0; y < size.height / 2; y++) {
				for (x = 0; x < size.width; x++) {
				    byte value = (byte) ((y*2) & 0xFF);
				    data[pos++] = value;
				    data[pos++] = 0;
				    data[pos++] = 0;
				    data[revpos++] = value;
				    data[revpos++] = 0;
				    data[revpos++] = 0;
				}
				revpos -= size.width * 6;
		    }
		} else { */// audio data
		    audioFormat = new AudioFormat(AudioFormat.LINEAR,
						  44100.0,
						  16,
						  2,
						  AudioFormat.LITTLE_ENDIAN,
						  AudioFormat.SIGNED,
						  8,
						  Format.NOT_SPECIFIED,
						  Format.byteArray);
		    maxDataLength = 822000;
		//}
	
		thread = new Thread(this);
		thread.setName("RawLiveStream Thread");
    }

    /***************************************************************************
     * SourceStream
     ***************************************************************************/
	/**
	 * Get the current content type for this stream.
	 *
	 * @return The current <CODE>ContentDescriptor</CODE> for this stream.
	 */
    public ContentDescriptor getContentDescriptor() {
		return cd;
    }
	/**
	 * Get the size, in bytes, of the content on this stream.
	 *
	 * @return The content length in bytes.
	 */
    public long getContentLength() {
		return LENGTH_UNKNOWN;
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
    public boolean endOfStream() {
		return false;
    }

    /***************************************************************************
     * PushBufferStream
     ***************************************************************************/

    int seqNo = 0;
    double freq = 2.0;
    
    /**
     * @see javax.media.protocol.PushBufferStream#getFormat()
     */
    public Format getFormat() {
		/*if (videoData)
		    return rgbFormat;
		else*/
		    return audioFormat;
    }
	/**
	 * Fill the buffer with the raw audio data
	 */
    public void read(Buffer buffer) throws IOException {
		synchronized (this) {
			System.out.println("read");
		    Object outdata = buffer.getData();
		    if (outdata == null || !(outdata.getClass() == Format.byteArray) ||
			((byte[])outdata).length < maxDataLength) {
				System.out.println("null buffer, creating the buffer");
				maxDataLength=data.length;
				outdata = new byte[maxDataLength];
				buffer.setData(outdata);
		    }
		    /*if (videoData) {
				buffer.setFormat( rgbFormat );
				buffer.setTimeStamp( (long) (seqNo * (1000 / frameRate) * 1000000) );
				int lineNo = (seqNo * 2) % size.height;
				int chunkStart = lineNo * size.width * 3;
				System.arraycopy(data, chunkStart,
						 outdata, 0,
						 maxDataLength - (chunkStart));
				if (chunkStart != 0) {
				    System.arraycopy(data, 0,
						     outdata, maxDataLength - chunkStart,
						     chunkStart);
				}
		    } 
		    else {*/
				buffer.setFormat( audioFormat );
				//buffer.setTimeStamp( 1000000000 / 8 );
				for (int i = 0; i < maxDataLength; i++) {			
				    ((byte[])outdata)[i] = data[i];
				}
		    //}	    
			System.out.println(((byte[])outdata).length);
		    buffer.setSequenceNumber( seqNo );
		    buffer.setLength(maxDataLength);
		    buffer.setFlags(0);
		    buffer.setHeader( null );
		    seqNo++;
		}
    }
	/**
	 * @see javax.media.protocol.PushBufferStream#setTransferHandler(javax.media.protocol.BufferTransferHandler)
	 */
    public void setTransferHandler(BufferTransferHandler transferHandler) {
		synchronized (this) {
		    this.transferHandler = transferHandler;
		    notifyAll();
		}
    }
	/**
	 * Start the thread 
	 * @param started
	 */
    void start(boolean started) {
		synchronized ( this ) {
		    this.started = started;
		    if (started && !thread.isAlive()) {
				thread = new Thread(this);
				thread.setName("RawLiveStream Thread");
				thread.start();
		    }
		    notifyAll();
		}
    }

    /***************************************************************************
     * Runnable
     ***************************************************************************/
	/**
	 * the execution method of the thread
	 */
    public void run() {
		while (started) {
		    synchronized (this) {
				while (transferHandler == null && started) {
				    try {
						wait(1000);
				    } 
				    catch (InterruptedException ie) {}
				} // while
		    }
	
		    if (started && transferHandler != null) {
				transferHandler.transferData(this);
				try {
				    Thread.currentThread().sleep( 10 );
				} 
				catch (InterruptedException ise) {}
		    }
		} // while (started)
    } // run
	
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

       } 
       catch (Exception e) {   // no such controlType or such control
         return null;
       }
    }

}