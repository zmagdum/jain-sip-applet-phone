/*
 * UDPPushSourceStream.java
 *
 * Created on November 18, 2003, 11:36 PM
 */

package gov.nist.applet.phone.media.protocol.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import javax.media.protocol.SourceTransferHandler;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.ContentDescriptor;
/**
 * This class is an implementation of the PushSourceStream for
 * The UDP transport protocol underlying to RTP. 
 * @author  DERUELLE Jean
 */
public class UDPPushSourceStream extends Thread implements PushSourceStream {
    
    private DatagramSocket sock;
    private InetAddress addr;
    private int port;
    private boolean done = false;
    private boolean dataRead = false;

    private SourceTransferHandler sth = null;

    /** 
     * Creates a new instance of UDPPushSourceStream.
     * @param sock - the UDP socket from which we will get the input stream.     
     * @param addr - the address of the remote host
     * @param port - the port of the remote host
     */
    public UDPPushSourceStream(DatagramSocket sock, InetAddress addr, int port) {
        this.sock = sock;
        this.addr = addr;
        this.port = port;
    }

    /**
     * Read some data from the input stream.
     * @param buffer - the buffer into which the data is read.
     * @param offset - the start offset in array buffer at which the data is written.
     * @param len - the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer,
     * or -1 if there is no more data because the end of the stream has been reached.
     */
    public int read(byte buffer[], int offset, int length) {
        DatagramPacket p = new DatagramPacket(buffer, offset, length, addr, port);
        try {
            sock.receive(p);            
        } catch (IOException e){               
            return -1;
        }
        synchronized (this) {
            dataRead = true;
            notify();
        }
        return p.getLength();
    }
    /**
     * Start this thread
     */
    public synchronized void start() {
        super.start();
        if (sth != null) {
            dataRead = true;
            notify();
        }
    }
    /**
     * Kill this thread
     */
    public synchronized void kill() {
        done = true;
        notify();
    }
    /**
     * Determine the size of the buffer needed for the data transfer. 
     * This method is provided so that a transfer handler can determine how much data, 
     * at a minimum, will be available to transfer from the source. 
     * Overflow and data loss is likely to occur if this much data isn't read at transfer time.
     * @return The size of the data transfer.
     */
    public int getMinimumTransferSize() {
        return 2 * 1024;	// twice the MTU size, just to be safe.
    }
    /**
     * Register an object to service data transfers to this stream.
     *
     * If a handler is already registered when setTransferHandler is called, 
     * the handler is replaced; there can only be one handler at a time.
     * @param sth - The handler to transfer data to.
     */
    public synchronized void setTransferHandler(SourceTransferHandler sth) {
        this.sth = sth;
        dataRead = true;
        notify();
    }
    /**
     * Get the current content type for this stream.
     * @return The current ContentDescriptor for this stream.
     */
    // Not applicable.
    public ContentDescriptor getContentDescriptor() {
        return null;
    }
    /**
     * Get the size, in bytes, of the content on this stream. 
     * LENGTH_UNKNOWN is returned if the length is not known.
     * @return The content length in bytes.
     */
    // Not applicable.
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }
    /**
     * Find out if the end of the stream has been reached.
     * @return Returns true if there is no more data.
     */
    // Not applicable.
    public boolean endOfStream() {
        return false;
    }
    /**
     * Obtain the collection of objects that control the object that implements this interface.
     * If no controls are supported, a zero length array is returned.
     * @return the collection of object controls
     */
    // Not applicable.
    public Object[] getControls() {
        return new Object[0];
    }
    /**
     * Obtain the object that implements the specified Class or Interface  The full class or interface name must be used.
     * If the control is not supported then null is returned.
     * @return the object that implements the control, or null.
     */
    // Not applicable.
    public Object getControl(String type) {
        return null;
    }

    /**
     * Loop and notify the transfer handler of new data.
     */
    public void run() {
        while (!done) {

            synchronized (this) {
                while (!dataRead && !done) {
                    try {
                        wait();
                    } catch (InterruptedException e) { }
                }
                dataRead = false;
            }

            if (sth != null && !done) {
                sth.transferData(this);
            }
        }
    }
    
}
