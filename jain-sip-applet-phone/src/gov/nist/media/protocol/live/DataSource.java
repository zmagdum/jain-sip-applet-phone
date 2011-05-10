/*
 * DataSource.java
 *
 * Created on March 20, 2003, 8:56 AM
 */
package gov.nist.media.protocol.live;

import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PullDataSource;

import java.io.IOException;

/**
 * A default data-source created directly from an array of byte
 * This Data source allow one to play or play back a buffer filled by MPEG_AUDIO
 * or GSM audio data
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class DataSource extends PullDataSource {

	protected ContentDescriptor contentType;
	protected ByteStream[] sources; 
	protected boolean connected;
	protected byte[] buffer;
    
    /**
     * Construct a <CODE>DataSource</CODE> unconnected containing nothing
     * The <CODE>setBuffer</CODE> method need to be called to process data
     */
	public DataSource(){
		connected=false;
	}
     
	 /**
	 * Construct a <CODE>DataSource</CODE> from a byte array.
	 * @param input - the array of byte used to construct the DataSource
	 * @param contentType - the content Type of the Data Source
	 * @throws IOException - if there is an I/O problem
	 */
	public DataSource(byte[] input, String contentType) throws IOException {
		buffer = input;
		this.contentType = new ContentDescriptor(contentType);
		connected = false;
	}
    
	/**
	 * The <CODE>connect</CODE> method initiates communication with the source.
	 *
	 * @exception IOException Thrown if there are IO problems
	 * when <CODE>connect</CODE> is called.
	 */
	public void connect() throws java.io.IOException {
		connected = true;

	}

	/**
	 * The <CODE>disconnect</CODE> method frees resources used to maintain a
	 * connection to the source.
	 * If no resources are in use, <CODE>disconnect</CODE> is ignored.
	 */
	public void disconnect() {
		if(connected) {									
			sources[0].close();
			sources=null;
			buffer=null;			
			connected = false;
		}
	}
    
	/**
	 * Get a string that describes the content-type of the media
	 * that the source is providing.
	 * <p>
	 * It is an error to call <CODE>getContentType</CODE> if the source is
	 * not connected.
	 *
	 * @return The name that describes the media content.
	 */
	public String getContentType() {
		if( !connected) {
	   		throw new java.lang.Error("Source is unconnected.");
		}
		if(contentType!=null)
			return contentType.getContentType();
		else
			//return new ContentDescriptor(FileTypeDescriptor.MPEG_AUDIO).getContentType();
			return new ContentDescriptor(FileTypeDescriptor.GSM).getContentType();
	}
	
	/**
	 * @see javax.media.Controls#getControl(java.lang.String)
	 */
    public Object getControl(String str) {
		return null;
	}
    /**
     * @see javax.media.Controls#getControls()
     */
	public Object[] getControls() {
		return new Object[0];
	}
    /**
     * @see javax.media.Duration#getDuration()
     */
	public Time getDuration() {
		return new Time(5.0);
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
	public javax.media.protocol.PullSourceStream[] getStreams() {
		if( !connected) {
	   		throw new java.lang.Error("Source is unconnected.");
		}
		return sources;
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
	}
    
	/**
	 * Stop the data-transfer.
	 * If the source has not been connected and started,
	 * <CODE>stop</CODE> does nothing.
	 */
	public void stop() throws IOException {		
	}
	
	/**
	 * Set the buffer of this <code>DataSource</code>
	 * @param buffer
	 */
	public void setBuffer(byte[] buffer){
		this.buffer=buffer;
		sources = new ByteStream [1];
		sources[0] = new ByteStream(buffer);
	}
}

