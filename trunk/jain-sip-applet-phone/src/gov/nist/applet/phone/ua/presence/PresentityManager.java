/*
 * PresentityManager.java
 * 
 * Created on Mar 22, 2004
 *
 */
package gov.nist.applet.phone.ua.presence;

import gov.nist.applet.phone.ua.Configuration;
import gov.nist.applet.phone.ua.MessageListener;
import gov.nist.applet.phone.ua.MessengerManager;
import gov.nist.applet.phone.ua.pidf.parser.XMLpidfParser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * This class manage the presence of the application, i.e subscriptions and 
 * notifications
 * 
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class PresentityManager {
	private Hashtable subscriberList = null;
	private MessengerManager sipMeetingManager=null;
	private MessageListener messageListener=null;
	private Configuration configuration=null;
	/**
	 * 
	 */
	public PresentityManager(MessengerManager sipMeetingManager) {
		subscriberList=new Hashtable();	
		this.sipMeetingManager=sipMeetingManager;
		this.messageListener=sipMeetingManager.getMessageListener();
		this.configuration=this.messageListener.getConfiguration();
	}

	/**
	 * Retrieve a subscriber by his address
	 * @param subscriberAddress - the subscriber address
	 * @return the subscriber
	 */
	public Subscriber getSubscriber(String subscriberAddress){
		return (Subscriber)subscriberList.get(subscriberAddress);
	}
	
	/**
	 * Add a subscriber to our subscriber list
	 * @param subscriber - subscriber to add
	 */
	public void addSubscriber(Subscriber subscriber){
		Subscriber subscriberFromList=
			(Subscriber)subscriberList.get(subscriber.getAddress());
		if(subscriberFromList==null)
			subscriberList.put(subscriber.getAddress(),subscriber);
		else{
			System.out.println("Already in list put the dialog to"+
				subscriber.getDialog());
			subscriberFromList.setDialog(subscriber.getDialog());			
		}
		System.out.println(subscriber.getAddress()+" has been added to your contacts.");
		System.out.println(subscriber.getDialog());
	}

	/**
	 * Remove a subscriber to our subscriber list
	 * @param subscriber - subscriber to remove
	 */
	public void removeSubscriber(String subscriber){		
		subscriberList.remove(subscriber);
		System.out.println(subscriber+" has been removed from your contacts.");
	}	

	/**
	 * The user has accepted to add the contact to his contact list
	 * So we send an ok
	 * @param subscriber - the subscriber for who we are going to send an ok
	 */
	public void acceptSubscribe(String subscriberAddress){
		Subscriber subscriber=(Subscriber)subscriberList.get(subscriberAddress);
		Dialog dialog=subscriber.getDialog();
		if (!dialog.isServer()) {
			System.out.println("Problem : it's a client transaction");            
		}
		ServerTransaction serverTransaction=
			(ServerTransaction) dialog.getFirstTransaction();
		Request request=serverTransaction.getRequest();
		Response response=null;
		try{
			response=MessageListener.messageFactory.createResponse(Response.OK,request);
			ToHeader toHeader=(ToHeader)response.getHeader(ToHeader.NAME);
			if (toHeader.getTag()==null)
				toHeader.setTag(new Integer((int)(Math.random() * 10000)).toString());
		}
		catch(ParseException pe){
			pe.printStackTrace();
		}
		try{
			serverTransaction.sendResponse(response);
		}
		catch(SipException se){
			se.printStackTrace();
		}
		messageListener.sipMeetingManager.getPresentityManager().
			sendNotifyToSubscriber(
									subscriber,
									"open",
									"online");
	}

	/**
	 * The user has declined to add the contact to his contact list
	 * So we send a DECLINE
	 * @param subscriber - the subscriber for who we are going to send an DECLINE
	 */    
	public void declineSubscribe(String subscriberAddress){
		Subscriber subscriber=(Subscriber)subscriberList.get(subscriberAddress);
		Dialog dialog=subscriber.getDialog();
		if (!dialog.isServer()) {
			System.out.println("Problem : it's a client transaction");            
		}
		ServerTransaction serverTransaction=
			(ServerTransaction) dialog.getFirstTransaction();
		Request request=serverTransaction.getRequest();
		Response response=null;
		try{
			response=MessageListener.messageFactory.createResponse(Response.DECLINE,request);
		}
		catch(ParseException pe){
			pe.printStackTrace();
		}
		try{
			serverTransaction.sendResponse(response);
		}
		catch(SipException se){
			se.printStackTrace();
		}
		subscriberList.remove(subscriber.getAddress());
	}

	/**
	 * Send a notification of the presence status of the application to all 
	 * subscribers
	 * @param status - status of the presence
	 * @param subStatus - subStatus of the presence
	 */
	public void sendNotifyToAllSubscribers(String status,String subStatus) {
		try{
			 // We have to get all our subscribers and send them a NOTIFY!
			 System.out.println("DEBUG, IMNotifyProcessing, sendNotifyToAllSuscribers(),"+
			 " we have to notify our SUBSCRIBERS: let's send a NOTIFY for each one "+
			 "of them (subscribersList: "+subscriberList.size()+")!!!");
			Enumeration e=subscriberList.elements();
			while(e.hasMoreElements()) {
				Subscriber subscriber=(Subscriber)e.nextElement();                
				String subscriberName=subscriber.getAddress();
            
				String contactAddress= configuration.contactIPAddress+":"+
					configuration.listeningPort;
				String xmlBody=null;
				if (!status.equals("closed") )                
					xmlBody=XMLpidfParser.createXMLBody(status,subStatus,subscriberName,
					contactAddress);
            
				Dialog dialog=subscriber.getDialog();
				if (dialog==null) {
					System.out.println("ERROR, sendNotifyToAllSubscribers(), Pb to "+
					"retrieve the dialog, NOTIFY not sent!");
				}
				else
					sendNotify(xmlBody,dialog);
			 }
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} 
	}

	/**
	 * Send a notification of the presence status of the application to a 
	 * subscriber
	 * @param subscriber - the subscriber to notify
	 * @param status - the new presence status of the application
	 * @param subStatus - the new presence substatus of the application
	 */
	public void sendNotifyToSubscriber(Subscriber subscriber,String status,String subStatus){
		String subscriberName=subscriber.getAddress();
            
		String contactAddress= configuration.contactIPAddress+":"+
			configuration.listeningPort;
		String xmlBody=null;
		if (!status.equals("closed") )                
			xmlBody=XMLpidfParser.createXMLBody(status,subStatus,subscriberName,
			contactAddress);

		Dialog dialog=subscriber.getDialog();
		if (dialog==null) {
			System.out.println("ERROR, sendNotifyToAllSubscribers(), PB to "+
			"retrieve the dialog, NOTIFY not sent!");
		}
		else
			sendNotify(xmlBody,dialog);
	}

	/**
	 * Send a notify from a dialog
	 * @param body -  the body of the notify
	 * @param dialog - the dialog form which the notify will be created
	 */
	public void sendNotify(String body,Dialog dialog) {
		try{
			// We send the NOTIFY!!!
        
			// we create the Request-URI: the one of the proxy
			HeaderFactory headerFactory=MessageListener.headerFactory;
			AddressFactory addressFactory=MessageListener.addressFactory;
			MessageFactory messageFactory=MessageListener.messageFactory;
			//SipProvider sipProvider=imUA.getSipProvider();
        
			String transport=configuration.signalingTransport;
			String proxyAddress=configuration.outboundProxy;
			int proxyPort=configuration.proxyPort;
        
			SipURI requestURI=null;
			if (proxyAddress!=null) {
				 requestURI=addressFactory.createSipURI(null,proxyAddress);
				 requestURI.setPort(proxyPort);
				 requestURI.setTransportParam(transport);
			}
			else {
				System.out.println("DEBUG, IMNotifyProcessing, sendNotify(), request-uri is null");
				return;
			}
        
       
			Address localAddress=dialog.getLocalParty();
			Address remoteAddress=dialog.getRemoteParty();  
                  
			FromHeader fromHeader=headerFactory.createFromHeader(localAddress,dialog.getLocalTag());
			ToHeader toHeader=headerFactory.createToHeader(remoteAddress,dialog.getRemoteTag());
        
			int cseq=dialog.getLocalSequenceNumber();
			CSeqHeader cseqHeader=headerFactory.createCSeqHeader(cseq,"NOTIFY");
            
			CallIdHeader callIdHeader=dialog.getCallId();
			//Listening Point
			ListeningPoint listeningPoint = messageListener.sipProvider.getListeningPoint();    
			  
			//ViaHeader
			ArrayList viaHeaders = null;        
			viaHeaders = new ArrayList();
			try {
				ViaHeader viaHeader = MessageListener.headerFactory.createViaHeader(
					configuration.contactIPAddress,
					listeningPoint.getPort(),
					listeningPoint.getTransport(),
					null
					);
				viaHeaders.add(viaHeader);
			}
			catch (ParseException ex) {
				//Shouldn't happen
				ex.printStackTrace();
			}
			catch (InvalidArgumentException ex) {
				//Should never happen
				System.out.println("The application is corrupt");                
			}
        
			// MaxForwards header:
			MaxForwardsHeader maxForwardsHeader=headerFactory.createMaxForwardsHeader(70);
        
			Request request=null;
			ClientTransaction clientTransaction=null;
			if (body==null) {
            
				request=messageFactory.createRequest(requestURI,"NOTIFY",
				callIdHeader,cseqHeader,fromHeader,toHeader,viaHeaders,maxForwardsHeader);
           
				// We have to add an Expires-Header of 0!!!
				ExpiresHeader expiresHeader=headerFactory.createExpiresHeader(0);
				request.setHeader(expiresHeader);
            
			}
			else {
				body=body+"\r\n";
				// Content-Type:
				ContentTypeHeader contentTypeHeader=headerFactory.createContentTypeHeader(
				"application","xpidf+xml");
            
				request=messageFactory.createRequest(requestURI,"NOTIFY",
				callIdHeader,cseqHeader,fromHeader,toHeader,viaHeaders,maxForwardsHeader
				,contentTypeHeader,body);
               
			}         
			//Route Header
			SipURI routeURI=null;
			try{
				routeURI=MessageListener.addressFactory.createSipURI(
					  null,
					  configuration.outboundProxy);
			}
			catch(ParseException pe){
				pe.printStackTrace();
			}
			routeURI.setPort(configuration.proxyPort);
			try {
				routeURI.setTransportParam(configuration.signalingTransport);
			}
			catch(ParseException pe){
				pe.printStackTrace();
			}
			RouteHeader routeHeader = MessageListener.headerFactory.createRouteHeader(
				MessageListener.addressFactory.createAddress(routeURI));			           
			request.addHeader(routeHeader);
		
			// WE have to add a new Header: "Subscription-State"
			System.out.println("DEBUG, sendNotify(), We add the Subscription-State"+
			" header to the request");
			Header header=headerFactory.createHeader("Subscription-State","active");
			request.setHeader(header);
        
			// WE have to add a new Header: "Event"
			header=headerFactory.createHeader("Event","presence");
			request.setHeader(header);
        
			// ProxyAuthorization header if not null:
			//ProxyAuthorizationHeader proxyAuthHeader=imUA.getProxyAuthorizationHeader();
			//if (proxyAuthHeader!=null) 
				//request.setHeader(proxyAuthHeader);
        
			clientTransaction=messageListener.sipProvider.getNewClientTransaction(request);
			dialog.sendRequest(clientTransaction);
        
			System.out.println("DEBUG, sendNotify(),"+
			" NOTIFY sent:\n" );
			System.out.println(request.toString());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}                	    
}
