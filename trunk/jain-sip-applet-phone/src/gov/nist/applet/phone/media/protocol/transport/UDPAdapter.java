/*
 * RTPSocketAdapter.java
 *
 * Created on November 18, 2003, 3:08 PM
 */

package gov.nist.applet.phone.media.protocol.transport;

/**
 *
 * @author  DERUELLE Jean
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.SocketException;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.OutputDataStream;


/**
 * An implementation of RTPConnector based on UDP sockets.
 */
public class UDPAdapter implements RTPConnector {

    private DatagramSocket dataSock;
    private DatagramSocket ctrlSock;

    private InetAddress addr;
    private int port;    
    //private int destinationPort;    
    private int rtcpPort;
    //private int rtcpDestinationPort;
    
    private UDPPushSourceStream dataInStrm = null, ctrlInStrm = null;
    private UDPOutputDataStream dataOutStrm = null, ctrlOutStrm = null;

    /** Creates a new instance of UDPAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     */
    public UDPAdapter(InetAddress addr, int port) throws IOException {
		this(addr, port, 1);
    }
    /** Creates a new instance of UDPAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     * @param ttl - time to live
     */
    public UDPAdapter(InetAddress addr, int port, int ttl) throws IOException {
        rtcpPort=port++;
        //rtcpDestinationPort=destPort++;
		try {	    
			System.out.println(addr+" port "+port+" rtcp" + rtcpPort);
            dataSock = new DatagramSocket(port, addr);
            ctrlSock = new DatagramSocket(rtcpPort, addr);	    
		} catch(SocketException e) {			
		    throw new IOException(e.getMessage());
		}
		this.addr = addr;
		this.port = port;
        //this.destinationPort = destPort;
    }

     /**
     * Returns an input stream to receive the RTP data.
     * @return input stream to receive the RTP data.
     */
    public PushSourceStream getDataInputStream() throws IOException {
		if (dataInStrm == null) {
		    dataInStrm = new UDPPushSourceStream(dataSock, addr, port);
		    dataInStrm.start();
		}
		return dataInStrm;
    }

     /**
     * Returns an output stream to send the RTP data.
     * @return output stream to send the RTP data.
     */
    public OutputDataStream getDataOutputStream() throws IOException {
		if (dataOutStrm == null)
		    dataOutStrm = new UDPOutputDataStream(dataSock, addr, port);
		return dataOutStrm;
    }

    /**
     * Returns an input stream to receive the RTCP data.
     * @return input stream to receive the RTCP data.
     */
    public PushSourceStream getControlInputStream() throws IOException {
		if (ctrlInStrm == null) {
		    ctrlInStrm = new UDPPushSourceStream(ctrlSock, addr, rtcpPort);
		    ctrlInStrm.start();
		}
		return ctrlInStrm;
    }

    /**
     * Returns an output stream to send the RTCP data.
     * @return output stream to send the RTCP data.
     */
    public OutputDataStream getControlOutputStream() throws IOException {
		if (ctrlOutStrm == null)
		    ctrlOutStrm = new UDPOutputDataStream(ctrlSock, addr, rtcpPort);
		return ctrlOutStrm;
    }

    /**
     * Close all the RTP, RTCP streams.
     */
    public void close() {
        System.out.println("Closing the streams");
		if (dataInStrm != null)
		    dataInStrm.kill();
		if (ctrlInStrm != null)
		    ctrlInStrm.kill();
        while(!dataSock.isClosed())
            dataSock.close();
        while(!ctrlSock.isClosed())
            ctrlSock.close();
        dataSock=null;
        ctrlSock=null;
    }

    /**
     * Set the receive buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     * @param size - receive buffer size 
     */
    public void setReceiveBufferSize( int size) throws IOException {
		dataSock.setReceiveBufferSize(size);
    }

    /**
     * Get the receive buffer size set on the RTP data channel.
     * Return -1 if the receive buffer size is not applicable for
     * the implementation.
     * @return receive buffer size 
     */
    public int getReceiveBufferSize() {
		try {
		    return dataSock.getReceiveBufferSize();
		} catch (Exception e) {
		    return -1;
		}
    }

    /**
     * Set the send buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     * @param size - send buffer size.
     */
    public void setSendBufferSize( int size) throws IOException {
		dataSock.setSendBufferSize(size);
    }

    /**
     * Get the send buffer size set on the RTP data channel.
     * Return -1 if the send buffer size is not applicable for
     * the implementation.
     * @return send buffer size 
     */
    public int getSendBufferSize() {
		try {
		    return dataSock.getSendBufferSize();
		} catch (Exception e) {
		    return -1;
		}
    }

    /**
     * Return the RTCP bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * @return RTCP bandwidth fraction. -1 to use the default values.
     */
    public double getRTCPBandwidthFraction() {
		return -1;
    }

    /**
     * Return the RTCP sender bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * Return -1 to use the default values.
     * @return RTCP sender bandwidth fraction. -1 to use the default values.
     */
    public double getRTCPSenderBandwidthFraction() {
		return -1;
    }
}

