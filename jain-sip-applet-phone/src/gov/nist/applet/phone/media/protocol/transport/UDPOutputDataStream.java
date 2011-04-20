/*
 * UDPOutputDataStream.java
 *
 * Created on November 18, 2003, 11:33 PM
 */

package gov.nist.applet.phone.media.protocol.transport;

import javax.media.rtp.OutputDataStream;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

/**
 * This class is an implementation of the OutputDataStream for
 * The UDP transport protocol underlying to RTP. 
 * @author  DERUELLE Jean
 */
public class UDPOutputDataStream implements OutputDataStream{        
    private DatagramSocket sock;
    private InetAddress addr;
    private int port;

    /** 
     * Creates a new instance of UDPOutputDataStream.
     * @param sock - the UDP socket from which we will get the output stream.     
     * @param addr - the address of the remote host
     * @param port - the port of the remote host
     */
    public UDPOutputDataStream(DatagramSocket sock, InetAddress addr, int port) {
        this.sock = sock;
        this.addr = addr;
        this.port = port;
    }
    /**
     * Write some data in the output stream.
     * @param data - data to write.
     * @param offset - the start offset in the data.
     * @param len - the number of bytes to write.
     * @return number of bytes written. -1 if there is any exception caught
     */
    public int write(byte data[], int offset, int len) {
        try {
            DatagramPacket p=new DatagramPacket(data, offset, len, addr, port);
            //System.out.println("Sending RTP packet "+p+" to "+p.getAddress()+"/"+p.getPort());
            sock.send(p);
        } catch (Exception e) {               
            return -1;
        }
        return len;
    }
    
}
