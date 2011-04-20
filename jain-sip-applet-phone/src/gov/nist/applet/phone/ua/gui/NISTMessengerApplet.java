/*
 * NISTMessengerApplet.java
 *
 * Created on December 11, 2003, 3:51 PM
 */

package gov.nist.applet.phone.ua.gui;

import gov.nist.applet.phone.media.messaging.VoiceRecorder;
import gov.nist.applet.phone.ua.ChatSessionManager;
import gov.nist.applet.phone.ua.Configuration;
import gov.nist.applet.phone.ua.RegisterStatus;
import gov.nist.applet.phone.ua.MessengerController;
import gov.nist.applet.phone.ua.MessengerManager;
import gov.nist.applet.phone.ua.StopMessenger;
import gov.nist.applet.phone.ua.presence.Subscriber;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;

//import netscape.javascript.JSObject;
/**
 *
 * @author  DERUELLE Jean
 */
public class NISTMessengerApplet
	extends javax.swing.JApplet
	implements NISTMessengerGUI {
	Configuration configuration;
	/*MVC attributes*/
	private MessengerManager sipMeetingManager;
	private MessengerController controllerMeeting;
	private ChatSessionManager chatSessionManager;
	/**
	 * GUI variables.
	 */
	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton addContactButton;
	private javax.swing.JMenuBar fileMenuBar1;
	private javax.swing.JLabel imageLabel;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JMenu jMenu5;
	private javax.swing.JMenuItem jMenuItemConfiguration;
	private javax.swing.JMenuItem jMenuItemRegister;
	private javax.swing.JMenuItem jMenuItemUnregister;
	private javax.swing.JMenu jMenuStatus;
	private javax.swing.JPanel mainPanel;
	private javax.swing.JButton removeContactButton;
	// End of variables declaration//GEN-END:variables
	private javax.swing.JList jList1;
	private DefaultListModel listModel;
	private JRadioButtonMenuItem onlineJRadioButtonMenuItem;
	private JRadioButtonMenuItem awayJRadioButtonMenuItem;
	private JRadioButtonMenuItem offlineJRadioButtonMenuItem;
	private JRadioButtonMenuItem busyJRadioButtonMenuItem;
	private ButtonGroup statusGroup;
	private boolean useResponder = false;
	//private JSObject document=null;
	/**
	 * 
	 */
	public void init() {
		if (!checkForJMF()) {
			JEditorPane jEditorPane = new JEditorPane();
			JOptionPane.showMessageDialog(
				this,
				"Please install the latest version of JMF from "
					+ "the link provided on the webpage\n",
				"Java Media Framework not installed on your computer",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		System.out.println("initializing Applet");
		configuration = new Configuration();
		configuration.outboundProxy = getParameter("PROXYADDRESS");
		if (configuration.outboundProxy == null) {
			JOptionPane.showMessageDialog(
				this,
				"JSP configuration Missing PROXYADDRESS");
			return;
		}
		if (getParameter("RESPONDER") != null)
			useResponder = true;
		System.out.println(
			"outbound proxy address " + configuration.outboundProxy);
		try {
			configuration.proxyPort =
				Integer.parseInt(getParameter("PROXYPORT"));
		} catch (Exception nfe) {
			nfe.printStackTrace();
			JOptionPane.showMessageDialog(
				this,
				"JSP configuration error bad PROXYPORT "
					+ configuration.proxyPort);
		}
		System.out.println("outbound proxy port " + configuration.proxyPort);

		configuration.signalingTransport = getParameter("SIGNALINGTRANSPORT");
		if (configuration.signalingTransport == null) {
			JOptionPane.showMessageDialog(
				this,
				"JSP Configuration ERROR SINGALINGTRANSPORT param missing");
			return;
		}
		configuration.mediaTransport = getParameter("MEDIATRANSPORT");
		if (configuration.mediaTransport == null) {
			JOptionPane.showMessageDialog(
				this,
				"JSP Configuration ERROR MEDIATRANSPORT param missing");
			return;
		}
		configuration.userURI = getParameter("USERURI");
		if (configuration.userURI == null) {
			JOptionPane.showMessageDialog(
				this,
				"JSP Configuration ERROR USERURI param missing");
			return;
		}
		System.out.println(configuration.userURI);

		try {
			//open socket to web server to ensure you to get correct local IP
			String serverAddr = getParameter("SERVERADDR");
			int serverPort = Integer.parseInt(getParameter("SERVERPORT"));

			Socket socket = new Socket(serverAddr, serverPort);
			configuration.stackIPAddress =
				socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "JSP Configuration ERROR ");
			return;
		}

		configuration.contactIPAddress = getParameter("MYADDRESS");
		System.out.println(configuration.stackIPAddress);
		try {
			UIManager.setLookAndFeel(
				UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} catch (InstantiationException ie) {
			ie.printStackTrace();
		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
		} catch (UnsupportedLookAndFeelException ulafe) {
			ulafe.printStackTrace();
		}

		/*JSObject jsobject = JSObject.getWindow(this);
		document = (JSObject) jsobject.getMember("document");*/
	}
	/**
	 * 
	 */
	public void start() {
		final JApplet object = this;
		chatSessionManager = new ChatSessionManager();
		sipMeetingManager = new MessengerManager(configuration, this);
		/*String cookieContacts = getCookieContacts();
		System.out.println(cookieContacts);
		if (cookieContacts != null) {
			Vector contacts = parseCookieContacts(cookieContacts);
			sipMeetingManager.setContactList(contacts);
		}*/
		//sipMeetingManager.addObserver(this);
		controllerMeeting =
			new MessengerController(
				sipMeetingManager,
				chatSessionManager,
				this);
		initComponents();
		listModel = new DefaultListModel();

		//Create the list and put it in a scroll pane.
		jList1 = new JList(listModel);
		jList1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jList1.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String contactAddress =
						(String) listModel.elementAt(jList1.getSelectedIndex());
					if (contactAddress.trim().indexOf('(') != -1)
						contactAddress =
							contactAddress.substring(
								0,
								contactAddress.trim().indexOf("("));
					//check if a chat frame has already been opened
					ChatFrame chatFrame =
						(ChatFrame) chatSessionManager.getChatFrame(
							contactAddress);
					if (chatFrame == null) {
						//emulate button click
						chatFrame =
							new ChatFrame(
								object,
								contactAddress,
								sipMeetingManager,
								chatSessionManager);
						chatSessionManager.addChatSession(
							contactAddress,
							chatFrame);
						chatFrame.show();
					} else {
						chatFrame.show();
					}
				}
			}
		});

		JScrollPane listScrollPane = new JScrollPane(jList1);
		listScrollPane.setBounds(10, 100, 200, 250);
		mainPanel.add(listScrollPane);
		getContentPane().add(mainPanel);
		Image image = getImage(getCodeBase(), "short_nisthome_banner.jpg");
		if (image != null)
			imageLabel.setIcon(new ImageIcon(image));
		this.resize(320, 520);
		sipMeetingManager.unRegisterAndReRegister();

		if (useResponder) {
			addContactButton.setEnabled(false);
			removeContactButton.setEnabled(false);
			controllerMeeting.addContact("responder@nist.gov");
		}
		//this.show();	
	}
	/**
	 * Call when the applet is stopped and cleaned 
	 */
	public void destroy() {
		chatSessionManager.closeAllSessions();
		if (!VoiceRecorder.isClosed())
			VoiceRecorder.getInstance().close();
		if (sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.REGISTERED))
			sipMeetingManager.unRegister();
		//write a one more new cookie
		//storeContactsInCookie();
		new StopMessenger(sipMeetingManager.getMessageListener());
	}

	/**
	 * Call when the applet is stopped and cleaned 
	 */
	public void stop() {
		chatSessionManager.closeAllSessions();
		if (!VoiceRecorder.isClosed())
			VoiceRecorder.getInstance().close();
		if (sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.REGISTERED))
			sipMeetingManager.unRegister();
		new StopMessenger(sipMeetingManager.getMessageListener());
	}
	/**
	 * 
	 *
	 */
	protected void unRegister() {
		if (sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.REGISTERED)) {
			if (chatSessionManager.hasActiveSessions()) {
				int response =
					javax.swing.JOptionPane.showConfirmDialog(
						null,
						" All current sessions will be closed,\n"
							+ " do you still want to close the application ?",
						"Close the Application",
						javax.swing.JOptionPane.YES_NO_OPTION,
						javax.swing.JOptionPane.QUESTION_MESSAGE);
				if (response == javax.swing.JOptionPane.NO_OPTION)
					return;
				else if (response == javax.swing.JOptionPane.YES_OPTION) {
					chatSessionManager.closeAllSessions();
				}
			}
			sipMeetingManager.unRegister();
		}
	}
	/**
	 *      
	 * @param serverPort
	 * @return
	public void queryServerForIpAddress(int serverPort){    	        
	    String host=getCodeBase().getHost();
		System.out.println("Query Server "+host+":"+serverPort+" to get my own ip address");
	    Socket socket=null;
	    try{
	        socket=new Socket(host,serverPort);
	    }
	    catch(java.net.UnknownHostException uhe){
	        uhe.printStackTrace();
	    }
	    catch(java.io.IOException ioe){
	        ioe.printStackTrace();
	    }   
		String ipAddress=null;
		String localAddress=socket.getLocalAddress().getHostAddress();		     	
	    try{
	        java.io.InputStream in=socket.getInputStream();
	        java.io.BufferedReader bin=new java.io.BufferedReader(
	        	new java.io.InputStreamReader(in));
	        ipAddress=bin.readLine();
	        in.close();
	        bin.close();
	        socket.close();            
	    }
	    catch(java.io.IOException ioe){
	        ioe.printStackTrace();
	    }                
		System.out.println("localAddress : "+localAddress);
		System.out.println("NATAddress : "+ipAddress);
		configuration.stackIPAddress=localAddress;
		configuration.contactIPAddress=ipAddress;
	}
	 */

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents() { //GEN-BEGIN:initComponents
		mainPanel = new javax.swing.JPanel();
		imageLabel = new javax.swing.JLabel();
		addContactButton = new javax.swing.JButton();
		removeContactButton = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();
		fileMenuBar1 = new javax.swing.JMenuBar();
		jMenu5 = new javax.swing.JMenu();
		jMenuItemConfiguration = new javax.swing.JMenuItem();
		jMenuItemRegister = new javax.swing.JMenuItem();
		jMenuItemUnregister = new javax.swing.JMenuItem();
		jMenuStatus = new javax.swing.JMenu();

		onlineJRadioButtonMenuItem = new JRadioButtonMenuItem("Online");
		awayJRadioButtonMenuItem = new JRadioButtonMenuItem("Away");
		offlineJRadioButtonMenuItem = new JRadioButtonMenuItem("Be Right Back");
		busyJRadioButtonMenuItem = new JRadioButtonMenuItem("Busy");

		getContentPane().setLayout(null);

		mainPanel.setLayout(null);

		mainPanel.setMinimumSize(new java.awt.Dimension(310, 500));
		mainPanel.setPreferredSize(new java.awt.Dimension(310, 500));
		imageLabel.setMaximumSize(new java.awt.Dimension(400, 50));
		imageLabel.setMinimumSize(new java.awt.Dimension(300, 50));
		imageLabel.setPreferredSize(new java.awt.Dimension(400, 50));
		mainPanel.add(imageLabel);
		imageLabel.setBounds(11, 6, 290, 50);

		addContactButton.setText("Add Contact");
		addContactButton.setOpaque(false);
		addContactButton
			.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addContactButtonActionPerformed(evt);
			}
		});

		mainPanel.add(addContactButton);
		addContactButton.setBounds(10, 400, 120, 40);

		removeContactButton.setText("Remove Contact");
		removeContactButton.setEnabled(false);
		removeContactButton
			.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				removeContactButtonActionPerformed(evt);
			}
		});

		mainPanel.add(removeContactButton);
		removeContactButton.setBounds(160, 400, 130, 40);

		jLabel1.setText("Not Logged");
		mainPanel.add(jLabel1);
		jLabel1.setBounds(10, 70, 290, 20);

		getContentPane().add(mainPanel);
		mainPanel.setBounds(0, 0, 310, 500);

		jMenu5.setText("Menu");
		jMenuItemConfiguration.setText("Configuration");
		jMenuItemConfiguration
			.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemConfigurationActionPerformed(evt);
			}
		});

		jMenu5.add(jMenuItemConfiguration);

		jMenuItemRegister.setText("Register");
		jMenuItemRegister
			.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemRegisterActionPerformed(evt);
			}
		});

		jMenu5.add(jMenuItemRegister);

		jMenuItemUnregister.setText("Unregister");
		jMenuItemUnregister
			.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemUnregisterActionPerformed(evt);
			}
		});

		jMenu5.add(jMenuItemUnregister);

		jMenuStatus.setText("Status");
		onlineJRadioButtonMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				onlineActionPerformed(evt);
			}
		});

		awayJRadioButtonMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				awayActionPerformed(evt);
			}
		});

		offlineJRadioButtonMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				beRightBackActionPerformed(evt);
			}
		});

		busyJRadioButtonMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				busyActionPerformed(evt);
			}
		});
		statusGroup = new ButtonGroup();
		onlineJRadioButtonMenuItem.setSelected(true);
		statusGroup.add(onlineJRadioButtonMenuItem);
		statusGroup.add(offlineJRadioButtonMenuItem);
		statusGroup.add(busyJRadioButtonMenuItem);
		statusGroup.add(awayJRadioButtonMenuItem);

		jMenuStatus.add(awayJRadioButtonMenuItem);
		jMenuStatus.add(onlineJRadioButtonMenuItem);
		jMenuStatus.add(offlineJRadioButtonMenuItem);
		jMenuStatus.add(busyJRadioButtonMenuItem);

		jMenu5.add(jMenuStatus);

		fileMenuBar1.add(jMenu5);

		setJMenuBar(fileMenuBar1);

	} //GEN-END:initComponents

	private void jMenuItemUnregisterActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemUnregisterActionPerformed
		// Add your handling code here:
		if (sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.NOT_REGISTERED)) {
			JOptionPane.showMessageDialog(
				this,
				"You are currently not registered, please register to un-register",
				"Already un-registered",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		unRegister();
		controllerMeeting.undisplayAllContact();
		removeContactButton.setEnabled(false);
	} //GEN-LAST:event_jMenuItemUnregisterActionPerformed

	private void jMenuItemRegisterActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemRegisterActionPerformed
		// Add your handling code here:
		if (sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.REGISTERED)) {
			JOptionPane.showMessageDialog(
				this,
				"You are already registered, please un-register before",
				"Already registered",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		sipMeetingManager.unRegisterAndReRegister();
		controllerMeeting.displayAllContact();
		removeContactButton.setEnabled(true);
	} //GEN-LAST:event_jMenuItemRegisterActionPerformed

	private void jMenuItemConfigurationActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemConfigurationActionPerformed
		// Add your handling code here:
		new ConfigurationFrame(sipMeetingManager).show();
	} //GEN-LAST:event_jMenuItemConfigurationActionPerformed

	private void onlineActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemRegisterActionPerformed
		// Add your handling code here:
		jLabel1.setText("Logged as : " + configuration.userURI + " - Online");
		sipMeetingManager.getPresentityManager().sendNotifyToAllSubscribers(
			"open",
			"online");
	} //GEN-LAST:event_jMenuItemRegisterActionPerformed

	private void busyActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemRegisterActionPerformed
		// Add your handling code here:
		jLabel1.setText("Logged as : " + configuration.userURI + " - Busy");
		sipMeetingManager.getPresentityManager().sendNotifyToAllSubscribers(
			"inuse",
			"busy");
	} //GEN-LAST:event_jMenuItemRegisterActionPerformed

	private void awayActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemRegisterActionPerformed
		// Add your handling code here:
		jLabel1.setText("Logged as : " + configuration.userURI + " - Away");
		sipMeetingManager.getPresentityManager().sendNotifyToAllSubscribers(
			"inactive",
			"away");
	} //GEN-LAST:event_jMenuItemRegisterActionPerformed

	private void beRightBackActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_jMenuItemRegisterActionPerformed
		// Add your handling code here:
		jLabel1.setText(
			"Logged as : " + configuration.userURI + " - Be Right Back");
		sipMeetingManager.getPresentityManager().sendNotifyToAllSubscribers(
			"inactive",
			"berightback");
	} //GEN-LAST:event_jMenuItemRegisterActionPerformed

	private void removeContactButtonActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_removeContactButtonActionPerformed
		controllerMeeting.removeContact();
	} //GEN-LAST:event_removeContactButtonActionPerformed

	private void addContactButtonActionPerformed(
		java.awt.event.ActionEvent evt) {
		//GEN-FIRST:event_addContactButtonActionPerformed
		// Add your handling code here:
		if (!sipMeetingManager
			.getRegisterStatus()
			.equalsIgnoreCase(RegisterStatus.REGISTERED)) {
			JOptionPane.showMessageDialog(
				this,
				"You must be registered to add a new contact",
				"Contact Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		String contactAddress =
			(String) JOptionPane.showInputDialog(
				this,
				"Enter the contact address to add:\n",
				"Add Contact",
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				null);
		if (contactAddress != null) {
			if (contactAddress.indexOf("@") != -1) {
				Subscriber subscriber = new Subscriber(contactAddress);
				sipMeetingManager.getPresentityManager().addSubscriber(
					subscriber);
				sipMeetingManager.sendSubscribe(contactAddress);
				controllerMeeting.addContact(contactAddress);
			} else {
				JOptionPane.showMessageDialog(
					this,
					"The contact must be of the form user@domain"
						+ ", the contact has not been added",
					"Contact Error",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	} //GEN-LAST:event_addContactButtonActionPerformed

	/**
	 * Get the contact list from this frame
	 * @return the contact list from this frame
	 */
	public JList getContactList() {
		return jList1;
	}

	/**
	 * Get the view component representing the logged status label
	 * @return the logged status label
	 */
	public JLabel getLoggedStatusLabel() {
		return jLabel1;
	}

	/**
	 * Get the contact list from this frame
	 * @return the contact list from this frame
	 */
	public JButton getRemoveContactButton() {
		return removeContactButton;
	}

	public boolean useResponder() {
		return useResponder;
	}

	/**     
	 *
	 */
	public boolean checkForJMF() {
		Class clz;

		//Check for basic JMF class.
		try {
			Class.forName("javax.media.Player");
		} catch (Throwable throwable2) {
			return false;
		}
		return true;
	}

	/**
	 * Return the cookie with the contacts
	 * @return the string representing the contacts
	 */
	/*public String getCookieContacts(){
		if(document!=null){
			return (String)document.getMember("cookie");
		}
		else
			return null;
	}*/

	/**
	 * Store the contacts into a cookie
	 */
	/*public void storeContactsInCookie(){
		Vector contacts=sipMeetingManager.getContactList();
		String cookieContacts="";
		for(int i=0;i<contacts.size();i++){
			cookieContacts+="contact"+i+"="+(String)contacts.get(i)+";";			
		}
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 1);	 // Setting the calendar to one year ahead			
		String cookie=cookieContacts+" expires="+calendar.getTime().toString();			
		System.out.println("Setting the cookie for the contacts: "+cookie);
		//document.removeMember("cookie");						
		document.setMember( "cookie" ,  cookie);
	} */

	/**
	 * Parse the cookie String into a List of contacts 
	 * @param cookieContact -  the cookie string containing the contacts
	 * @return list of contacts
	 */
	public Vector parseCookieContacts(String cookieContact) {
		if (cookieContact == null || cookieContact.length() < 1) {
			return null;
		} else {
			Vector contacts = new Vector();
			StringTokenizer st = new StringTokenizer(cookieContact, ";");
			while (st.hasMoreTokens()) {
				String contact = st.nextToken();
				if (contact.indexOf("contact") != -1) {
					if (contact.indexOf("=") != -1) {
						contact = contact.substring(contact.indexOf("=") + 1);
						System.out.println(
							"Contact found in the cookie= " + contact);
						contacts.addElement(contact);
					}
				}
			}
			return contacts;
		}
	}

	/** Show a message in a dialog box and die.
	 */
	public void fatalError(String errorText) {
		JOptionPane.showMessageDialog(this, errorText);
		this.destroy();
	}
}
