/*
 * MessengerController.java
 *
 * Created on November 25, 2003, 9:03 AM
 */

package gov.nist.applet.phone.ua;

import gov.nist.applet.phone.ua.call.AudioCall;
import gov.nist.applet.phone.ua.call.Call;
import gov.nist.applet.phone.ua.gui.AuthenticationDialog;
import gov.nist.applet.phone.ua.gui.ChatFrame;
import gov.nist.applet.phone.ua.gui.IncomingMessageFrame;
import gov.nist.applet.phone.ua.gui.NISTMessengerApplet;
import gov.nist.applet.phone.ua.gui.NISTMessengerGUI;
import gov.nist.applet.phone.ua.gui.ServerInfoXmlManager;
import gov.nist.applet.phone.ua.pidf.parser.AddressTag;
import gov.nist.applet.phone.ua.pidf.parser.AtomTag;
import gov.nist.applet.phone.ua.pidf.parser.MSNSubStatusTag;
import gov.nist.applet.phone.ua.pidf.parser.PresenceTag;
import gov.nist.applet.phone.ua.presence.PresentityManager;
import gov.nist.applet.phone.ua.presence.Subscriber;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import javax.sip.address.URI;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * This application has been designed in following the MVC design pattern
 * Thus, this class is part of the Control.
 * when there is a change in the model it updates the view
 * 
 * @author  DERUELLE Jean
 */
public class MessengerController implements java.util.Observer {
    private MessengerManager sipMeetingManager;    
    private NISTMessengerGUI nistMeetingGUI;
    private ChatSessionManager chatSessionManager;
    private ServerInfoXmlManager serverInfoManage;
    private String path="xmlSource/ServerInfo.xml";
    
    /** Creates a new instance of ControllerMeeting 
     * @param sipMeetingManager - the model of the MVC design pattern used in this application
     * @param sipMeetingGUI - the view of the MVC design pattern used in this application
     */
    public MessengerController(
    							MessengerManager sipMeetingManager,
    							ChatSessionManager chatSessionManager, 
    							NISTMessengerGUI nistMeetingGUI) {
        this.nistMeetingGUI=nistMeetingGUI;
        this.chatSessionManager=chatSessionManager;
        this.sipMeetingManager=sipMeetingManager;
        this.sipMeetingManager.addObserver(this);
        this.serverInfoManage=new ServerInfoXmlManager(path);
    }
    
    /**
     * @see java.util.Observer#update(java.util.Observable, Object)
     */
    public void update(java.util.Observable o, Object arg) {    	
		if(arg instanceof RegisterStatus){
			RegisterStatus registerStatus=(RegisterStatus)arg;
			String userURI=
				sipMeetingManager.getMessageListener().getConfiguration().userURI;			
			if(sipMeetingManager.getRegisterStatus().equalsIgnoreCase(
			   RegisterStatus.NOT_REGISTERED))
				nistMeetingGUI.getLoggedStatusLabel().setText("Not Logged");
			if(sipMeetingManager.getRegisterStatus().equalsIgnoreCase(
			   RegisterStatus.REGISTRATION_IN_PROGRESS))
				nistMeetingGUI.getLoggedStatusLabel().setText(
					"Trying to log as : "+ userURI);
			if(sipMeetingManager.getRegisterStatus().equalsIgnoreCase(
			   RegisterStatus.PROXY_AUTHENTICATION_REQUIRED)){

				if(serverInfoManage.getInfo("name").trim().equals("")||serverInfoManage.getInfo("password").trim().equals("")){
				AuthenticationDialog authenticationDialog= 
					new AuthenticationDialog(nistMeetingGUI,"localhost");
				sipMeetingManager.registerWithAuthentication(
					authenticationDialog.getUserName(),
					authenticationDialog.getPassword(),
					"localhost");					       
				}
				else{
					sipMeetingManager.registerWithAuthentication(
							serverInfoManage.getInfo("name").trim(),
							serverInfoManage.getInfo("password").trim(),
							"localhost");	
				}
			}					
			if(sipMeetingManager.getRegisterStatus().equalsIgnoreCase(
			   RegisterStatus.REGISTERED)){
					nistMeetingGUI.getLoggedStatusLabel().setText(
									"Logged as : "+userURI+" - (Online)");
					displayAllContact();
			   }
				
		}
		if(arg instanceof Call){
			Call call=(Call)arg;
			String callee=call.getCallee().trim().toLowerCase();
			if(callee.indexOf(";")!=-1)
				callee=callee.substring(0,callee.indexOf(";"));		
			if(callee.indexOf("sip:")!=-1)
				callee=callee.substring("sip:".length());					
			ChatFrame chatFrame=(ChatFrame)chatSessionManager.getChatFrame(callee);			
			String callStatus=call.getStatus();
			System.out.println("callee "+callee+":chatFrame "+chatFrame+":status "+callStatus);
			if(callStatus.equalsIgnoreCase(Call.NOT_IN_A_CALL)){
				/*System.out.println(
					"Updating chatFrame "+
					chatFrame+
					" callee: "+
					callee);*/							
				if(chatFrame!=null)
					chatFrame.disableAudioConversation();				
			}
			else if(callStatus.equalsIgnoreCase(Call.CANCEL)){
				if(chatFrame!=null)
					chatFrame.cancelAudioConversation();
			}
			else if(callStatus.equalsIgnoreCase(Call.IN_A_CALL)){
				chatFrame.enableAudioConversation("sip:"+callee);
			}
			else if(callStatus.equalsIgnoreCase(Call.RINGING)
				    || callStatus.equalsIgnoreCase(Call.TRYING)
				    || callStatus.equals(Call.BUSY)
				    || callStatus.equals(Call.TEMPORARY_UNAVAILABLE)){
				chatFrame.updateAudioStatus(callStatus);
				if(call instanceof AudioCall){
					AudioCall audioCall=(AudioCall)call;
					URI url=audioCall.getURL();
					if(url!=null){
						if(nistMeetingGUI instanceof NISTMessengerApplet){
							NISTMessengerApplet applet=(NISTMessengerApplet)nistMeetingGUI;
							try{
								applet.getAppletContext().showDocument(new URL(url.toString()),"_blank");
							}
							catch (MalformedURLException mue) {
								mue.printStackTrace();
							}
						}
					}
				}				
			}
			else if(callStatus.equalsIgnoreCase(Call.INCOMING_CALL)){																	
				if(chatFrame==null){																
					chatFrame=new ChatFrame(
						nistMeetingGUI,
						callee,
						sipMeetingManager,
						chatSessionManager);
					chatFrame.show();
					chatSessionManager.addChatSession(callee,chatFrame);						
				}						
				IncomingMessageFrame incomingMessageFrame=
					new IncomingMessageFrame(chatFrame,callee);
				chatFrame.setIncomingMessageFrame(incomingMessageFrame);
				incomingMessageFrame.show();																
			}									           
		}
		if(arg instanceof InstantMessage){
			InstantMessage im=(InstantMessage)arg;
			String sender=im.getSender();
			String message=im.getMessage();
			Object frame=chatSessionManager.getChatFrame(sender);
			ChatFrame chatFrame=null;
			if(frame==null){
				chatFrame=new ChatFrame(
						nistMeetingGUI,
						sender,
						sipMeetingManager,
						chatSessionManager);
				chatFrame.show();
				chatSessionManager.addChatSession(sender,chatFrame);
				chatFrame.newMessage(message);
			}
			else{
				chatFrame=(ChatFrame)frame;
				chatFrame.newMessage(message);
			}
		}   
		if(arg instanceof Subscriber){
			Subscriber subscriber=(Subscriber)arg;
			String subscriberAddress=subscriber.getAddress();
			if(!sipMeetingManager.isInContactList(subscriberAddress)){
				int response=javax.swing.JOptionPane.showConfirmDialog(null,
				subscriberAddress+
				" wants to be added to your contacts, do you agree ?",
				"New Contact",
				javax.swing.JOptionPane.YES_NO_OPTION,
				javax.swing.JOptionPane.QUESTION_MESSAGE);       
				if(response==javax.swing.JOptionPane.NO_OPTION)
					declineContact(subscriberAddress);
				else if(response==javax.swing.JOptionPane.YES_OPTION){
					addContact(subscriberAddress);
					acceptContact(subscriberAddress);									
					sipMeetingManager.sendSubscribe(subscriberAddress);							
				}
			}
			else
				acceptContact(subscriberAddress);
		}         
		if(arg instanceof PresenceTag){
			PresenceTag presenceTag=(PresenceTag)arg;
			Vector atomTagList=presenceTag.getAtomTagList();
			AtomTag atomTag=(AtomTag)atomTagList.firstElement();
			AddressTag addressTag=atomTag.getAddressTag();
			MSNSubStatusTag msnSubStatusTag=addressTag.getMSNSubStatusTag();
			this.updateStatusContact(
				presenceTag.getAddress(),
				msnSubStatusTag.getMSNSubStatus());
		}
		nistMeetingGUI.repaint();
    }
            
    /**
     * Accept that the contact be added to our contact list
     * @param subscriberAddress
     */
    private void acceptContact(String subscriberAddress){
    	sipMeetingManager.getPresentityManager().
    		acceptSubscribe(subscriberAddress);
    }
    
    /**
     * Decline that the contact be added to our contact list
     * @param subscriberAddress
     */
    private void declineContact(String subscriberAddress){
		sipMeetingManager.getPresentityManager().
			declineSubscribe(subscriberAddress);
		sipMeetingManager.getPresentityManager().
    		removeSubscriber(subscriberAddress);		
    }
	/**
	 * Remove a contact from the contact list
	 */
    public void removeContact(){
		JList jList1=nistMeetingGUI.getContactList();
		DefaultListModel listModel=(DefaultListModel)jList1.getModel();
		//Add your handling code here:
		int index = jList1.getSelectedIndex();
		String contactAddress=(String)listModel.get(index);
		/*listModel.remove(index);
		
		int size = listModel.getSize();
		if (size == 0) { //Nobody's left, disable firing.
			nistMeetingGUI.getRemoveContactButton().setEnabled(false);
		} 
		else { //Select an index.
			if (index == listModel.getSize()) {
				//removed item in last position
			 	index--;
		 	}
			jList1.setSelectedIndex(index);
			jList1.ensureIndexIsVisible(index);
		}*/
		sipMeetingManager.removeContact(contactAddress);
		displayAllContact();
    }
    
	/**
	 * Add a contact in the contact list
	 * @param contactAddress - the contact to add
	 */
	public void addContact(String contactAddress){
		/*Subscriber subscriber=
				sipMeetingManager.getPresentityManager().getSubscriber(contactAddress);
		JList jList1=nistMeetingGUI.getContactList();
		DefaultListModel listModel=(DefaultListModel)jList1.getModel();
		listModel.addElement(contactAddress+" ("+subscriber.getStatus()+")");
		int size= listModel.getSize(); //get selected index
		//Select the new item and make it visible.
		jList1.setSelectedIndex(size-1);
		jList1.ensureIndexIsVisible(size-1);
		if (size > 0) { //Nobody's left, disable firing.
			nistMeetingGUI.getRemoveContactButton().setEnabled(true);
		}*/ 
		sipMeetingManager.addContact(contactAddress);
		displayAllContact();
	}
	/**
	 *
	 */
	public void displayAllContact(){
		Vector contactList=sipMeetingManager.getContactList();
		PresentityManager presentityManager=
			sipMeetingManager.getPresentityManager();
		JList jList1=nistMeetingGUI.getContactList();
		DefaultListModel listModel=(DefaultListModel)jList1.getModel();
		listModel.removeAllElements();
		for(int i=0;i<contactList.size();i++){
			String contactAddress=(String)contactList.get(i);
			Subscriber subscriber=
				presentityManager.getSubscriber(contactAddress);
			if(sipMeetingManager.presenceAllowed)
				listModel.addElement(contactAddress+" ("+subscriber.getStatus()+")");
			else
				listModel.addElement(contactAddress);
		}
		if(nistMeetingGUI instanceof NISTMessengerApplet){			
			if( ((NISTMessengerApplet)nistMeetingGUI).useResponder() )
				return;
		}
	
		if(contactList.size()>0)
			nistMeetingGUI.getRemoveContactButton().setEnabled(true);
		else
			nistMeetingGUI.getRemoveContactButton().setEnabled(false);	
	}
	
	/**
	 *
	 */
	public void undisplayAllContact(){
		JList jList1=nistMeetingGUI.getContactList();
		DefaultListModel listModel=(DefaultListModel)jList1.getModel();		
		listModel.removeAllElements();
		nistMeetingGUI.getRemoveContactButton().setEnabled(false);
	}
	
	/**
	 * Add a contact in the contact list
	 * @param contactAddress - the contact to add
	 */
	public void updateStatusContact(String contactAddress,String status){
		JList jList1=nistMeetingGUI.getContactList();
		DefaultListModel listModel=(DefaultListModel)jList1.getModel();
		Enumeration e =listModel.elements();
		int i=0;
		while(e.hasMoreElements()){
			String contact=(String)e.nextElement();
			if(contact.indexOf(contactAddress)!=-1){
				listModel.removeElementAt(i);
				if(i==0){
					listModel.addElement(
						contactAddress+" ("+status+")");
					jList1.ensureIndexIsVisible(i);
				}
				else{
					listModel.add(
						i-1,contactAddress+" ("+status+")");
					jList1.ensureIndexIsVisible(i-1);
				}
				break;
			}
			i++;
		}
			}
}
