/*
 * MessageListener.java
 *
 * Created on November 17, 2003, 5:51 PM
 */

package gov.nist.applet.phone.ua;

import gov.nist.applet.phone.ua.gui.NISTMessengerGUI;
import gov.nist.applet.phone.ua.router.MessengerHop;

import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.AddressFactory;
import javax.sip.header.CSeqHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * Manager of all incoming messages
 * This is the sip listener
 * 
 * @author  DERUELLE Jean
 */
public class MessageListener implements javax.sip.SipListener {
	/**
	 * The SipFactory instance used to create the SipStack and the Address
	 * Message and Header Factories.
	 */
	public static SipFactory sipFactory;

	/**
	 * The AddressFactory used to create URLs ans Address objects.
	 */
	public static AddressFactory addressFactory;

	/**
	 * The HeaderFactory used to create SIP message headers.
	 */
	public static HeaderFactory headerFactory;

	/**
	 * The Message Factory used to create SIP messages.
	 */
	public static MessageFactory messageFactory;

	/**
	 * The sipStack instance that handles SIP communications.
	 */
	public SipStack sipStack;

	/**
	 * The sipProvider instance that handles sending messages statelessly.
	 */
	public SipProvider sipProvider;

	/**
	 * The sipProvider instance that handles sending messages statelessly.
	 */
	protected MessageProcessor messageProcessor;

	/**
	 * manager for the status of the user agent and its calls.
	 */
	public MessengerManager sipMeetingManager;

	/**
	 * Configuration Stack of this user agent.
	 */
	private Configuration configuration;

	/** Applet handle to signal errors.
	 */
	private NISTMessengerGUI appletHandle;

	//Instanciation of the static members
	static {
		try {
			sipFactory = SipFactory.getInstance();
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
		} catch (PeerUnavailableException pue) {
			pue.printStackTrace();
		}
	}

	/** Creates a new instance of MessageListener 
	 * It takes in parameter the callManager.
	 * @param sipMeetingManager - manager for the status of the user agent and its calls.
	 * @param configuration
	 * It will be notified of every changes in the status of the call,     
	 */
	public MessageListener(
		MessengerManager sipMeetingManager,
		Configuration configuration,
		NISTMessengerGUI appletHandle) {
		this.sipMeetingManager = sipMeetingManager;
		this.configuration = configuration;
		this.appletHandle = appletHandle;
	}

	/********************************************************************************/
	/**************************                            **************************/
	/**************************    SIP LISTENER METHODS    **************************/
	/**************************                            **************************/
	/********************************************************************************/

	/**
	 * @see javax.sip.Listener#processRequest(RequestEvent requestEvent)
	 */
	public void processRequest(RequestEvent requestEvent) {
		ServerTransaction serverTransaction =
			requestEvent.getServerTransaction();
		Request request = requestEvent.getRequest();
		if (!request.getMethod().equals(Request.MESSAGE))
			System.out.println("received request : " + request);
		else
			System.out.println("received request : " + request.getMethod());
		CSeqHeader cSeqHeader = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
		String method = cSeqHeader.getMethod();
		if (serverTransaction == null) {
			try {
				serverTransaction =
					sipProvider.getNewServerTransaction(request);
			} catch (TransactionAlreadyExistsException ex) {

				return;
			} catch (TransactionUnavailableException ex) {

				return;
			}
		}
		Dialog dialog = serverTransaction.getDialog();
		//Request requestClone = (Request) request.clone();
		//INVITE
		if (request.getMethod().equals(Request.INVITE)) {
			if (serverTransaction.getDialog().getState() == null) {
				messageProcessor.processInvite(serverTransaction, request);
			} else {
				System.out.println(
					"This message is a retransmission we dropped it : "
						+ request.toString());
			}
		}
		//ACK
		else if (request.getMethod().equals(Request.ACK)) {
			if (serverTransaction != null
				&& serverTransaction
					.getDialog()
					.getFirstTransaction()
					.getRequest()
					.getMethod()
					.equals(Request.INVITE)) {
				messageProcessor.processAck(serverTransaction, request);
			} else {
				// just ignore
				System.out.println("ignoring ack");
			}
		}
		//BYE
		else if (request.getMethod().equals(Request.BYE)) {
			if (dialog
				.getFirstTransaction()
				.getRequest()
				.getMethod()
				.equals(Request.INVITE)) {
				messageProcessor.processBye(serverTransaction, request);
			}
		}
		//CANCEL
		else if (request.getMethod().equals(Request.CANCEL)) {
			if (dialog
				.getFirstTransaction()
				.getRequest()
				.getMethod()
				.equals(Request.INVITE)) {
				messageProcessor.processCancel(serverTransaction, request);
			}
		}
		//MESSAGE
		else if (request.getMethod().equals(Request.MESSAGE)) {
			messageProcessor.processMessage(serverTransaction, request);
		}
		//SUBSCRIBE
		else if (request.getMethod().equals(Request.SUBSCRIBE)) {
			messageProcessor.processSubscribe(serverTransaction, request);
		}
		//NOTIFY
		else if (request.getMethod().equals(Request.NOTIFY)) {
			messageProcessor.processNotify(serverTransaction, request);
		}
	}

	/**
	 * @see javax.sip.Listener#processTimeout(TimeoutEvent timeoutEvent)
	 */
	public void processTimeout(TimeoutEvent timeoutEvent) {
		javax.sip.Transaction transaction;
		if (timeoutEvent.isServerTransaction()) {
		    transaction = timeoutEvent.getServerTransaction();
		}
		else {
		    transaction = timeoutEvent.getClientTransaction();
		}
		Request request = transaction.getRequest();
		request.removeContent();
		Request newRequest = (Request) request.clone();
		newRequest.removeContent();
		
		System.out.println("Timeout event received on this request : " + newRequest  );
		if((request.getMethod().equals(Request.MESSAGE))) {
			// messageProcessor.processTimedOutMessage(request);            
		} else if (request.getMethod().equals(Request.REGISTER)) {
			messageProcessor.processTimedOutRegister(request);       
		} else if (request.getMethod().equals(Request.INVITE)) {
			messageProcessor.processTimedOutInvite(request);       
		} else {            
		        System.out.println("TimeOut received,"+ newRequest);
		 }
	}

	/**
	 * @see javax.sip.Listener#processResponse(ResponseEvent responseEvent)
	 */
	public void processResponse(ResponseEvent responseEvent) {
		ClientTransaction clientTransaction =
			responseEvent.getClientTransaction();
		Response response = responseEvent.getResponse();
		System.out.println("received response : " + response);
		if (clientTransaction == null) {
			System.out.println("ignoring a transactionless response");
			return;
		}
		String method =
			((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();
		//Response responseClone = (Response) response.clone();
		//OK
		if (response.getStatusCode() == Response.OK) {
			//REGISTER
			if (method.equals(Request.REGISTER)) {
				messageProcessor.processRegisterOK(clientTransaction, response);
			} //INVITE
			else if (method.equals(Request.INVITE)) {
				messageProcessor.processInviteOK(clientTransaction, response);
			} //BYE
			else if (method.equals(Request.BYE)) {
				messageProcessor.processByeOK(clientTransaction, response);
			} //CANCEL
			else if (method.equals(Request.CANCEL)) {
				messageProcessor.processCancelOK(clientTransaction, response);
			} //MESSAGE
			else if (method.equals(Request.MESSAGE)) {
				messageProcessor.processMessageOK(clientTransaction, response);
			}
			//SUBSCRIBE
			else if (method.equals(Request.SUBSCRIBE)) {
				messageProcessor.processSubscribeOK(
					clientTransaction,
					response);
			}
		}
		//TRYING
		else if (response.getStatusCode() == Response.TRYING) {
			if (method.equals(Request.INVITE)) {
				messageProcessor.processTrying(clientTransaction, response);
			}
			//We could also receive a TRYING response to a REGISTER req            
			else if (method.equals(Request.REGISTER)) {
				//do nothing
			}
		}
		//RINGING
		else if (response.getStatusCode() == Response.RINGING) {
			if (method.equals(Request.INVITE)) {
				messageProcessor.processRinging(clientTransaction, response);
			}
		}
		//SUBSCRIBE ACCEPTED
		else if (response.getStatusCode() == Response.ACCEPTED) {
			if (method.equals(Request.SUBSCRIBE)) {
				messageProcessor.processSubscribeAccepted(
					clientTransaction,
					response);
			}
		}
		//NOT_FOUND
		else if (response.getStatusCode() == Response.NOT_FOUND) {
			if (method.equals(Request.INVITE)) {
				messageProcessor.processNotFound(clientTransaction, response);
			}
		}
		//NOT_IMPLEMENTED
		else if (response.getStatusCode() == Response.NOT_IMPLEMENTED) {
			if (method.equals(Request.INVITE)) {
				messageProcessor.processNotImplemented(
					clientTransaction,
					response);
			} else if (method.equals(Request.REGISTER)) {
				messageProcessor.processNotImplemented(
					clientTransaction,
					response);
			} else {
				System.out.println("Unknow message received : " + response);
			}
		}
		//REQUEST_TERMINATED
		else if (response.getStatusCode() == Response.REQUEST_TERMINATED) {
			messageProcessor.processRequestTerminated(
				clientTransaction,
				response);
		}
		//BUSY_HERE
		else if (response.getStatusCode() == Response.BUSY_HERE) {
			if (method.equals(Request.INVITE)) {
				messageProcessor.processBusyHere(clientTransaction, response);
			}
		}
		//TEMPORARY_UNAVAILABLE
		else if (
			response.getStatusCode() == Response.TEMPORARILY_UNAVAILABLE) {
			if (method.equals(Request.INVITE)
				|| method.equals(Request.MESSAGE)) {
				messageProcessor.processUnavailable(clientTransaction,
					response);
			}
		}
		//407 PROXY_AUTHENTICATION_REQUIRED & 401 UNAUTHORIZED
		else if (
			response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED ||
            response.getStatusCode() == Response.UNAUTHORIZED) {
			if (method.equals(Request.REGISTER) ||
                    method.equals(Request.BYE) ||
                    method.equals(Request.INVITE) ||
                    method.equals(Request.OPTIONS) ) {
				sipMeetingManager.registerStatus.setRegisterTransaction(
					clientTransaction);
				sipMeetingManager.registerStatus.setRegisterResponse(response);
				messageProcessor.processProxyAuthenticationRequired(
					clientTransaction,
					response);
			}
		}
		//401 UNAUTHORIZED
		/*else if (
			response.getStatusCode() == Response.UNAUTHORIZED
				|| response.getStatusCode()
					== Response.PROXY_AUTHENTICATION_REQUIRED) {
			if (method.equals(Request.REGISTER)) {
				sipMeetingManager.registerStatus.setRegisterTransaction(
					clientTransaction);
				sipMeetingManager.registerStatus.setRegisterResponse(response);
				messageProcessor.processProxyAuthenticationRequired(
					clientTransaction,
					response);
			}
		}*/
		//405 METHOD NOT ALLOWED
		else if (response.getStatusCode() == Response.METHOD_NOT_ALLOWED) {
			messageProcessor.processMethodNotAllowed(
				clientTransaction,
				response);
		} 
		//503 service unavailable.
		else if (response.getStatusCode() == Response.SERVICE_UNAVAILABLE) {
			messageProcessor.processUnavailable(clientTransaction, response)  ;
		}
	}

	/**
	 * Starts the stack and so the user agent
	 */
	public void start() {
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		properties.setProperty(
			"javax.sip.IP_ADDRESS",
			configuration.stackIPAddress);
		properties.setProperty(
			"javax.sip.RETRANSMISSION_FILTER",
			configuration.retransmissionFilter);
		properties.setProperty(
			"gov.nist.javax.sip.LOG_MESSAGE_CONTENT",
			"false");
		/*properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL","32");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "./debug/debug_log.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG","./debug/server_log.txt");
		properties.setProperty("gov.nist.javax.sip.BAD_MESSAGE_LOG","./debug/bad_message_log.txt");*/
		properties.setProperty("javax.sip.STACK_NAME", configuration.stackName);
		properties.setProperty(
			"javax.sip.ROUTER_PATH",
			"gov.nist.applet.phone.ua.router.MessengerRouter");
		properties.setProperty(
			"javax.sip.OUTBOUND_PROXY",
			configuration.outboundProxy
				+ ":"
				+ configuration.proxyPort
				+ "/"
				+ configuration.signalingTransport);

		// Create a dialog when message comes in.
		properties.setProperty("javax.sip.EXTENSION_METHODS","MESSAGE");

		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
		} catch (PeerUnavailableException e) {
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			if (e.getCause() != null)
				e.getCause().printStackTrace();
			appletHandle.fatalError(
				"Error creating the communication stack. \n"
					+ "Could not find communication stack!");
		}

		try {
			ListeningPoint lp =
				sipStack.createListeningPoint(
					configuration.listeningPort,
					configuration.signalingTransport);
			sipProvider = sipStack.createSipProvider(lp);
			sipProvider.addSipListener(this);
			messageProcessor = new MessageProcessor(this);

		} catch (Exception ex) {
			ex.printStackTrace();
			appletHandle.fatalError(
				"Error creating the communication stack. \n"
					+ "Only one instance of this applet is allowed!");
		}
	}

	/**
	 * Return the configuration associated to this sip listener
	 * @return the configuration associated to this sip listener
	 */
	public Configuration getConfiguration() {
		return configuration;
	}
	/**
	 * Reset the outbound proxy with the new values
	 */
	public void resetOutBoundProxy(){
		MessengerHop messengerHop=(MessengerHop)sipStack.getRouter().getOutboundProxy();
		messengerHop.setHost(configuration.outboundProxy);
		messengerHop.setPort(configuration.proxyPort);
		messengerHop.setTransport(configuration.signalingTransport);
	}

}
