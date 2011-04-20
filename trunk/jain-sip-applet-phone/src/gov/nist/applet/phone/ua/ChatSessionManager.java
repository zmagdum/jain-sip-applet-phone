/*
 * Created on Feb 1, 2004
 */
package gov.nist.applet.phone.ua;

import gov.nist.applet.phone.ua.gui.ChatFrame;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Class managing the chat sessions
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class ChatSessionManager {
	private Hashtable chatSessions=null;
	/**
	 * Constructor
	 */
	public ChatSessionManager() {
		chatSessions=new Hashtable();
	}

	/**
	 * Add a mapping between a contact and a chat Frame
	 * @param chatContact - the contact
	 * @param frame - the chat frame
	 */
	public void addChatSession(String chatContact,Object frame){
		chatSessions.put(chatContact.trim().toLowerCase(),frame);
		System.out.println("Chat Session added: "+chatContact+":"+frame);
	}

	/**
	 * remove a mapping between a contact and a chat Frame
	 * @param chatContact - the contact
	 */
	public void removeChatSession(String chatContact){
		Object frame=chatSessions.remove(chatContact.trim().toLowerCase());
		System.out.println("Chat Session removed: "+chatContact+":"+frame);
	}
	
	/**
	 * get the chat frame corresponding to a chat contact
	 * @param chatContact - the contact
	 * @return the chat frame
	 */
	public Object getChatFrame(String chatContact){
		System.out.println("Get chat session: "+chatContact);
		return chatSessions.get(chatContact.trim().toLowerCase());
	}
	
	/**
	 * Check if we still have some active sessions
	 * @return false if there is no im session active
	 */
	public boolean hasActiveSessions(){
		if(chatSessions.isEmpty())
			return false;
		return true; 	
	}
	/**
	 * Close all the active Sessions	 
	 */
	public void closeAllSessions(){
		Collection c=chatSessions.values();
		Iterator it=c.iterator();
		while(it.hasNext()){
			ChatFrame chatFrame=(ChatFrame)it.next();
			chatFrame.exitIMSession();
			chatFrame.dispose();
			c=chatSessions.values();
			it=c.iterator();
		}
	}
}
