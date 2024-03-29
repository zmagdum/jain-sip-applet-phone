/*
 * ChatFrame.java
 *
 * Created on January 27, 2004, 10:13 PM
 */

package gov.nist.applet.phone.ua.gui;

import java.net.URL;

import javax.swing.*;

import gov.nist.applet.phone.media.messaging.VoiceRecorder;
import gov.nist.applet.phone.ua.ChatSessionManager;
import gov.nist.applet.phone.ua.MessengerManager;
import gov.nist.applet.phone.ua.call.AudioCall;
import gov.nist.applet.phone.ua.call.Call;

/**
 *
 * @author  DERUELLE Jean
 */
public class ChatFrame extends javax.swing.JFrame {
    String contactAddress=null;
    MessengerManager sipMeetingManager=null;
	ChatSessionManager chatSessionManager=null;	
	IncomingMessageFrame incomingMessageFrame=null;
    /** Creates new form ChatFrame */
    public ChatFrame(
    				Object parent,
    				String contactAddress,
    				MessengerManager sipMeetingManager,
    				ChatSessionManager chatSessionManager) {
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
            } catch(Exception exe){
            exe.printStackTrace();
            }
    	this.chatSessionManager=chatSessionManager;
        this.contactAddress=contactAddress.trim().toLowerCase();
        this.sipMeetingManager=sipMeetingManager;
        initComponents();        
        if(parent instanceof JApplet){
        	JApplet applet=(JApplet)parent;
        	jLabel1.setIcon(new ImageIcon(
    			applet.getImage(
					applet.getCodeBase(),
					"short_nisthome_banner.jpg")));
        	jLabel2.setIcon(new ImageIcon(
    			applet.getImage(
					applet.getCodeBase(),
					"short_logo.jpg")));
        }
        else{
			//Get current classloader
			ClassLoader cl = this.getClass().getClassLoader(); 
			URL url=cl.getResource("images/short_nisthome_banner.jpg");
			if(url!=null)
				jLabel1.setIcon(new ImageIcon(url));		
			URL url2=cl.getResource("images/short_logo.jpg");
			if(url2!=null)
				jLabel2.setIcon(new ImageIcon(url2));
        }
        this.setSize(750, 430);
        jTextArea1.requestFocus();
        //this.setResizable(false);
        this.setTitle("In conversation with "+contactAddress);
        //sipMeetingManager.createInstantMessagingSession("sip:"+contactAddress);		
    }
    
    /****************************** GUI METHODS *******************************/
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
		
        getContentPane().setLayout(null);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setAlignmentX(10.0F);
        jSplitPane1.setMaximumSize(new java.awt.Dimension(100, 100));
        jSplitPane1.setOneTouchExpandable(false);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(100, 100));
        jPanel1.setLayout(null);
//
//		jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
//			public void keyPressed(java.awt.event.KeyEvent evt) {
//				jTextArea1KeyPressed(evt);
//			}
//		});
//		jTextArea2.setEditable(false);
//		jTextArea2.setLineWrap(true);
//		jTextArea1.setLineWrap(true);
//        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        jScrollPane1.setViewportView(jTextArea2);

        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(0, 0, 460, 200);

        jSplitPane1.setTopComponent(jPanel1);

        jPanel2.setLayout(null);

//        jButton1.setText("Send");
//		jButton1.addActionListener(new java.awt.event.ActionListener() {
//			public void actionPerformed(java.awt.event.ActionEvent evt) {
//				jButton1ActionPerformed(evt);
//			}
//		});
//        jPanel2.add(jButton1);
//        jButton1.setBounds(390, 10, 63, 50);

//        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        jScrollPane2.setViewportView(jTextArea1);

        jPanel2.add(jScrollPane2);
        jScrollPane2.setBounds(0, 0, 380, 70);
		
        jSplitPane1.setBottomComponent(jPanel2);

        getContentPane().add(jSplitPane1);
        jSplitPane1.setBounds(10, 70, 462, 280);

        getContentPane().add(jLabel1);
        jLabel1.setBounds(10, 10, 460, 50);

        getContentPane().add(jLabel2);
        jLabel2.setBounds(500, 70, 191, 310);

        jPanel3.setLayout(null);

        /*jLabel3.setText("Voice Messaging :");
        jLabel3.setEnabled(false);
        jPanel3.add(jLabel3);
        jLabel3.setBounds(0, 0, 130, 20);

        jButton4.setText("Right click & hold to record msg.");
        jButton4.setEnabled(false);*/
		/*jButton4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton4ActionPerformed(evt);
			}
		});*/
		/*jButton4.addMouseListener(new MouseListener(){				
			public void mousePressed(MouseEvent m){
				VoiceRecorder voiceRecorder=VoiceRecorder.getInstance();
				//record voice in a buffer						
				if(voiceRecorder.start())									  			      
					jButton4.setText("Release to send voice msg."); 
			}
			public void mouseReleased(MouseEvent m){
				VoiceRecorder voiceRecorder=VoiceRecorder.getInstance();
				jButton4.setText("Right click & hold to record msg.");
				//stop recording voice 			
				if(voiceRecorder.stop()){					
					//send the voice message            
					sipMeetingManager.sendVoiceMessage(
						"sip:"+contactAddress,
						voiceRecorder.getRecord());
				}			   				
			}
			public void mouseEntered(MouseEvent m){
				
			}
			public void mouseExited(MouseEvent m){
				
			}
			public void mouseClicked(MouseEvent m){
				
			}
		});
        jPanel3.add(jButton4);
        jButton4.setBounds(1, 40, 230, 50);*/

        getContentPane().add(jPanel3);
        jPanel3.setBounds(500, 240, 300, 110);

        jButton2.setText("Audio");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAudioActionPerformed(evt);
            }
        });

        getContentPane().add(jButton2);
        jButton2.setBounds(496, 10, 80, 50);

        jButton3.setText("Video");
		jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonVideoActionPerformed(evt);
            }
        });
        getContentPane().add(jButton3);
        jButton3.setBounds(610, 10, 80, 50);

        pack();
    }//GEN-END:initComponents

//	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
//		sendMessage(jTextArea1.getText());
//	}

    private void jButtonAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Add your handling code here:
        if(jButton2.getText().equalsIgnoreCase("audio")){
			dial();                                   
        }
        else if(jButton2.getText().equalsIgnoreCase("stop")){
        	disableAudioConversation();
            stopCall();            
        }
        else if(jButton2.getText().equalsIgnoreCase("cancel")){
			//jTextArea2.append("You cancelled the call\n");
        	cancelCall();
        }
    }//GEN-LAST:event_jButton2ActionPerformed       
    
	private void jButtonVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
	    JOptionPane.showMessageDialog(
			this,
			"feature not available in this release",
			"Video feature",
			JOptionPane.INFORMATION_MESSAGE);

    }//GEN-LAST:event_jButton2ActionPerformed

	private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		// Add your handling code here:
		VoiceRecorder voiceRecorder=VoiceRecorder.getInstance();		
		if(jButton4.getText().equalsIgnoreCase("talk")){			
			//record voice in a buffer					
			if(voiceRecorder.start())									  			      
				jButton4.setText("Stop");                     
		}
		else{
			//stop recording voice 			
			if(voiceRecorder.stop()){
				jButton4.setText("Talk");
				//send the voice message            
				sipMeetingManager.sendVoiceMessage(
					"sip:"+contactAddress,
					voiceRecorder.getRecord());
			}			   
		}
	}//GEN-LAST:event_jButton2ActionPerformed       
    
//	private void jTextArea1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
//		// Add your handling code here:
//		if(evt.getKeyCode()==java.awt.event.KeyEvent.VK_ENTER)
//			sendMessage(jTextArea1.getText());
//	}
	/**
	 * 
	 *
	 */
	public void cancelAudioConversation(){
		jLabel3.setEnabled(false);
		jButton4.setEnabled(false);
		jButton2.setEnabled(true);	
		jButton2.setText("Audio");
		if(incomingMessageFrame!=null){
			incomingMessageFrame.dispose();
			incomingMessageFrame=null;
		}
	//	jTextArea2.append("Your buddy has cancelled the call\n ");
	}
    /**
     * 
     *
     */
	public void disableAudioConversation(){
		jLabel3.setEnabled(false);
		jButton4.setEnabled(false);	
		jButton2.setText("Audio");
    }
	/**
	 * 
	 * @param callee
	 */
	public void enableAudioConversation(String callee){
		AudioCall audioCall=
			sipMeetingManager.getCallManager().findAudioCall(callee);
		if(audioCall.getVoiceMessaging()){
			jButton4.setEnabled(true);		
			jLabel3.setEnabled(true);
		}
		jButton2.setText("Stop");
	}
	/**
	 * 
	 * @param callStatus
	 */
	public void updateAudioStatus(String callStatus){		
		if(callStatus.equalsIgnoreCase(Call.BUSY)){
			//jTextArea2.append("Your buddy is currently busy, " +
			//			  "your call was rejected\n");
			jButton2.setText("Audio");			
		}		
		else if (callStatus.equalsIgnoreCase(Call.TEMPORARY_UNAVAILABLE)){
			//jTextArea2.append("Your buddy is currently unavailable\n");
			jButton2.setText("Audio");						
		}
		else{				
			jButton2.setText("Cancel");			
		}		
	}
	/**
	 * 
	 *
	 */
	public void disableVideoConversation(){
    
	}
        
	/**
	 * Exit the session and the chat frame 
	 */
	protected void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
		exitIMSession();
	}//GEN-LAST:event_exitForm
	
	/**	 
	 * Exit the session and the chat frame
	 */	
	public void exitIMSession(){
		if(jButton2.getText().equalsIgnoreCase("Stop"))
			stopCall();
		//sipMeetingManager.stopInstantMessagingSession("sip:"+contactAddress);
		chatSessionManager.removeChatSession(contactAddress);		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);	
	}
		
    /***************************** APPLICATION METHODS ************************/
    
    /**
     * Got a new instant message, update the gui
     * @param message - message received
     */
    public void newMessage(String message){
		if(message.indexOf("\n")==0)
			message=message.substring("\n".length());
//		jTextArea2.append(
//					contactAddress
//					+ "> "
//					+ message+"\n");
//		jTextArea2.setCaretPosition(jTextArea2.getText().length());		
    }
    
    /**
     * Action performed when pushing the Dial Button
     */
    protected void dial() {
        sipMeetingManager.call("sip:"+contactAddress);
    }
    
    /**
     * Action performed when pushing the Stop Button
     */
    protected void stopCall() {        
        sipMeetingManager.endCall("sip:"+contactAddress);
    }
    
    /**
     * Action performed when pushing the Cancel Button
     */
	protected void cancelCall() {        
        sipMeetingManager.cancelCall("sip:"+contactAddress);
    }
    
	/**
	 * Action performed when answering NO to an incoming call
	 */
	protected void answerBusy(String caller) {        
		sipMeetingManager.sendBusy(caller);
		jButton2.setEnabled(true);
	}

	/**
	 * Action performed when answering YES to an incoming call
	 */
	protected void answerOK(String caller) {        
		sipMeetingManager.answerCall(caller);
		jButton2.setEnabled(true);
	}
    
    /**
     * 
     * @param message
     */
	protected void sendMessage(String message){
		while(message.indexOf("\n")==0)
			message=message.substring("\n".length());
		if(message.length()<=0)
			return;
//    	jTextArea2.append(
//    		sipMeetingManager.getMessageListener().getConfiguration().userURI
//    		+ "> "
//    		+ message+"\n");
//		jTextArea2.setCaretPosition(jTextArea2.getText().length());
//    	jTextArea1.setText(null);		
    	sipMeetingManager.sendInstantMessage("sip:"+contactAddress,message);
    }  
         
    /**
     * 
     * @param incomingMessageFrame
     */
    public void setIncomingMessageFrame(IncomingMessageFrame incomingMessageFrame){
    	this.incomingMessageFrame=incomingMessageFrame;
    	jButton2.setEnabled(false);
    }
    
    private javax.swing.ButtonGroup buttonGroup1;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;    
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    // End of variables declaration//GEN-END:variables
    
}
