/*
 * MessengerManager.java
 *
 * Created on November 25, 2003, 9:14 AM
 */

package gov.nist.applet.phone.ua;

import javax.sip.TransactionUnavailableException;
import javax.sip.InvalidArgumentException;
import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.ListeningPoint;
import javax.sip.header.CallInfoHeader;
import javax.sip.header.Header;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.sip.header.AcceptHeader;

import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.Version;
import javax.sdp.Origin;
import javax.sdp.Time;
import javax.sdp.Connection;
import javax.sdp.SessionName;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import gov.nist.applet.phone.media.MediaManager;
import gov.nist.applet.phone.media.messaging.VoiceRecorder;
import gov.nist.applet.phone.ua.call.AudioCall;
import gov.nist.applet.phone.ua.call.Call;
import gov.nist.applet.phone.ua.call.CallManager;
import gov.nist.applet.phone.ua.call.IMCall;
import gov.nist.applet.phone.ua.pidf.parser.AddressTag;
import gov.nist.applet.phone.ua.pidf.parser.AtomTag;
import gov.nist.applet.phone.ua.pidf.parser.MSNSubStatusTag;
import gov.nist.applet.phone.ua.pidf.parser.PresenceTag;
import gov.nist.applet.phone.ua.presence.PresentityManager;
import gov.nist.applet.phone.ua.presence.Subscriber;

import gov.nist.applet.phone.ua.gui.NISTMessengerGUI;

/**
 * This class will manage all the calls and their status, and the subscriptions
 * to the presence.
 * This application has been designed in following the MVC design pattern.
 * Thus this class is part of the Model.
 *
 * @author  DERUELLE Jean
 */

public class MessengerManager extends java.util.Observable {
    private MessageListener messageListener = null;
    protected CallManager callManager = null;
    private PresentityManager presentityManager = null;
    protected RegisterStatus registerStatus = null;
    private SipURI userSipURI = null;
    private Vector contactList = null;
    protected boolean presenceAllowed = false;
    private NISTMessengerGUI appletHandle;
    protected boolean reRegisterFlag = false;
    
    /** Creates a new instance of CallManager
     * @param sipURI - the sipURI of the user
     */
    public MessengerManager(
    Configuration configuration,
    NISTMessengerGUI appletHandle) {
        MediaManager.detectSupportedCodecs();
        contactList = new Vector();
        callManager = new CallManager();
        //Create a new instance of the sip Listener
        messageListener =
        new MessageListener(this, configuration, appletHandle);
        messageListener.start();
        
        presentityManager = new PresentityManager(this);
        //Set the registration status to Not registered
        registerStatus = new RegisterStatus();
        String userURI = messageListener.getConfiguration().userURI;
        try {
            //Create the SIP URI for the user URI
            String user = userURI.substring(0, userURI.indexOf("@"));
            String host =
            userURI.substring(userURI.indexOf("@") + 1, userURI.length());
            userSipURI =
            MessageListener.addressFactory.createSipURI(user, host);
            //userSipURI .setTransportParam(messageListener.getConfiguration().signalingTransport);
        } catch (ParseException ex) {
            System.out.println(userURI + " is not a legal SIP uri! " + ex);
        }
    }
    
    /**
     * Call the user specified in parameter
     * @param contactURI - the user to call
     */
    public void call(String contactURI) {
        if(!callManager.isAlreadyInAudioCall()){
            //Create a new Call
            AudioCall call = new AudioCall(messageListener);
            //Store the callee in the call
            contactURI = contactURI.trim();
            call.setCallee(contactURI);
            if (messageListener
            .getConfiguration()
            .mediaTransport
            .equalsIgnoreCase("tcp"))
                call.setVoiceMesaging(true);
            callManager.addAudioCall(call);
            String sdpBody = null;
            if (!call.getVoiceMessaging())
                sdpBody = createSDPBody(call.getMediaManager().getAudioPort());
            sendInvite(contactURI, sdpBody);
        }
    }
    
    /**
     * Answer the call, i.e. answer ok to an incoming invite
     * after have found the good media session
     */
    public void answerCall(String caller) {
        //Find the Audio call
        AudioCall call = callManager.findAudioCall(caller);
        //Get the request
        Request request = call.getDialog().getFirstTransaction().getRequest();
        //Getting the media Manager for this call
        MediaManager mediaManager = call.getMediaManager();
        //Getting the sdp body for creating the response
        //This sdp body will present what codec has been chosen
        //and on which port every media will be received
        Object responseSdpBody = null;
        if (!call.getVoiceMessaging()) {
            ContentTypeHeader contentTypeHeader =
            (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
            String contentType = contentTypeHeader.getContentType();
            String subType = contentTypeHeader.getContentSubType();
            System.out.println("the other end invite us to " + subType);
            
            if (contentType.equals("application") && subType.equals("sdp")) {
                //Get the Sdp Body
                String content = new String(request.getRawContent());
                responseSdpBody = mediaManager.getResponseSdpBody(content);
            }
        }
        sendOK(responseSdpBody, caller);
    }
    
    /**
     * Cancel the current call
     */
    public void cancelCall(String calleeURI) {
        //Find the Audio call
        AudioCall call = callManager.findAudioCall(calleeURI);
        Request request = call.getDialog().getFirstTransaction().getRequest();
        if (call.getDialog().isServer()) {
            System.out.println("Cannot cancel a server transaction");
            
        }
        ClientTransaction clientTransaction =
        (ClientTransaction) call.getDialog().getFirstTransaction();
        try {
            if (call.getDialog().getState() == javax.sip.DialogState.EARLY ||
                call.getDialog().getState() == null) {
                Request cancel = clientTransaction.createCancel();
                ClientTransaction cancelTransaction =
                messageListener.sipProvider.getNewClientTransaction(cancel);
                cancelTransaction.sendRequest();
            } else System.out.println("Too late to cancel -- send BYE instead!");
        } catch (SipException ex) {
            System.out.println("Failed to send the CANCEL request " + ex);
        }
    }
    
    /**
     * End the current call
     */
    public void endCall(String calleeURI) {
        //Find the Audio call
        AudioCall call = callManager.findAudioCall(calleeURI);
	if (call == null) {
		System.out.println("Call not found " +  calleeURI );
		return;
	}
        //Request
        Request request = null;
        try {
            request = call.getDialog().createRequest("BYE");
        } catch (SipException ex) {
            System.out.println("Could not create the bye request! " + ex);
        }
        //Transaction
        ClientTransaction clientTransaction = null;
        try {
            clientTransaction =
            messageListener.sipProvider.getNewClientTransaction(request);
            System.out.println(request.toString());
            call.getDialog().sendRequest(clientTransaction);
            
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Could not create a register transaction!\n"
            + "Check that the Registrar address is correct!"
            + " "
            + ex);
        } catch (SipException ex) {
            System.out.println(
            "Could not send out the register request! " + ex);
        }
        if (call.getVoiceMessaging()) {
            //Stop the voice messages schedule and the voiceRecorder
            messageListener.messageProcessor.stopVoiceMessagingSchedule();
        } else {
            call.getMediaManager().stopMediaSession();
        }
    }
    
    /**
     * Add a contact to our contact list
     * @param contact - contact to add
     */
    public void addContact(String contact) {
        if (isInContactList(contact))
            System.out.println(
            "The contact is already in our contact list,"
            + "he's not going to be added");
        else
            contactList.addElement(contact);
    }
    
    /**
     * Remove a contact to our contact list
     * @param contact - contact to remove
     */
    public void removeContact(String contact) {
        if (isInContactList(contact))
            contactList.remove(contact);
        else
            System.out.println(
            "The contact is not in our contact list,"
            + "he can not going to be removed");
    }
    
    /**
     * Check if the contact is in the contact List
     * @param contactAddress - the contact
     * @return true if the contact is in the contact List
     */
    protected boolean isInContactList(String contactAddress) {
        Enumeration e = contactList.elements();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equals(contactAddress))
                return true;
        }
        return false;
    }
    
    /**
     * Stop an opened instantMessaging Session
     * @param calleeURI - sip address of the person the application is chatting with
     */
    public void stopInstantMessagingSession(String calleeURI) {
        calleeURI = calleeURI.trim();
        IMCall call = callManager.findIMCall(calleeURI);
        //If no instant messaging session exists
        if (call == null)
            return;
        Request bye = null;
        try {
            bye = call.getDialog().createRequest("BYE");
        } catch (SipException se) {
            se.printStackTrace();
        }
        //Transaction
        ClientTransaction clientTransaction = null;
        try {
            clientTransaction =
            messageListener.sipProvider.getNewClientTransaction(bye);
        } catch (TransactionUnavailableException ex) {
            System.out.println("Could not create a bye transaction!\n" + ex);
        }
        System.out.println(bye.toString());
        try {
            call.getDialog().sendRequest(clientTransaction);
        } catch (SipException ex) {
            System.out.println("Could not send out the bye request! " + ex);
        }
    }
    
    /**
     *
     * @param requestName
     * @param callee
     * @return
     */
    protected Request createRequest(
    String requestName,
    SipURI callee,
    SipURI caller) {
        //Listening Point
        ListeningPoint listeningPoint =
        messageListener.sipProvider.getListeningPoint();
        //Request URI
        SipURI requestURI = null;
        try {
            requestURI = callee;
            requestURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
            //Call ID
            CallIdHeader callIdHeader =
            messageListener.sipProvider.getNewCallId();
            //CSeq
            CSeqHeader cSeqHeader =
            MessageListener.headerFactory.createCSeqHeader(1, requestName);
            //From Header
            Address fromAddress =
            MessageListener.addressFactory.createAddress(
            MessageListener.addressFactory.createSipURI(
            caller.getUser(),
            caller.getHost()));
            FromHeader fromHeader =
            MessageListener.headerFactory.createFromHeader(
            fromAddress,
            generateTag());
            //ToHeader
            Address toAddress =
            MessageListener.addressFactory.createAddress(
            MessageListener.addressFactory.createSipURI(
            callee.getUser(),
            callee.getHost()));
            // From and To are logical identifiers (should have no parameters)
            ToHeader toHeader =
            MessageListener.headerFactory.createToHeader(toAddress, null);
            //ViaHeader
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader =
            MessageListener.headerFactory.createViaHeader(
            messageListener.getConfiguration().contactIPAddress,
            listeningPoint.getPort(),
            listeningPoint.getTransport(),
            null);
            viaHeaders.add(viaHeader);
            //Max Forward Header - just forward by one hop.
            MaxForwardsHeader maxForwardsHeader =
            MessageListener.headerFactory.createMaxForwardsHeader(2);
            Request request =
            MessageListener.messageFactory.createRequest(
            requestURI,
            requestName,
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            viaHeaders,
            maxForwardsHeader);
            //Contact Header
            SipURI contactURI =
            MessageListener.addressFactory.createSipURI(
            caller.getUser(),
            messageListener.getConfiguration().contactIPAddress);
            contactURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
            ContactHeader contactHeader =
            MessageListener.headerFactory.createContactHeader(
            MessageListener.addressFactory.createAddress(contactURI));
            contactURI.setPort(listeningPoint.getPort());
            request.addHeader(contactHeader);
            //Route Header
            
            /**
             * SipURI routeURI= MessageListener.addressFactory.createSipURI(
             * null,
             * messageListener.getConfiguration().outboundProxy);
             * routeURI.setPort(messageListener.getConfiguration().proxyPort);
             * routeURI.setTransportParam(messageListener.getConfiguration().signalingTransport);
             * RouteHeader routeHeader = MessageListener.headerFactory.createRouteHeader(
             * MessageListener.addressFactory.createAddress(routeURI));
             * request.addHeader(routeHeader);
             **/
            
            return request;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(
            "Something bad happened when creating request!!");
            System.out.println("method = " + requestName);
            System.out.println("caller = " + caller);
            System.out.println("caller = " + callee);
            return null;
        }
    }
    
    /**
     * Send A Register to the proxy with the authentication acquired from the user.
     * @param userName - the user name
     * @param password - the password's user
     * @param realm - the realm's user
     * TODO : Keep a trace of the expires and schedule a new registration
     */
    public void registerWithAuthentication(
    String userName,
    String password,
    String realm) {
        //WE start the authentication process!!!
        // Let's get the Request related to this response:
        Request request = registerStatus.getRegisterTransaction().getRequest();
        if (request == null) {
            System.out.println(
            "IMUserAgent, processResponse(), the request "
            + " that caused the 407 has not been retrieved!!! Return cancelled!");
        } else {
            Request clonedRequest = (Request) request.clone();
            // Let's increase the Cseq:
            CSeqHeader cseqHeader =
            (CSeqHeader) clonedRequest.getHeader(CSeqHeader.NAME);
            try {
                cseqHeader.setSequenceNumber(
                cseqHeader.getSequenceNumber() + 1);
            } catch (InvalidArgumentException iae) {
                iae.printStackTrace();
            }
            
            // Let's add a Proxy-Authorization header:
            // We send the informations stored:
            Header header =
            registerStatus.getHeader(
            registerStatus.getRegisterResponse(),
            userName,
            password,
            messageListener.getConfiguration().outboundProxy,
            messageListener.getConfiguration().proxyPort);
            
            if (header == null) {
                System.out.println(
                "IMUserAgent, processResponse(), Proxy-Authorization "
                + " header is null, the request is not resent");
            } else {
                clonedRequest.setHeader(header);
                ClientTransaction newClientTransaction = null;
                try {
                    newClientTransaction =
                    messageListener.sipProvider.getNewClientTransaction(
                    clonedRequest);
                } catch (TransactionUnavailableException tue) {
                    tue.printStackTrace();
                }
                
                try {
                    newClientTransaction.sendRequest();
                } catch (SipException se) {
                    se.printStackTrace();
                }
                System.out.println(
                "IMUserAgent, processResponse(), REGISTER "
                + "with credentials sent:\n"
                + clonedRequest);
                System.out.println();
            }
        }
    }
    
    /**
     * Send A Register to the proxy.
     * TODO : Keep a trace of the expires and schedule a new registration
     */
    public void register() {
        SipURI proxyURI = null;
        try {
            proxyURI =
            MessageListener.addressFactory.createSipURI(
            null,
            messageListener.getConfiguration().outboundProxy);
            proxyURI.setPort(messageListener.getConfiguration().proxyPort);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        Request request =
        createRequest(Request.REGISTER, userSipURI, userSipURI);
        // Resource to which this is directed is the proxy.
        request.setRequestURI(proxyURI);
        //Transaction
        ClientTransaction regTrans = null;
        try {
            regTrans =
            messageListener.sipProvider.getNewClientTransaction(request);
            // System.out.println(request.toString());
            regTrans.sendRequest();
            this.setRegisterStatus(RegisterStatus.REGISTRATION_IN_PROGRESS);
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Could not create a register transaction!\n"
            + "Check that the Registrar address is correct!"
            + " "
            + ex);
        } catch (SipException ex) {
            System.out.println(
            "Could not send out the register request! " + ex);
            ex.printStackTrace();
        }
        
    }
    
    public void unRegisterAndReRegister() {
        this.reRegisterFlag = true;
        this.deRegister();
    }
    
    public void unRegister() {
        this.reRegisterFlag = false;
        this.deRegister();
    }
    /**
     * Stop the registration
     */
    private void deRegister() {
        SipURI proxyURI = null;
        try {
            proxyURI =
            MessageListener.addressFactory.createSipURI(
            null,
            messageListener.getConfiguration().outboundProxy);
            proxyURI.setPort(messageListener.getConfiguration().proxyPort);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        Request request =
        createRequest(Request.REGISTER, userSipURI, userSipURI);
        // Direct the request towards the proxy.
        request.setRequestURI(proxyURI);
        //ExpiresHeader
        try {
            ExpiresHeader expires =
            MessageListener.headerFactory.createExpiresHeader(0);
            request.setHeader(expires);
        } catch (InvalidArgumentException iae) {
            iae.printStackTrace();
        }
        ContactHeader contactHeader =
        MessageListener.headerFactory.createContactHeader();
        request.setHeader(contactHeader);
        //Transaction
        ClientTransaction regTrans = null;
        try {
            regTrans =
            messageListener.sipProvider.getNewClientTransaction(request);
            System.out.println(request.toString());
            regTrans.sendRequest();
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Could not create a un-register transaction!\n"
            + "Check that the Registrar address is correct!"
            + " "
            + ex);
        } catch (SipException ex) {
            System.out.println(
            "Could not send out the un-register request! " + ex);
            ex.printStackTrace();
        }
    }
    
    /**
     * Create the SDP body of the INVITE message
     * for the initiation of the media session
     */
    private String createSDPBody(int audioPort) {
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();
            SessionDescription sessionDescription =
            sdpFactory.createSessionDescription();
            //Protocol version
            Version version = sdpFactory.createVersion(0);
            sessionDescription.setVersion(version);
            //Owner
            long sdpSessionId=(long)(Math.random() * 1000000);
            Origin origin =
            sdpFactory.createOrigin(
            userSipURI.getUser(),
            sdpSessionId,
            sdpSessionId+1369,
            "IN",
            "IP4",
            messageListener.getConfiguration().contactIPAddress);
            sessionDescription.setOrigin(origin);
            //Session Name
            SessionName sessionName = sdpFactory.createSessionName("-");
            sessionDescription.setSessionName(sessionName);
            //Connection
            Connection connection =
            sdpFactory.createConnection(
            messageListener.getConfiguration().contactIPAddress);
            sessionDescription.setConnection(connection);
            //Time
            Time time = sdpFactory.createTime();
            Vector timeDescriptions = new Vector();
            timeDescriptions.add(time);
            sessionDescription.setTimeDescriptions(timeDescriptions);
            //Media Description
            MediaDescription mediaDescription =
            sdpFactory.createMediaDescription(
            "audio",
            audioPort,
            1,
            "RTP/AVP",
            MediaManager.getSdpAudioSupportedCodecs());
            Vector mediaDescriptions = new Vector();
            mediaDescriptions.add(mediaDescription);
            sessionDescription.setMediaDescriptions(mediaDescriptions);
            return sessionDescription.toString();
        } catch (SdpException se) {
            se.printStackTrace();
        }
        return null;
    }
    
    /**
     * Send an Invite to the sip URI in parameter
     * @param calleeURI - the URI of the user to call
     * @param sdpBody - the sdp content describing the media session
     * to include to the message
     */
    private void sendInvite(String calleeURI, String sdpBody) {
        if (calleeURI.indexOf("sip:") != -1)
            calleeURI = calleeURI.substring("sip:".length());
        //Request URI
        SipURI contactURI = null;
        try {
            //Create the SIP URI for the user URI
            String user = calleeURI.substring(0, calleeURI.indexOf("@"));
            String host =
            calleeURI.substring(
            calleeURI.indexOf("@") + 1,
            calleeURI.length());
            contactURI =
            MessageListener.addressFactory.createSipURI(user, host);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        Request invite = createRequest(Request.INVITE, contactURI, userSipURI);
        // Indicate to the other end the type of media I am willing to accept.
        try {
            if (messageListener
            .getConfiguration()
            .mediaTransport
            .equalsIgnoreCase("tcp")){
                AcceptHeader acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "audio",
                "gsm");
                invite.addHeader(acceptHeader);
                acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "audio",
                "x-gsm");
                invite.addHeader(acceptHeader);
                acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "text",
                "plain");
                invite.addHeader(acceptHeader);
            }
            //Content
            ContentTypeHeader contentTypeHeader = null;
            if (sdpBody != null) {
                contentTypeHeader =
                MessageListener.headerFactory.createContentTypeHeader(
                "application",
                "sdp");
                invite.setContent(sdpBody, contentTypeHeader);
            }
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Failed to create a content type header for the INVITE request "
            + ex);
        }
        //Transaction
        ClientTransaction inviteTransaction = null;
        try {
            inviteTransaction =
            messageListener.sipProvider.getNewClientTransaction(invite);
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Failed to create inviteTransaction.\n"
            + "This is most probably a network connection error. ");
            ex.printStackTrace();
        }
        System.out.println("send request:\n" + invite);
        try {
            inviteTransaction.sendRequest();
            //Find the Audio call
            AudioCall call = callManager.findAudioCall("sip:" + calleeURI);
            call.setDialog(inviteTransaction.getDialog());
        } catch (SipException ex) {
            System.out.println(
            "An error occurred while sending invite request " + ex);
        }
    }
    
    /**
     * Send a MESSAGE to notify the proxy that a mail must be send to the callee
     */
    public void sendMessage(String emailBody, String calleeURI) {
        //Store the callee in the call status
        calleeURI = calleeURI.trim();
        //call.setCallee(calleeURI);
        //Listening Point
        ListeningPoint listeningPoint =
        messageListener.sipProvider.getListeningPoint();
        SipURI requestURI = null;
        try {
            requestURI =
            MessageListener.addressFactory.createSipURI(
            null,
            messageListener.getConfiguration().outboundProxy);
            requestURI.setPort(messageListener.getConfiguration().proxyPort);
            requestURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return;
        }
        //Call ID
        CallIdHeader callIdHeader = messageListener.sipProvider.getNewCallId();
        //CSeq
        CSeqHeader cSeqHeader = null;
        try {
            cSeqHeader =
            MessageListener.headerFactory.createCSeqHeader(
            1,
            Request.MESSAGE);
        } catch (ParseException ex) {
            //Shouldn't happen
            ex.printStackTrace();
        } catch (InvalidArgumentException ex) {
            //Shouldn't happen
            System.out.println(
            "An unexpected error occurred while"
            + "constructing the CSeqHeader "
            + ex);
        }
        FromHeader fromHeader = null;
        //From
        try {
            fromHeader =
            MessageListener.headerFactory.createFromHeader(
            MessageListener.addressFactory.createAddress(userSipURI),
            generateTag());
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        //To Header
        SipURI ToURI = null;
        try {
            //Create the SIP URI for the user URI
            String user = calleeURI.substring(0, calleeURI.indexOf("@"));
            String host =
            calleeURI.substring(
            calleeURI.indexOf("@") + 1,
            calleeURI.length());
            ToURI = MessageListener.addressFactory.createSipURI(user, host);
            ToURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
        } catch (ParseException ex) {
            System.out.println(calleeURI + " is not a legal SIP uri! " + ex);
        }
        Address toAddress = MessageListener.addressFactory.createAddress(ToURI);
        ToHeader toHeader = null;
        try {
            toHeader =
            MessageListener.headerFactory.createToHeader(toAddress, null);
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Null is not an allowed tag for the to header! " + ex);
        }
        //ViaHeader
        ArrayList viaHeaders = null;
        viaHeaders = new ArrayList();
        try {
            ViaHeader viaHeader =
            MessageListener.headerFactory.createViaHeader(
            messageListener.getConfiguration().contactIPAddress,
            listeningPoint.getPort(),
            listeningPoint.getTransport(),
            null);
            viaHeaders.add(viaHeader);
        } catch (ParseException ex) {
            //Shouldn't happen
            ex.printStackTrace();
        } catch (InvalidArgumentException ex) {
            //Should never happen
            System.out.println("The application is corrupt");
        }
        //Max Forward Header
        MaxForwardsHeader maxForwardsHeader = null;
        try {
            maxForwardsHeader =
            MessageListener.headerFactory.createMaxForwardsHeader(70);
        } catch (InvalidArgumentException ex) {
            //Should never happen
            System.out.println("The application is corrupt");
        }
        Request message = null;
        try {
            message =
            MessageListener.messageFactory.createRequest(
            requestURI,
            Request.MESSAGE,
            callIdHeader,
            cSeqHeader,
            fromHeader,
            toHeader,
            viaHeaders,
            maxForwardsHeader);
        } catch (ParseException ex) {
            System.out.println("Failed to create message Request! ");
            ex.printStackTrace();
        }
        //Contact Header
        try {
            SipURI contactURI =
            MessageListener.addressFactory.createSipURI(
            userSipURI.getUser(),
            messageListener.getConfiguration().contactIPAddress);
            contactURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
            ContactHeader contactHeader =
            MessageListener.headerFactory.createContactHeader(
            MessageListener.addressFactory.createAddress(contactURI));
            contactURI.setPort(listeningPoint.getPort());
            message.addHeader(contactHeader);
        } catch (ParseException ex) {
            System.out.println("Could not create the message request! " + ex);
        }
        //Content
        ContentTypeHeader contentTypeHeader = null;
        try {
            contentTypeHeader =
            MessageListener.headerFactory.createContentTypeHeader(
            "text",
            "plain;charset=UTF-8");
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Failed to create a content type header for the MESSAGE request "
            + ex);
        }
        try {
            message.setContent(emailBody, contentTypeHeader);
        } catch (ParseException ex) {
            System.out.println(
            "Failed to parse sdp data while creating message request! "
            + ex);
        }
        //Transaction
        //Send the message with the sip Provider
        //So that he doesn't retransmit the message even if he doesn't receive
        //an OK (the drawback is that we don't know if the other end ever
        //received the message)
        //TODO : send the message in an invite transaction
        //to know more about delivery
        //ClientTransaction inviteTransaction=null;
        try {
            //inviteTransaction =
            messageListener.sipProvider.sendRequest(message);
        } catch (SipException ex) {
            System.out.println(
            "An error occurred while sending message request " + ex);
            ex.printStackTrace();
        }
        System.out.println("request sent :\n" + message);
                /*try {
                        inviteTransaction.sendRequest();
                        call.setDialog(inviteTransaction.getDialog());
                }
                catch (SipException ex) {
                        System.out.println(
                                "An error occurred while sending invite request "+ ex);
                } */
    }
    
    /**
     *
     * @param contactAddress
     * @param voiceMessage
     */
    public void sendVoiceMessage(String contactAddress, byte[] voiceMessage) {
        Request messageRequest = null;
        String contact = contactAddress.trim();
        //Try to find if we have an existing transaction for this one
        
        AudioCall call = callManager.findAudioCall(contact);
        javax.sip.Dialog dialog = null;
        
        if (call != null)  {
            dialog = call.getDialog();
	    if (dialog == null 				   || 
		dialog.getState() == null 		   || 
		dialog.getState() == javax.sip.DialogState.COMPLETED ||
		dialog.getState() == javax.sip.DialogState.TERMINATED)  {
		System.out.println("Cannot send message on terminated or un-established dialog!");
		// stop the audio stuff 
                messageListener.messageProcessor.stopVoiceMessagingSchedule();
		callManager.removeAudioCall(call);
		return;
	    }
            
            try {
                messageRequest = dialog.createRequest(Request.MESSAGE);
                System.out.println(
                "SEND VOICE MESSAGE: " + messageRequest.toString());
            } catch (SipException se) {
                se.printStackTrace();
                return;
            }
            
        } else {
            return;
        }
        
        
        System.out.println("voice messaging session found!!");
        //Content
        ContentTypeHeader contentTypeHeader = null;
        try {
            contentTypeHeader =
            MessageListener.headerFactory.createContentTypeHeader(
            "audio",
            "x-gsm");
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Failed to create a content type header for the MESSAGE request "
            + ex);
        }


        try {
            messageRequest.setContent(voiceMessage, contentTypeHeader);
        } catch (ParseException ex) {
            System.out.println(
            "Failed to parse sdp data while creating message request! "
            + ex);
        }
        try {
            ListeningPoint listeningPoint =
            messageListener.sipProvider.getListeningPoint();
            SipURI contactURI =
            MessageListener.addressFactory.createSipURI(
            userSipURI.getUser(),
            messageListener.getConfiguration().contactIPAddress);
            contactURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
            ContactHeader contactHeader =
            MessageListener.headerFactory.createContactHeader(
            MessageListener.addressFactory.createAddress(
            contactURI));
            contactURI.setPort(listeningPoint.getPort());
            messageRequest.addHeader(contactHeader);
            SipURI routeURI =
            MessageListener.addressFactory.createSipURI(
            null,
            messageListener.getConfiguration().outboundProxy);
            
        } catch (ParseException ex) {
            System.out.println(
            "Could not create the message request! " + ex);
        }
        //Transaction
        ClientTransaction messageTransaction = null;
        
        try {
            messageTransaction =
            messageListener.sipProvider.getNewClientTransaction(
            messageRequest);
            dialog.sendRequest(messageTransaction);
        } catch (Exception ex) {
            System.out.println(
            "Failed to create Message Transaction.\n"
            + "This is most probably a network connection error. ");
            ex.printStackTrace();
            return;
        }
        
        
    }
    
    /**
     * Send a MESSAGE to the contact we want to chat with
     * @param contactAddress - the contact to send the message to
     * @parma message - message to be sent
     */
    public void sendInstantMessage(String contactAddress, String message) {
        Request messageRequest = null;
       
        //Try to find if we have an existing transaction for this one
        SipURI callee = null;
        try {
              callee = (SipURI) MessageListener.addressFactory.createURI(contactAddress);
        } catch (ParseException ex) {
                ex.printStackTrace();
                return;
        }
        String contact = callee.getUser()+"@"+callee.getHost();
        IMCall call = callManager.findIMCall(contact);
        javax.sip.Dialog dialog = null;
        
        
        // If we have a call record for this IM session then use the dialog.
        if (call != null)  {
            dialog = call.getDialog();
            System.out.println("im session found!!");
            
            try {
                messageRequest = dialog.createRequest(Request.MESSAGE);
                System.out.println(
                "SEND VOICE MESSAGE: " + messageRequest.toString());
            } catch (SipException se) {
		// The dialog must be dead so get rid of it.
		// proceed on bravely and re-establish the dialog.
		callManager.removeIMCall(contact);
		System.out.println("dialog is dead!");
                // Signal that the dialog is dead.
                dialog = null;
            }
            
        } 


	if (messageRequest == null)  {
	    System.out.println("could not find IM session for " + contact);
            try{
                messageRequest = createRequest(Request.MESSAGE, callee, userSipURI);
                ((FromHeader)messageRequest.getHeader(FromHeader.NAME)).setTag(generateTag());
            } catch (ParseException pe) {
		// should not happen.
                pe.printStackTrace();
            }
        }

        //Content
        ContentTypeHeader contentTypeHeader = null;
        try {
            contentTypeHeader =
            MessageListener.headerFactory.createContentTypeHeader(
            "text",
            "plain;charset=UTF-8");
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Failed to create a content type header for the MESSAGE request "
            + ex);
        }
        try {
            messageRequest.setContent(message, contentTypeHeader);
        } catch (ParseException ex) {
            System.out.println(
            "Failed to parse sdp data while creating message request! "
            + ex);
        }
        //Transaction
        ClientTransaction messageTransaction = null;
        try {
            messageTransaction =
            messageListener.sipProvider.getNewClientTransaction(
            messageRequest);
            if (dialog != null )
                dialog.sendRequest(messageTransaction);
            else  {
                messageTransaction.sendRequest();
                
                IMCall imcall = new IMCall(contact);
                imcall.setDialog(messageTransaction.getDialog());
                callManager.addIMCall(imcall);
            }
            
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Failed to create message Transaction.\n"
            + "This is most probably a network connection error. ");
            ex.printStackTrace();
        } catch (SipException se) {
            se.printStackTrace();
        }
        
    }
    
    /**
     * Send a request for subscription
     * @param subscriberAddress - the address of the contact we want to
     * subscribe to
     */
    public void sendSubscribe(String subscriberAddress) {
        //Store the callee in the call status
        String calleeURI = subscriberAddress.trim();
        //Request URI
        SipURI contactURI = null;
        try {
            //Create the SIP URI for the user URI
            String user = calleeURI.substring(0, calleeURI.indexOf("@"));
            String host =
            calleeURI.substring(
            calleeURI.indexOf("@") + 1,
            calleeURI.length());
            contactURI =
            MessageListener.addressFactory.createSipURI(user, host);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        Request subscribe =
        createRequest(Request.SUBSCRIBE, contactURI, userSipURI);
        //Content
        ContentTypeHeader contentTypeHeader = null;
        try {
            contentTypeHeader =
            MessageListener.headerFactory.createContentTypeHeader(
            "application",
            "sdp");
        } catch (ParseException ex) {
            //Shouldn't happen
            System.out.println(
            "Failed to create a content type header for the SUBSCRIBE request "
            + ex);
        }
        //Transaction
        ClientTransaction subscribeTransaction = null;
        try {
            subscribeTransaction =
            messageListener.sipProvider.getNewClientTransaction(subscribe);
            System.out.println("send request:\n" + subscribe);
            subscribeTransaction.sendRequest();
            //call.setDialog(subscribeTransaction.getDialog());
        } catch (TransactionUnavailableException ex) {
            System.out.println(
            "Failed to create subscribeTransaction.\n"
            + "This is most probably a network connection error. ");
            ex.printStackTrace();
        } catch (SipException ex) {
            System.out.println(
            "An error occurred while sending subscribe request " + ex);
        }
    }
    
    /**
     * Send an OK in repsonse to an incoming invite
     * @param sdpBody - the sdpBody to include in the response
     */
    private void sendOK(Object sdpBody, String caller) {
        //Find the Audio call
        AudioCall call = callManager.findAudioCall(caller);
        //Listening Point
        ListeningPoint listeningPoint =
        messageListener.sipProvider.getListeningPoint();
        //Get the request
        Request request = call.getDialog().getFirstTransaction().getRequest();
        if (!call.getDialog().isServer())
            System.out.println("Problem, this is a client transaction");
        //Get the server Transaction
        ServerTransaction serverTransaction =
        (ServerTransaction) call.getDialog().getFirstTransaction();
        try {
            Response ok =
            (Response) MessageListener.messageFactory.createResponse(
            Response.OK,
            request);
            //Put a tag on the To Header
            ((ToHeader) ok.getHeader(ToHeader.NAME)).setTag(generateTag());
            //Specify the contact Header
            SipURI contactURI =
            MessageListener.addressFactory.createSipURI(
            userSipURI.getUser(),
            messageListener.getConfiguration().contactIPAddress);
            contactURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
            ContactHeader contactHeader =
            MessageListener.headerFactory.createContactHeader(
            MessageListener.addressFactory.createAddress(contactURI));
            contactURI.setPort(listeningPoint.getPort());
            ok.addHeader(contactHeader);
            //IF the call is voice messaging we add the Accept Headers
            if (call.getVoiceMessaging()) {
                AcceptHeader acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "audio",
                "gsm");
                ok.addHeader(acceptHeader);
                acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "audio",
                "x-gsm");
                ok.addHeader(acceptHeader);
                acceptHeader =
                MessageListener.headerFactory.createAcceptHeader(
                "application",
                "text");
                ok.addHeader(acceptHeader);
                //initialize the voiceRecorder
                VoiceRecorder.getInstance();
            } else {
                //Adding the sdp Body describing the media session
                ContentTypeHeader contentTypeHeader =
                (ContentTypeHeader) request.getHeader(
                ContentTypeHeader.NAME);
                if (contentTypeHeader != null && sdpBody != null)
                    ok.setContent(sdpBody, contentTypeHeader);
                else
                    ok.setHeader(contentTypeHeader);
            }
            //Send the ok
            serverTransaction.sendResponse(ok);
        } catch (SipException ex) {
            System.out.println("Failed to send the OK response " + ex);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Send a BUSY in response to an incoming invite
     */
    public void sendBusy(String caller) {
        //Find the Audio call
        AudioCall call = callManager.findAudioCall(caller);
        //Get the request
        Request request = call.getDialog().getFirstTransaction().getRequest();
        if (!call.getDialog().isServer())
            System.out.println("Problem, this is a client transaction");
        //Get the server Transaction
        ServerTransaction serverTransaction =
        (ServerTransaction) call.getDialog().getFirstTransaction();
        try {
            Response busy =
            (Response) MessageListener.messageFactory.createResponse(
            Response.BUSY_HERE,
            request);
            //If the user has put an URL for the BUSY we add i in the CALL-Info
            if(messageListener.getConfiguration().httpBusy!=null){
                CallInfoHeader callInfoHeader=
                MessageListener.headerFactory.createCallInfoHeader(
                MessageListener.addressFactory.createURI(
                messageListener.getConfiguration().httpBusy));
                busy.addHeader(callInfoHeader);
            }
            serverTransaction.sendResponse(busy);
            System.out.println(
            "Audio Call removed : " + call.getDialog().getDialogId());
            callManager.removeAudioCall(call);
        } catch (SipException ex) {
            System.out.println("Failed to send the BUSY response " + ex);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * The subscriber is added now and the user is notified that a new person
     * wants to be added to the contact list, if he doesn't want this person
     * to be added, the subscriber is gonna be removed from the GUI
     * @param subscriber - the subscirber to add
     */
    public void notifySubscribe(Subscriber subscriber) {
        //the subscriber is added now and the user is notified that a new person
        //wants to be added to the contact list, if he doesn't want this person
        //to be added, the subscriber is gonna be removed from the GUI
        presentityManager.addSubscriber(subscriber);
        //Notify the GUI and the controller that the status has changed
        setChanged();
        notifyObservers(subscriber);
    }
    
    /**
     * Notify the GUI and the controller that the status has changed
     * @param presenceTag - presence tag containing all the presence information
     */
    public void notifyPresence(PresenceTag presenceTag) {
        Vector atomTagList = presenceTag.getAtomTagList();
        AtomTag atomTag = (AtomTag) atomTagList.firstElement();
        AddressTag addressTag = atomTag.getAddressTag();
        MSNSubStatusTag msnSubStatusTag = addressTag.getMSNSubStatusTag();
        Subscriber subscriber =
        presentityManager.getSubscriber(presenceTag.getAddress());
        if (subscriber != null) {
            subscriber.setStatus(msnSubStatusTag.getMSNSubStatus());
        }
        //Notify the GUI and the controller that the status has changed
        setChanged();
        notifyObservers(presenceTag);
    }
    
    /**
     * Retrieve the messageListener
     * @return the messageListener
     */
    public MessageListener getMessageListener() {
        return messageListener;
    }
    
    /**
     * Set the current status of the call
     * @param callStatus - the current status of the call
     */
    public void notifyObserversNewCallStatus(Call call) {
        //Notify the GUI and the controller that the status has changed
        setChanged();
        notifyObservers(call);
    }
    
    /**
     * Retrieve the contact List
     * @return the contact List
     */
    public Vector getContactList() {
        return contactList;
    }
    
    /**
     * Retrieve the call Manager
     * @return the call Manager
     */
    public CallManager getCallManager() {
        return callManager;
    }
    
    /**
     * Retrieve the presentity manager
     * @return the presentity manager
     */
    public PresentityManager getPresentityManager() {
        return presentityManager;
    }
    
    /**
     * Retrieve the current status of the registration
     * @return the current status of the registration
     */
    public String getRegisterStatus() {
        return this.registerStatus.getStatus();
    }
    
    /**
     * Set the current status of the registration
     * @param registerStatus - the current status of the registration
     */
    public void setRegisterStatus(String registerStatus) {
        this.registerStatus.setStatus(registerStatus);
        //Notify the GUI and the controller that the status has changed
        setChanged();
        notifyObservers(this.registerStatus);
    }
    
    /**
     * Notify Observers an Instant Message has been received
     * @param message - the message received
     */
    public void notifyObserversIMReceived(String message, String sender) {
        InstantMessage im = new InstantMessage(message, sender);
        setChanged();
        notifyObservers(im);
    }
    
    /**
     * Generate a Tag
     * @return the tag generated
     */
    public static String generateTag() {
        return new Integer((int) (Math.random() * 10000)).toString();
    }
    
    /**
     * Get the current sip user URI
     * @return the current sip user URI
     */
    public SipURI getUserURI() {
        return userSipURI;
    }
    
    /**
     * Set the current sip user URI
     * @param userURI - the current sip user URI
     */
    public void setUserURI(String userURI) {
        try {
            //Create the SIP URI for the user URI
            String user = userURI.substring(0, userURI.indexOf("@"));
            String host =
            userURI.substring(userURI.indexOf("@") + 1, userURI.length());
            userSipURI =
            MessageListener.addressFactory.createSipURI(user, host);
            userSipURI.setTransportParam(
            messageListener.getConfiguration().signalingTransport);
        } catch (ParseException ex) {
            System.out.println(userURI + " is not a legal SIP uri! " + ex);
        }
    }
}



