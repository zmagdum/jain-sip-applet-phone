/*
 * Receiver.java
 *
 * Created on November 19, 2003, 10:38 AM
 */
package gov.nist.applet.phone.media.receiver;


import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.Vector;

import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.protocol.DataSource;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.control.BufferControl;
import javax.media.control.MpegAudioControl;
import javax.media.control.FrameRateControl;

import gov.nist.applet.phone.media.util.*;
import gov.nist.applet.phone.media.transmitter.*;
import gov.nist.applet.phone.media.protocol.transport.*;

/**
 * Class to receive RTP transmission using the JMF RTP API.
 * @author DERUELLE Jean
 */
public class Receiver implements ReceiveStreamListener, SessionListener, ControllerListener
{
    RTPManager mgrs[] = null;
    Vector playerWindows = null;
    private static boolean bye=false;
    boolean dataReceived = false;    
    SessionDescription sessionDescription=null;    
    Transmit transmitter=null;
    Socket socketRTPReceiver=null;
    Socket socketRTCPReceiver=null;
    /**
     * Constructor for Receiver 
     * @param session - the concatened parameters of the session stored in a string
     */
    public Receiver(String session) throws IllegalArgumentException{
        playerWindows = new Vector();
        //the session Label containing the address, the port and the Time To Live
        try {
           //create a session label on the session given in argument
           // and parse the session address.
           sessionDescription=new SessionDescription(session);
           sessionDescription.setAudioFormat("mpegaudio/rtp, 48000.0 hz, 16-bit, mono");
           sessionDescription.setVideoFormat("h263/rtp");
           sessionDescription.setTransportProtocol("tcp");
           if(sessionDescription.getVideoFormat()!=null && 
                sessionDescription.getAudioFormat()!=null)
               mgrs=new RTPManager[2];
           else if (sessionDescription.getVideoFormat()!=null ||
                sessionDescription.getAudioFormat()!=null)
               mgrs=new RTPManager[1];
           
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to parse the session address given: " + session);
            throw e;
        }
    }

    /**
     * Constructor for Receiver
     * @param session - the session Description containing the address, the port, the Time To Live
     * the video format, the audio format and the transport protocol
     */
    public Receiver(SessionDescription session,Transmit transmitter) throws IllegalArgumentException{
        this.sessionDescription=session;
        if(sessionDescription.getVideoFormat()!=null && 
            sessionDescription.getAudioFormat()!=null)
           mgrs=new RTPManager[2];
       else if (sessionDescription.getVideoFormat()!=null ||
            sessionDescription.getAudioFormat()!=null)
           mgrs=new RTPManager[1];
        playerWindows = new Vector();
        this.transmitter=transmitter;
    }
    
    /**
     * Initialize the RTP Mamagers an wait for the data
     * There is one by stream
     * @return false if the rtpmanagers can't be initialized or if no data was received
     */
    protected boolean initialize(String localIpAddress) {
		if(mgrs==null)
			return false;
        try {
            for(int i=0;i<mgrs.length;i++){
                if(i==0){
                //Creates a new instance of RTPManager
                //which will allow us to create, maintain and close an RTP session.
                mgrs[i] = (RTPManager) RTPManager.newInstance();
                // create the local endpoint for the local interface on the port given in parameter
                int localPort=sessionDescription.getLocalPort()+2*i;
                // specify the remote endpoint of this unicast session on the port given in parameter
                int destPort=sessionDescription.getDestinationPort()+2*i;
                SessionAddress localAddr = new SessionAddress(
                	InetAddress.getByName(localIpAddress),
                	localPort);
				mgrs[i].addSessionListener(this);
				// add the ReceiveStreamListener to receive data
				mgrs[i].addReceiveStreamListener(this);
                // initialize the RTPManager, so the session
                if(sessionDescription.getTransportProtocol().toLowerCase().equals("tcp")){
                    if(transmitter==null){
                        TCPConnectionListener listener;
                        TCPConnectionListener ctrlListener;                        
                        int rtcpLocalPort=localPort+1;    
                        try {	    
                            //Start the serverSocket for the RTP 
                            ServerSocket serverSocket = new ServerSocket(localPort);
                            System.out.println("TCP Listening Point created on port: "+localPort);
                            listener=new TCPConnectionListener(serverSocket,  false);
                            listener.start();
                        } catch(SocketException e) {
                            System.out.println(localPort+","+destPort);
                            throw new IOException(e.getMessage());
                        }        
                        try{                            
                            //Start the serverSocket for the RTCP 
                            ServerSocket ctrlServerSocket = new ServerSocket(rtcpLocalPort);
                            System.out.println("TCP Control Listening Point created on port: "+rtcpLocalPort);
                            ctrlListener=new TCPConnectionListener(ctrlServerSocket,  true);
                            ctrlListener.start();
                        } catch(SocketException e) {
                            System.out.println(rtcpLocalPort+","+destPort);
                            throw new IOException(e.getMessage());
                        }        
                        //Wait for connections
                        socketRTPReceiver=listener.waitForConnections();
                        socketRTCPReceiver=ctrlListener.waitForConnections();                                             
                    }
                    else{
                        socketRTPReceiver=transmitter.getSocketRTPTransmit();
                        socketRTCPReceiver=transmitter.getSocketRTCPTransmit();
                    }
                    mgrs[i].initialize(new TCPReceiveAdapter(socketRTPReceiver,socketRTCPReceiver));
                }
                else{
					System.out.println("Init UDP Transmitter");           					
					mgrs[i].initialize(localAddr);					    
                }                                                
  			    InetAddress remoteIPAddress = InetAddress.getByName(sessionDescription.getAddress());
                SessionAddress remoteDestinationAddressAndPort = new SessionAddress(remoteIPAddress, destPort);
                // You can try out some other buffer size to see
                // if you can get better smoothness.
                BufferControl bc = (BufferControl) mgrs[i].getControl(
                        "javax.media.control.BufferControl");
                if (bc != null){
                        if(i==0){
                                bc.setBufferLength(0);
                                //bc.setMinimumThreshold(0);
                                System.out.println("Threshold enabled : "+bc.getEnabledThreshold());
                                System.out.println("buf length : "+bc.getBufferLength());
                                System.out.println("minimum Threshold : "+bc.getMinimumThreshold());
                        }
                        else{
                                bc.setBufferLength(BufferControl.MAX_VALUE);
                                //bc.setMinimumThreshold(BufferControl.MAX_VALUE);
                                System.out.println("buf length : "+bc.getBufferLength());
                                System.out.println("minimum Threshold : "+bc.getMinimumThreshold());
                        }
                }
				if(sessionDescription.getTransportProtocol().toLowerCase().equals("udp")){					
					SessionAddress destAddr = new SessionAddress(
							InetAddress.getByName(sessionDescription.getAddress()),
							 destPort);                
					mgrs[i].addTarget(destAddr);
				}
                System.out.println("  - Open RTP session for: Address: " + sessionDescription.getAddress() +
                                                   " localPort: " + localPort + 
                                                   " destPort : " + destPort +                                                  
                                                   " Time To Live: " + sessionDescription.getTimeToLive());
                }
            }
        } catch (Exception e){
            System.err.println("Cannot create the RTP Session: ");
			e.printStackTrace();
            return false;
        }

        // Wait for data to arrive before moving on.
        /*long then = System.currentTimeMillis();
        long waitingPeriod = 30000;  // wait for a maximum of 30 secs.

        try{
            synchronized (dataSync) {
                while (!dataReceived && System.currentTimeMillis() - then < waitingPeriod) {
                    if (!dataReceived)
                        System.err.println("  - Waiting for RTP data to arrive...");
                    dataSync.wait(1000);
                }
            }
        } catch (Exception e) {}

        if (!dataReceived) {
            System.err.println("No RTP data was received.");
            close();
            return false;
        }*/
        System.err.println("  - Waiting for RTP data to arrive...");
        return true;
    }


    /**
     * Close the players and the session managers.
     */
    protected void close() {

        for (int i = 0; i < playerWindows.size(); i++) {
            try {
                ((PlayerWindow)playerWindows.elementAt(i)).close();
            } catch (Exception e) {}
        }

        playerWindows.removeAllElements();

        //close the RTP session.
        for(int i=0;i<mgrs.length;i++){
            if (mgrs[i] != null) {
                mgrs[i].removeTargets( "Closing session from Receiver");
                mgrs[i].dispose();
                mgrs[i] = null;
            }
        }
    }


    PlayerWindow find(Player p) {
        for (int i = 0; i < playerWindows.size(); i++) {
            PlayerWindow pw = (PlayerWindow)playerWindows.elementAt(i);
            if (pw.player == p)
                return pw;
        }
        return null;
    }


    PlayerWindow find(ReceiveStream strm) {
        for (int i = 0; i < playerWindows.size(); i++) {
            PlayerWindow pw = (PlayerWindow)playerWindows.elementAt(i);
            if (pw.stream == strm)
                return pw;
        }
        return null;
    }


    /**
     * SessionListener.
     */
    public synchronized void update(SessionEvent evt) {
        if (evt instanceof NewParticipantEvent) {
            Participant p = ((NewParticipantEvent)evt).getParticipant();
            System.err.println("  - A new participant had just joined: " + p.getCNAME());
        }
    }


    /**
     * ReceiveStreamListener
     */
    public synchronized void update( ReceiveStreamEvent evt) {

        RTPManager mgr = (RTPManager)evt.getSource();
        Participant participant = evt.getParticipant();	// could be null.
        ReceiveStream stream = evt.getReceiveStream();  // could be null.
               
        if (evt instanceof RemotePayloadChangeEvent) {

            System.err.println("  - Received an RTP PayloadChangeEvent.");
            System.err.println("Sorry, cannot handle payload change.");
            //System.exit(0);

        } 
        if (evt instanceof ApplicationEvent) {
            System.out.println("applciation event New stream received!!!!!!!!!");
        }
        else if (evt instanceof NewReceiveStreamEvent) {

            try {                
                //Pull out the stream 
                stream = ((NewReceiveStreamEvent)evt).getReceiveStream();
                DataSource ds = stream.getDataSource();

                // Find out the formats.   
                Object[] format=mgr.getControls();
                System.out.println(format.length);
                RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");                
                if (ctl != null){
                    System.err.println("  - Received new RTP stream: " + ctl.getFormat());                                         
                    //ctl.addFormat(new AudioFormat(AudioFormat.MPEG_RTP,48000,16,1),22);
                    ctl.addFormat(new AudioFormat(AudioFormat.MPEG_RTP,48000,16,1),14);
                    Format[] formats=ctl.getFormatList();
                    System.out.println("format list : "+formats.length);
                } else
                    System.err.println("  - Received new RTP stream");

                if (participant == null)
                    System.err.println("      The sender of this stream had yet to be identified.");
                else {
                    System.err.println("      The stream comes from: " + participant.getCNAME());
                }
                // create a player by passing datasource to the Media Manager
                Player p = javax.media.Manager.createPlayer(ds);
                if (p == null)
                    return;
				
                p.addControllerListener(this);
                p.realize();
                Control[] cs=p.getControls();
                for(int i=0;i<cs.length;i++)
                    if(cs[i] instanceof FrameRateControl)
                        System.out.println("oooooouuuuuuuuuuuuuuuuuuuuuiiiiiiiiiiiiiiii");
                if(ctl.getFormat() instanceof VideoFormat){               
                                PlayerWindow pw = new PlayerWindow(p, stream);
                                playerWindows.addElement(pw);

                }
                // Notify intialize() that a new stream had arrived.
                /*synchronized (dataSync) {
                    dataReceived = true;
                    dataSync.notifyAll();
                }*/

            } catch (Exception e) {
                System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
                return;
            }

        }

        else if (evt instanceof StreamMappedEvent) {

             if (stream != null && stream.getDataSource() != null) {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
                System.err.println("  - The previously unidentified stream ");
                if (ctl != null){
                    Format format=ctl.getFormat();
                    System.err.println("      " + format);
                    //if(format.getEncoding().indexOf("mpegaudio"))
                    //MpegAudioControl mpegControl=(MpegAudioControl)mgr.getControl("javax.media.control.MpegAudioControl");
                    //System.out.println("mpegControl::::"+mpegControl);
                }
                System.err.println("      had now been identified as sent by: " + participant.getCNAME());
             }
        }

        else if (evt instanceof ByeEvent) {

             System.err.println("  - Got \"bye\" from: " + participant.getCNAME());
             PlayerWindow pw = find(stream);
             if (pw != null) {
                pw.close();
                playerWindows.removeElement(pw);
				bye=true;
             }                         
        }        

    }
    public void stop(){
    	if(mgrs==null)
    		return;
        // close the RTP session.
        for (int i = 0; i < mgrs.length; i++) {
            if (mgrs[i] != null) {        
                mgrs[i].removeTargets("Closing session");
                mgrs[i].dispose();
                mgrs[i] = null;
            }
        }
    }
    
    /**
     * ControllerListener for the Players.
     */
    public synchronized void controllerUpdate(ControllerEvent ce) {

        Player p = (Player)ce.getSourceController();
        MpegAudioControl mpegControl=(MpegAudioControl)p.getControl("javax.media.control.MpegAudioControl");
        System.out.println("mpegControl::::"+mpegControl);
        if (p == null)
            return;

        // Get this when the internal players are realized.
        if (ce instanceof RealizeCompleteEvent) {
            PlayerWindow pw = find(p);
            if (pw != null) {
                pw.initialize();
                pw.setVisible(true);
            }
            p.start();
        }

        if (ce instanceof ControllerErrorEvent) {
            p.removeControllerListener(this);
            PlayerWindow pw = find(p);
            if (pw != null) {
                pw.close();
                playerWindows.removeElement(pw);
            }
            System.err.println("Receiver internal error: " + ce);
        }
        if (ce instanceof StartEvent) {
            GainControl gc=p.getGainControl();
            System.out.println("Class for gain contol"+gc);
            if(gc!=null){
                Component c=gc.getControlComponent();
                System.out.println("Class for component"+c);
            }
        }

    }

    public void receive(String localIpAddress){
        if (!this.initialize(localIpAddress)) {
            System.err.println("Failed to initialize the sessions.");           
        }

        // Check to see if a bye to end the RTPSession was received.
        /*try {
            while (!bye)
                Thread.sleep(1000);
        } 
        catch (Exception e) {
                e.printStackTrace();
        }

        System.err.println("Exiting Receiver");*/
    }
    
    public Socket getSocketRTPReceiver(){
        return socketRTPReceiver;
    }
    
    public Socket getSocketRTCPReceiver(){
        return socketRTCPReceiver;
    }

    public static void main(String argv[]) {
        Receiver receive=null;
        if (argv.length == 0)
            prUsage();
        try{
                receive = new Receiver(argv[0]);
        }
        catch(IllegalArgumentException iae){
                prUsage();
        }
        if (!receive.initialize("127.0.0.1")) {
            System.err.println("Failed to initialize the sessions.");
            System.exit(-1);
        }

        // Check to see if Receiver is done.
        try {
            //while (!avReceive.isDone())
            while (!bye)
                Thread.sleep(1000);
        } 
        catch (Exception e) {
                e.printStackTrace();
        }

        System.err.println("Exiting Receiver");
    }

    static void prUsage() {
        System.err.println("Usage: Receiver <session> <session> ...");
        System.err.println("     <session>: <address>/<destinationPort>/<localPort>/<ttl>");
        System.exit(0);
    }

}// end of Receiver