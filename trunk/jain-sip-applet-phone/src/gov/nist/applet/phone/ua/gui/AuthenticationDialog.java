/*
 * AuthenticationDialog.java
 *
 * Created on January 27, 2004, 6:08 PM
 */

package gov.nist.applet.phone.ua.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
/**
 * 
 * @author Jean Deruelle
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class AuthenticationDialog {
    
	private JLabel realmLabel;
	private JLabel realmLabelContent;
	private JLabel userNameLabel;
	private JLabel passwordLabel;
	private JButton submitButton;
	private JDialog dialog;
	private JTextField userNameTextField;
	private JPasswordField passwordTextField;
	private boolean STOP=false;
   
	/** Creates a new instance of AuthenticationDialog */
	public AuthenticationDialog(NISTMessengerGUI parent,String realm) {		
        JFrame parentFrame=null;		
		if(parent==null)
			parentFrame=new JFrame();
		else{
			if(parent instanceof JFrame)
				parentFrame=(JFrame)parent;
			else if(parent instanceof JApplet)
				parentFrame=new JFrame();
		}
		dialog= new JDialog(parentFrame,"Authentication",true);
        
		// width, height
		dialog.setSize(200,150) ;	
		//rows, columns, horizontalGap, verticalGap
		dialog.getContentPane().setLayout( new BoxLayout(dialog.getContentPane(), 1));
		//dialog.setBackground();
        
		JPanel firstPanel=new JPanel();
		firstPanel.setBorder(BorderFactory.createEmptyBorder(10,4,10,4));
		// If put to False: we see the container's background
		firstPanel.setOpaque(false);
		//rows, columns, horizontalGap, verticalGap
		firstPanel.setLayout( new GridLayout(3,2,0,2) );
		dialog.getContentPane().add(firstPanel);
        
		realmLabel = new JLabel("realm:");
		realmLabelContent = new JLabel(realm);
		firstPanel.add(realmLabel);
		firstPanel.add(realmLabelContent);
        
		userNameLabel = new JLabel("username:");
		userNameTextField = new JTextField(20);
		firstPanel.add(userNameLabel);
		firstPanel.add(userNameTextField);
        
		passwordLabel = new JLabel("password:");
		passwordTextField = new JPasswordField(20);
		passwordTextField.setEchoChar('*');
		firstPanel.add(passwordLabel);
		firstPanel.add(passwordTextField);
        
		JPanel thirdPanel = new JPanel();
		thirdPanel.setOpaque(false);
		thirdPanel.setLayout(new FlowLayout(FlowLayout.CENTER) );
       
		submitButton = new JButton(" OK ");
		submitButton.setToolTipText("Submit your changes!");
		submitButton.setFocusPainted(false);
		submitButton.setFont(new Font ("Dialog", 1, 14));
		//submitButton.setBackground();
		//submitButton.setBorder();
		submitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				 okButtonActionPerformed(evt);
			}
		}
		);
		thirdPanel.add(submitButton);
		dialog.getContentPane().add(thirdPanel);
         
		dialog.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				clean();
			}
		}
		);
         
        
		 dialog.show();
	}
    
	public void  clean() {
		 dialog.setVisible(false) ;	
		 STOP=true;
		 dialog.dispose();
	}
    
	public boolean isStop() {
		return STOP;
	}   
    
	public void okButtonActionPerformed(ActionEvent evt) {
		 if (userNameTextField.getText() ==null || userNameTextField.getText().trim().equals("") )
			 JOptionPane.showMessageDialog(null,
										   "You must enter an user name!!!",
										   null,
			 							   JOptionPane.ERROR_MESSAGE);
		 else {
			 char[] pass= passwordTextField.getPassword();
             
			 if ( pass ==null )
				JOptionPane.showMessageDialog(null,
									  "You must enter a password!!!",
									  null,
									  JOptionPane.ERROR_MESSAGE);
			 else {
				 String s=new String(pass);
				 if ( s.trim().equals("") ) 
					JOptionPane.showMessageDialog(null,
									 "You must enter a password!!!",
									 null,
									 JOptionPane.ERROR_MESSAGE);
				 else 
					dialog.setVisible(false) ;
			 }
		 }
	}

    
	public String getUserName() {
		return userNameTextField.getText().trim();
	}
    
	public String getPassword() {
		char[] pass= passwordTextField.getPassword();
		String s=new String(pass);
		return s.trim();
	}
    
}